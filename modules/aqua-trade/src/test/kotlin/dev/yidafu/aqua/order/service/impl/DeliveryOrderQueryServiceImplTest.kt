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

package dev.yidafu.aqua.order.service.impl

import dev.yidafu.aqua.api.service.admin.AdminService
import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.common.domain.model.DeliverWorkerModelStatus
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.exception.NotFoundException
import dev.yidafu.aqua.common.exception.UserNotFoundException
import dev.yidafu.aqua.order.domain.repository.OrderRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.Optional

/**
 * 配送端订单查询服务单元测试:派单权限校验、今日/本周统计、历史订单。
 */
class DeliveryOrderQueryServiceImplTest :
  FunSpec({
    var adminService: AdminService = mockk()
    var orderRepository: OrderRepository = mockk()
    var service: DeliveryOrderQueryServiceImpl = mockk()

    fun order(
      id: Long,
      status: OrderModelStatus = OrderModelStatus.PENDING_DELIVERY,
      workerId: Long? = null,
    ) = OrderModel(id = id, userId = 100L, orderNo = "ORD-$id", status = status, deliveryWorkerId = workerId)

    fun worker(
      id: Long? = 5L,
      role: AdminRoleModel = AdminRoleModel.DELIVERY_WORKER,
    ) = AdminModel(id = id, username = "worker$id", passwordHash = "x", realName = "张三", role = role)

    beforeTest {
      adminService = mockk(relaxed = true)
      orderRepository = mockk(relaxed = true)
      service = DeliveryOrderQueryServiceImpl(adminService, orderRepository)
    }
    test("getWorkerTasks delegates to repository") {
      val orders = listOf(order(1L, workerId = 5L), order(2L, workerId = 5L))
      every { orderRepository.findByDeliveryWorkerIdOrderByCreatedAtDesc(5L) } returns orders

      service.getWorkerTasks(5L) shouldBe orders
    }
    test("status queries pass through") {
      val delivering = listOf(order(1L, status = OrderModelStatus.DELIVERING))
      val pending = listOf(order(2L, status = OrderModelStatus.PENDING_DISPATCH))
      every { orderRepository.findByStatusOrderByCreatedAtAsc(OrderModelStatus.DELIVERING) } returns delivering
      every { orderRepository.findByStatusOrderByCreatedAtAsc(OrderModelStatus.PENDING_DISPATCH) } returns pending

      service.getOrdersByStatus(5L, OrderModelStatus.DELIVERING) shouldBe delivering
      service.getPendingDeliveryOrders() shouldBe pending
    }
    test("getAssignedOrders returns pending delivery orders") {
      every { adminService.findById(1L) } returns worker(id = 5L, role = AdminRoleModel.DELIVERY_WORKER)
      val orders = listOf(order(1L, status = OrderModelStatus.PENDING_DELIVERY), order(2L, status = OrderModelStatus.PENDING_DELIVERY))
      every { orderRepository.findByStatusOrderByCreatedAtAsc(OrderModelStatus.PENDING_DELIVERY) } returns orders

      service.getAssignedOrders(1L) shouldBe orders
      // 派单账号 id 与送水员账号 id 一致(迁移 v1.2.1)
      verify { orderRepository.findByStatusOrderByCreatedAtAsc(OrderModelStatus.PENDING_DELIVERY) }
    }
    test("getAssignedOrders allows admin role") {
      every { adminService.findById(1L) } returns worker(id = 9L, role = AdminRoleModel.ADMIN)
      every { orderRepository.findByStatusOrderByCreatedAtAsc(OrderModelStatus.PENDING_DELIVERY) } returns emptyList()

      service.getAssignedOrders(1L).isEmpty() shouldBe true
    }
    test("getAssignedOrders rejects non delivery roles") {
      every { adminService.findById(1L) } returns worker(id = 5L, role = AdminRoleModel.SUPER_ADMIN)
      every { adminService.findById(2L) } returns null
      every { adminService.findById(3L) } returns worker(id = null, role = AdminRoleModel.DELIVERY_WORKER)

      shouldThrow<UserNotFoundException> { service.getAssignedOrders(1L) }
      shouldThrow<UserNotFoundException> { service.getAssignedOrders(2L) }
      shouldThrow<UserNotFoundException> { service.getAssignedOrders(3L) }
    }
    test("getDeliveryStatistics aggregates statistics") {
      val capable = listOf(worker(id = 1L), worker(id = 2L), worker(id = 3L))
      val online = listOf(worker(id = 1L))
      every { adminService.findAllDeliveryCapable() } returns capable
      every { adminService.findByOnlineStatus(DeliverWorkerModelStatus.ONLINE) } returns online
      every { orderRepository.findByStatusOrderByCreatedAtAsc(OrderModelStatus.PENDING_DISPATCH) } returns
        listOf(order(1L, status = OrderModelStatus.PENDING_DISPATCH))
      every { orderRepository.countByStatus(OrderModelStatus.DELIVERING) } returns 4L

      val stats = service.getDeliveryStatistics()

      stats.totalWorkers shouldBe 3
      stats.onlineWorkers shouldBe 1
      stats.pendingOrders shouldBe 1
      stats.deliveringOrders shouldBe 4
    }
    test("getTodayStatistics aggregates today") {
      every { orderRepository.countOrdersByDateRange(any(), any(), 5L, null) } returns 10L
      // 业绩口径(R-ORD-11):含 待确认收货 + 已完成,按送达时间归属
      every { orderRepository.countDeliveredByDateRange(any(), any(), 5L) } returns 3L
      every {
        orderRepository.countOrdersByDateRange(
          any(),
          any(),
          5L,
          listOf(
            OrderModelStatus.DELIVERING,
            OrderModelStatus.PENDING_DELIVERY,
            OrderModelStatus.PENDING_DISPATCH,
            OrderModelStatus.PENDING_DELIVERY,
          ),
        )
      } returns 2L
      every { orderRepository.sumDeliveredAmountCentsByDateRange(any(), any(), 5L) } returns 8000L

      val stats = service.getTodayStatistics(5L)

      stats.totalOrders shouldBe 10
      stats.completedOrders shouldBe 3
      stats.unfinishedOrders shouldBe 2
      stats.earningCents shouldBe 8000L
    }
    test("getOrderById returns order or throws") {
      val order = order(1L)
      every { orderRepository.findById(1L) } returns Optional.of(order)
      every { orderRepository.findById(9L) } returns Optional.empty()

      service.getOrderById(1L) shouldBe order
      shouldThrow<NotFoundException> { service.getOrderById(9L) }
    }
    test("getWeekStatistics builds 7 daily stats") {
      every { orderRepository.countDeliveredByDateRange(any(), any(), 5L) } returnsMany
        listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L)
      every { orderRepository.sumDeliveredAmountCentsByDateRange(any(), any(), 5L) } returnsMany
        listOf(100L, 200L, 300L, 400L, 500L, 600L, 700L)

      val stats = service.getWeekStatistics(5L)

      stats.dailyStats.size shouldBe 7
      stats.totalOrders shouldBe 28
      stats.totalEarningCents shouldBe 2800L
      stats.dailyStats
        .first()
        .date.length shouldBe "MM/dd".length
      verify(exactly = 7) { orderRepository.countDeliveredByDateRange(any(), any(), 5L) }
    }
    test("getWeekStatistics handles zero days") {
      every { orderRepository.countDeliveredByDateRange(any(), any(), null) } returns 0L
      every { orderRepository.sumDeliveredAmountCentsByDateRange(any(), any(), null) } returns 0L

      val stats = service.getWeekStatistics(null)

      stats.dailyStats.size shouldBe 7
      stats.totalOrders shouldBe 0
      stats.totalEarningCents shouldBe 0L
    }
  })
