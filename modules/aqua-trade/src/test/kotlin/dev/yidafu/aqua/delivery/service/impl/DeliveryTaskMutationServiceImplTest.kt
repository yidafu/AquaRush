/**
 * AquaRush
 *
 * Copyright (C) 2025 AquaRush Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.yidafu.aqua.delivery.service.impl

import dev.yidafu.aqua.api.dto.UpdateOrderAddressRequest
import dev.yidafu.aqua.api.service.admin.AdminService
import dev.yidafu.aqua.api.service.admin.AddressService
import dev.yidafu.aqua.api.service.order.OrderMutationService
import dev.yidafu.aqua.api.service.order.OrderOperationService
import dev.yidafu.aqua.api.service.order.OrderQueryService
import dev.yidafu.aqua.common.domain.model.AddressModel
import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.common.domain.model.DeliverWorkerModelStatus
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.domain.model.enums.OrderOperationType
import dev.yidafu.aqua.common.domain.model.enums.PaymentType
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.messaging.service.SimplifiedEventPublishService
import dev.yidafu.aqua.common.utils.JsonHelper
import dev.yidafu.aqua.logging.util.BizLogger
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

/**
 * 配送任务变更服务单元测试:
 * - R-ORD-01/G09 状态机:派单仅接受「待派单」(待派单→配送中),二次派单/已接单订单被拦截;
 * - R-ORD-07 完成配送服务端硬校验:照片非空、收款方式必选(自提/水票核销豁免);
 * - R-ORD-08 派单约束:离线送水工不可派。
 *
 * 送水员档案并入 admins 单表后(PRD/07-user.md §F-USR-021),配送员即账号行,
 * workerId 即账号 id,校验走 AdminService。
 */
class DeliveryTaskMutationServiceImplTest :
  FunSpec({
    var adminService: AdminService = mockk()
    var orderQueryService: OrderQueryService = mockk()
    var orderMutationService: OrderMutationService = mockk()
    var orderOperationService: OrderOperationService = mockk()
    var eventPublishService: SimplifiedEventPublishService = mockk()
    var bizLogger: BizLogger = mockk()
    var addressService: AddressService = mockk()
    var service: DeliveryTaskMutationServiceImpl = mockk()
    val onlineWorker =
      AdminModel(
        id = 1L,
        username = "delivery_1",
        passwordHash = "hash",
        realName = "送水工甲",
        phone = "13800138000",
        role = AdminRoleModel.DELIVERY_WORKER,
        onlineStatus = DeliverWorkerModelStatus.ONLINE,
      )

    fun order(status: OrderModelStatus) = OrderModel(id = 1L, userId = 100L, status = status)

    beforeTest {
      adminService = mockk(relaxed = true)
      orderQueryService = mockk(relaxed = true)
      orderMutationService = mockk(relaxed = true)
      orderOperationService = mockk(relaxed = true)
      eventPublishService = mockk(relaxed = true)
      bizLogger = mockk(relaxed = true)
      addressService = mockk(relaxed = true)
      every { adminService.findById(1L) } returns onlineWorker
      every { orderMutationService.updateOrder(any(), any()) } answers { secondArg() as OrderModel }
      service =
        DeliveryTaskMutationServiceImpl(
          adminService,
          orderQueryService,
          orderMutationService,
          orderOperationService,
          eventPublishService,
          JsonHelper(),
          bizLogger,
          addressService,
        )
    }
    test("assign accepts pending dispatch only") {
      every { orderQueryService.getOrderById(1L) } returns order(OrderModelStatus.PENDING_DISPATCH)

      val assigned = service.assignDeliveryWorker(1L, 1L, 1L, false)

      assigned.status shouldBe OrderModelStatus.DELIVERING
      assigned.deliveryWorkerId shouldBe 1L
    }
    test("assign rejects non pending dispatch orders") {
      listOf(
        OrderModelStatus.PENDING_DELIVERY,
        OrderModelStatus.DELIVERING,
        OrderModelStatus.COMPLETED,
        OrderModelStatus.PENDING_PAYMENT,
      ).forEach { status ->
        every { orderQueryService.getOrderById(1L) } returns order(status)
        withClue("status=$status 应被拦截") {
          shouldThrow<BadRequestException> {
            service.assignDeliveryWorker(1L, 1L, 1L, false)
          }
        }
      }
    }
    test("assign rejects offline worker") {
      every { orderQueryService.getOrderById(1L) } returns order(OrderModelStatus.PENDING_DISPATCH)
      every { adminService.findById(1L) } returns
        AdminModel(
          id = 1L,
          username = "delivery_1",
          passwordHash = "hash",
          realName = "送水工甲",
          phone = "13800138000",
          role = AdminRoleModel.DELIVERY_WORKER,
          onlineStatus = DeliverWorkerModelStatus.OFFLINE,
        )

      shouldThrow<BadRequestException> { service.assignDeliveryWorker(1L, 1L, 1L, false) }
    }
    test("complete delivery rejects empty photos") {
      every { orderQueryService.getOrderById(1L) } returns order(OrderModelStatus.DELIVERING)

      shouldThrow<BadRequestException> { service.completeDelivery(1L, emptyList(), PaymentType.CASH, null) }
    }
    test("complete delivery rejects missing payment type") {
      every { orderQueryService.getOrderById(1L) } returns order(OrderModelStatus.DELIVERING)

      shouldThrow<BadRequestException> { service.completeDelivery(1L, listOf("https://x/1.jpg"), null, null) }
    }
    test("complete delivery defaults to water ticket for ticket orders") {
      val ticketOrder =
        order(OrderModelStatus.DELIVERING).also {
          it.ticketRedeemCount = 1
          it.deliveryWorkerId = 1L
        }
      every { orderQueryService.getOrderById(1L) } returns ticketOrder

      val completed = service.completeDelivery(1L, listOf("https://x/1.jpg"), null, null)

      completed.status shouldBe OrderModelStatus.COMPLETED
      completed.paymentType shouldBe PaymentType.WATER_TICKET
      (completed.deliveryPhotos != null) shouldBe true
      verify(exactly = 1) { eventPublishService.publishDeliveryCompleted(1L, 1L) }
    }
    test("complete delivery allows self collect without payment type") {
      val selfCollectOrder =
        order(OrderModelStatus.DELIVERING).also {
          it.isSelfCollect = true
          it.deliveryWorkerId = 1L
        }
      every { orderQueryService.getOrderById(1L) } returns selfCollectOrder

      val completed = service.completeDelivery(1L, listOf("https://x/1.jpg"), null, null)

      completed.status shouldBe OrderModelStatus.COMPLETED
    }
    test("complete delivery rejects non delivering order") {
      every { orderQueryService.getOrderById(1L) } returns order(OrderModelStatus.PENDING_DELIVERY)

      shouldThrow<BadRequestException> { service.completeDelivery(1L, listOf("https://x/1.jpg"), PaymentType.CASH, null) }
    }
    test("assign rejects disabled worker") {
      every { orderQueryService.getOrderById(1L) } returns order(OrderModelStatus.PENDING_DISPATCH)
      every { adminService.findById(1L) } returns
        AdminModel(
          id = 1L,
          username = "delivery_1",
          passwordHash = "hash",
          realName = "送水工甲",
          phone = "13800138000",
          role = AdminRoleModel.DELIVERY_WORKER,
          onlineStatus = DeliverWorkerModelStatus.ONLINE,
          accountStatus = dev.yidafu.aqua.common.domain.model.enums.AdminAccountStatus.DISABLED,
        )

      val exception = shouldThrow<BadRequestException> { service.assignDeliveryWorker(1L, 1L, 1L, false) }
      exception.message!!.contains("停用") shouldBe true
    }
    test("accept rejects disabled worker") {
      every { orderQueryService.getOrderById(1L) } returns order(OrderModelStatus.PENDING_DISPATCH)
      every { adminService.findById(1L) } returns
        AdminModel(
          id = 1L,
          username = "delivery_1",
          passwordHash = "hash",
          realName = "送水工甲",
          phone = "13800138000",
          role = AdminRoleModel.DELIVERY_WORKER,
          onlineStatus = DeliverWorkerModelStatus.ONLINE,
          accountStatus = dev.yidafu.aqua.common.domain.model.enums.AdminAccountStatus.DISABLED,
        )

      val exception = shouldThrow<BadRequestException> { service.acceptDelivery(1L, 1L) }
      exception.message!!.contains("停用") shouldBe true
    }
    // ==================== updateOrderAddress(F-ORD-024) ====================
    test("update order address applies non-null fields only") {
      val order =
        OrderModel(id = 1L, userId = 100L, status = OrderModelStatus.DELIVERING, addressId = 1L)
      val address = AddressModel(id = 1L, userId = 100L, receiverName = "旧收货人", phone = "13800138000",
        province = "广东省", city = "深圳市", district = "南山区", detailAddress = "旧详细地址1号")
      every { orderQueryService.getOrderById(1L) } returns order
      every { addressService.findById(1L) } returns address

      val result =
        service.updateOrderAddress(
          operatorId = 1L,
          orderId = 1L,
          request =
            UpdateOrderAddressRequest(
              receiverName = "新收货人",
              floor = 6,
              longitude = 113.9304,
              latitude = 22.5313,
            ),
        )

      result.id shouldBe 1L
      address.receiverName shouldBe "新收货人"
      address.floor shouldBe 6
      address.longitude shouldBe 113.9304
      address.latitude shouldBe 22.5313
      // 未传入字段不覆盖
      address.phone shouldBe "13800138000"
      address.detailAddress shouldBe "旧详细地址1号"
      verify(exactly = 1) { addressService.save(address) }
      verify(exactly = 1) {
        orderOperationService.recordOperation(1L, OrderOperationType.ADDRESS_UPDATED, any(), 1L, any(), any())
      }
    }
    test("update order address rejects terminal orders") {
      listOf(
        OrderModelStatus.COMPLETED,
        OrderModelStatus.CANCELLED,
        OrderModelStatus.REFUNDED,
      ).forEach { status ->
        every { orderQueryService.getOrderById(1L) } returns order(status)

        withClue("status=$status 应被拦截") {
          shouldThrow<BadRequestException> {
            service.updateOrderAddress(1L, 1L, UpdateOrderAddressRequest(receiverName = "新收货人"))
          }
        }
      }
      verify(exactly = 0) { addressService.save(any()) }
    }
    test("update order address rejects missing linked address") {
      every { orderQueryService.getOrderById(1L) } returns order(OrderModelStatus.PENDING_DELIVERY)
      every { addressService.findById(0L) } returns null

      val exception =
        shouldThrow<BadRequestException> {
          service.updateOrderAddress(1L, 1L, UpdateOrderAddressRequest(floor = 5))
        }
      exception.message!!.contains("地址不存在") shouldBe true
    }
  })
