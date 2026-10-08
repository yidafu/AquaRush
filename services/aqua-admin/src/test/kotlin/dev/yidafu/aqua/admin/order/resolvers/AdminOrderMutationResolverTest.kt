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

package dev.yidafu.aqua.admin.order.resolvers

import dev.yidafu.aqua.admin.order.mapper.AdminOrderMapper
import dev.yidafu.aqua.api.service.admin.AdminService
import dev.yidafu.aqua.api.service.delivery.DeliveryTaskMutationService
import dev.yidafu.aqua.api.service.order.OrderMutationService
import dev.yidafu.aqua.api.service.order.OrderQueryService
import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.domain.model.enums.PaymentType
import dev.yidafu.aqua.common.exception.UserNotFoundException
import dev.yidafu.aqua.common.graphql.generated.CreateDeliveryOrderInput
import dev.yidafu.aqua.common.graphql.generated.DeliveryPhotoReviewAction
import dev.yidafu.aqua.common.graphql.generated.Order
import dev.yidafu.aqua.common.graphql.generated.OrderItemInput
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.order.service.DeliveryPhotoReviewService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.time.LocalDateTime

class AdminOrderMutationResolverTest :
  FunSpec({
    var orderMutationService: OrderMutationService = mockk()
    var orderQueryService: OrderQueryService = mockk()
    var deliveryTaskMutationService: DeliveryTaskMutationService = mockk()
    var adminService: AdminService = mockk()
    var adminOrderMapper: AdminOrderMapper = mockk()
    var deliveryPhotoReviewService: DeliveryPhotoReviewService = mockk()
    var resolver: AdminOrderMutationResolver = mockk()
    val order: Order = mockk(relaxed = true)
    val orderModel: OrderModel =
      OrderModel(
        id = 1L,
        orderNo = "ORD-20250101-001",
        userId = 1L,
        status = OrderModelStatus.PENDING_DISPATCH,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )

    fun createSampleAdmin(): AdminModel =
      AdminModel(
        id = 1L,
        username = "admin",
        passwordHash = "hashed_password",
        realName = "管理员",
        phone = "13900139000",
        role = AdminRoleModel.ADMIN,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )

    fun principal(): UserPrincipal =
      UserPrincipal(
        id = 1L,
        _username = "admin",
        userType = "ADMIN",
        _authorities = listOf(SimpleGrantedAuthority("ROLE_ADMIN")),
      )

    beforeTest {
      orderMutationService = mockk(relaxed = true)
      orderQueryService = mockk(relaxed = true)
      deliveryTaskMutationService = mockk(relaxed = true)
      adminService = mockk(relaxed = true)
      adminOrderMapper = mockk(relaxed = true)
      deliveryPhotoReviewService = mockk(relaxed = true)
      resolver =
        AdminOrderMutationResolver(
          orderMutationService,
          orderQueryService,
          deliveryTaskMutationService,
          adminService,
          adminOrderMapper,
          deliveryPhotoReviewService,
        )
      every { adminOrderMapper.map(any()) } returns order
    }
    test("cancelOrder should cancel order via service") {
      // Given
      every { orderMutationService.cancelOrderForAdmin(1L) } returns orderModel

      // When
      val result = resolver.cancelOrder(1L)

      // Then
      result.shouldNotBeNull()
      verify { orderMutationService.cancelOrderForAdmin(1L) }
      verify { adminOrderMapper.map(orderModel) }
    }
    test("cancelOrder should throw when order not found") {
      // Given
      every { orderMutationService.cancelOrderForAdmin(999L) } returns null

      // When & Then
      shouldThrow<IllegalArgumentException> { resolver.cancelOrder(999L) }
    }
    test("updateOrderStatus should update status via service") {
      // Given
      every { orderMutationService.updateOrderStatus(1L, OrderModelStatus.DELIVERING) } returns orderModel

      // When
      val result = resolver.updateOrderStatus(1L, OrderModelStatus.DELIVERING)

      // Then
      result.shouldNotBeNull()
      verify { orderMutationService.updateOrderStatus(1L, OrderModelStatus.DELIVERING) }
    }
    test("assignDeliveryWorker should delegate with principal id") {
      // Given
      every { deliveryTaskMutationService.assignDeliveryWorker(1L, 1L, 2L, true) } returns orderModel

      // When
      val result = resolver.assignDeliveryWorker(1L, 2L, true, principal())

      // Then
      result.shouldNotBeNull()
      verify { deliveryTaskMutationService.assignDeliveryWorker(1L, 1L, 2L, true) }
    }
    test("batchAssignOrders should delegate with principal id") {
      // Given
      every { deliveryTaskMutationService.batchAssignOrders(1L, listOf(1L, 2L), 3L) } returns listOf(orderModel)

      // When
      val result = resolver.batchAssignOrders(listOf(1L, 2L), 3L, principal())

      // Then
      result.size shouldBe 1
      verify { deliveryTaskMutationService.batchAssignOrders(1L, listOf(1L, 2L), 3L) }
    }
    test("createDeliveryOrder should create order and reload it") {
      // Given
      val input =
        CreateDeliveryOrderInput(
          addressId = 10L,
          isSelfCollect = false,
          items = listOf(OrderItemInput(productId = 20L, quantity = 2)),
          remark = "放门口",
        )
      every { adminService.findByUsername("admin") } returns createSampleAdmin()
      every { orderMutationService.createDeliveryOrder(1L, any()) } returns orderModel
      every { orderQueryService.getOrderById(1L) } returns orderModel

      // When
      val result = resolver.createDeliveryOrder(input, principal())

      // Then
      result.shouldNotBeNull()
      verify { orderMutationService.createDeliveryOrder(1L, any()) }
      verify { orderQueryService.getOrderById(1L) }
    }
    test("createDeliveryOrder should support multi-product items (F-ORD-012)") {
      // Given
      val input =
        CreateDeliveryOrderInput(
          addressId = 10L,
          isSelfCollect = true,
          items =
            listOf(
              OrderItemInput(productId = 20L, quantity = 2),
              OrderItemInput(productId = 30L, quantity = 1),
            ),
        )
      every { adminService.findByUsername("admin") } returns createSampleAdmin()
      every { orderMutationService.createDeliveryOrder(1L, any()) } returns orderModel
      every { orderQueryService.getOrderById(1L) } returns orderModel

      // When
      val result = resolver.createDeliveryOrder(input, principal())

      // Then
      result.shouldNotBeNull()
      verify { orderMutationService.createDeliveryOrder(1L, any()) }
      verify { orderQueryService.getOrderById(1L) }
    }
    test("createDeliveryOrder should throw when admin not found") {
      // Given
      every { adminService.findByUsername("unknown") } returns null
      val input = CreateDeliveryOrderInput(addressId = 10L, items = listOf(OrderItemInput(productId = 20L, quantity = 1)))
      val user = UserPrincipal(2L, "unknown", "ADMIN", emptyList())

      // When & Then
      shouldThrow<UserNotFoundException> { resolver.createDeliveryOrder(input, user) }
    }
    test("acceptDelivery should delegate with resolved admin id") {
      // Given
      every { adminService.findByUsername("admin") } returns createSampleAdmin()
      every { deliveryTaskMutationService.acceptDelivery(1L, 1L) } returns orderModel

      // When
      val result = resolver.acceptDelivery(1L, principal())

      // Then
      result.shouldNotBeNull()
      verify { deliveryTaskMutationService.acceptDelivery(1L, 1L) }
    }
    test("startDelivery should delegate") {
      // Given
      every { deliveryTaskMutationService.startDelivery(1L) } returns orderModel

      // When
      val result = resolver.startDelivery(1L)

      // Then
      result.shouldNotBeNull()
      verify { deliveryTaskMutationService.startDelivery(1L) }
    }
    test("completeDelivery should delegate with photos and payment type") {
      // Given
      every { deliveryTaskMutationService.completeDelivery(1L, listOf("a.jpg"), PaymentType.CASH, "已收款") } returns orderModel

      // When
      val result = resolver.completeDelivery(1L, listOf("a.jpg"), PaymentType.CASH, "已收款")

      // Then
      result.shouldNotBeNull()
      (result === order) shouldBe true
      verify { deliveryTaskMutationService.completeDelivery(1L, listOf("a.jpg"), PaymentType.CASH, "已收款") }
    }
    test("reviewDeliveryPhoto should delegate approve with principal id and username") {
      // Given
      every {
        deliveryPhotoReviewService.reviewDeliveryPhoto(1L, DeliveryPhotoReviewAction.APPROVE, null, 1L, "admin")
      } returns orderModel

      // When
      val result = resolver.reviewDeliveryPhoto(1L, DeliveryPhotoReviewAction.APPROVE, null, principal())

      // Then
      result.shouldNotBeNull()
      (result === order) shouldBe true
      verify {
        deliveryPhotoReviewService.reviewDeliveryPhoto(1L, DeliveryPhotoReviewAction.APPROVE, null, 1L, "admin")
      }
      verify { adminOrderMapper.map(orderModel) }
    }
    test("reviewDeliveryPhoto should delegate reject with remark") {
      // Given
      every {
        deliveryPhotoReviewService.reviewDeliveryPhoto(1L, DeliveryPhotoReviewAction.REJECT, "照片模糊无法确认", 1L, "admin")
      } returns orderModel

      // When
      val result = resolver.reviewDeliveryPhoto(1L, DeliveryPhotoReviewAction.REJECT, "照片模糊无法确认", principal())

      // Then
      result.shouldNotBeNull()
      verify {
        deliveryPhotoReviewService.reviewDeliveryPhoto(1L, DeliveryPhotoReviewAction.REJECT, "照片模糊无法确认", 1L, "admin")
      }
    }
  })
