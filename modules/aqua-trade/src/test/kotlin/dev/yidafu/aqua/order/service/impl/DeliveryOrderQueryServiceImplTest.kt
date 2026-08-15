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
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * 配送端订单查询服务单元测试:派单权限校验、今日/本周统计、历史订单。
 */
class DeliveryOrderQueryServiceImplTest {
  private lateinit var adminService: AdminService
  private lateinit var orderRepository: OrderRepository
  private lateinit var service: DeliveryOrderQueryServiceImpl

  private fun order(
    id: Long,
    status: OrderModelStatus = OrderModelStatus.PENDING_DELIVERY,
    workerId: Long? = null,
  ) = OrderModel(id = id, userId = 100L, orderNo = "ORD-$id", status = status, deliveryWorkerId = workerId)

  private fun worker(
    id: Long? = 5L,
    role: AdminRoleModel = AdminRoleModel.DELIVERY_WORKER,
  ) = AdminModel(id = id, username = "worker$id", passwordHash = "x", realName = "张三", role = role)

  @BeforeEach
  fun setUp() {
    adminService = mockk(relaxed = true)
    orderRepository = mockk(relaxed = true)
    service = DeliveryOrderQueryServiceImpl(adminService, orderRepository)
  }

  @Test
  @DisplayName("getWorkerTasks 返回配送员全部订单")
  fun `getWorkerTasks delegates to repository`() {
    val orders = listOf(order(1L, workerId = 5L), order(2L, workerId = 5L))
    every { orderRepository.findByDeliveryWorkerIdOrderByCreatedAtDesc(5L) } returns orders

    assertEquals(orders, service.getWorkerTasks(5L))
  }

  @Test
  @DisplayName("getOrdersByStatus/getPendingDeliveryOrders 透传")
  fun `status queries pass through`() {
    val delivering = listOf(order(1L, status = OrderModelStatus.DELIVERING))
    val pending = listOf(order(2L, status = OrderModelStatus.PENDING_DISPATCH))
    every { orderRepository.findByStatusOrderByCreatedAtAsc(OrderModelStatus.DELIVERING) } returns delivering
    every { orderRepository.findByStatusOrderByCreatedAtAsc(OrderModelStatus.PENDING_DISPATCH) } returns pending

    assertEquals(delivering, service.getOrdersByStatus(5L, OrderModelStatus.DELIVERING))
    assertEquals(pending, service.getPendingDeliveryOrders())
  }

  @Test
  @DisplayName("getAssignedOrders 送水员/管理员账号可取待配送订单")
  fun `getAssignedOrders returns pending delivery orders`() {
    every { adminService.findById(1L) } returns worker(id = 5L, role = AdminRoleModel.DELIVERY_WORKER)
    val orders = listOf(order(1L, status = OrderModelStatus.PENDING_DELIVERY), order(2L, status = OrderModelStatus.PENDING_DELIVERY))
    every { orderRepository.findByStatusOrderByCreatedAtAsc(OrderModelStatus.PENDING_DELIVERY) } returns orders

    assertEquals(orders, service.getAssignedOrders(1L))
    // 派单账号 id 与送水员账号 id 一致(迁移 v1.2.1)
    verify { orderRepository.findByStatusOrderByCreatedAtAsc(OrderModelStatus.PENDING_DELIVERY) }
  }

  @Test
  @DisplayName("getAssignedOrders 管理员角色同样放行")
  fun `getAssignedOrders allows admin role`() {
    every { adminService.findById(1L) } returns worker(id = 9L, role = AdminRoleModel.ADMIN)
    every { orderRepository.findByStatusOrderByCreatedAtAsc(OrderModelStatus.PENDING_DELIVERY) } returns emptyList()

    assertTrue(service.getAssignedOrders(1L).isEmpty())
  }

  @Test
  @DisplayName("getAssignedOrders 非配送角色/账号不存在/无账号 id 抛 UserNotFoundException")
  fun `getAssignedOrders rejects non delivery roles`() {
    every { adminService.findById(1L) } returns worker(id = 5L, role = AdminRoleModel.SUPER_ADMIN)
    every { adminService.findById(2L) } returns null
    every { adminService.findById(3L) } returns worker(id = null, role = AdminRoleModel.DELIVERY_WORKER)

    assertFailsWith<UserNotFoundException> { service.getAssignedOrders(1L) }
    assertFailsWith<UserNotFoundException> { service.getAssignedOrders(2L) }
    assertFailsWith<UserNotFoundException> { service.getAssignedOrders(3L) }
  }

  @Test
  @DisplayName("getDeliveryStatistics 汇总配送员/订单统计")
  fun `getDeliveryStatistics aggregates statistics`() {
    val capable = listOf(worker(id = 1L), worker(id = 2L), worker(id = 3L))
    val online = listOf(worker(id = 1L))
    every { adminService.findAllDeliveryCapable() } returns capable
    every { adminService.findByOnlineStatus(DeliverWorkerModelStatus.ONLINE) } returns online
    every { orderRepository.findByStatusOrderByCreatedAtAsc(OrderModelStatus.PENDING_DISPATCH) } returns
      listOf(order(1L, status = OrderModelStatus.PENDING_DISPATCH))
    every { orderRepository.countByStatus(OrderModelStatus.DELIVERING) } returns 4L

    val stats = service.getDeliveryStatistics()

    assertEquals(3, stats.totalWorkers)
    assertEquals(1, stats.onlineWorkers)
    assertEquals(1, stats.pendingOrders)
    assertEquals(4, stats.deliveringOrders)
  }

  @Test
  @DisplayName("getTodayStatistics 汇总今日订单/收益")
  fun `getTodayStatistics aggregates today`() {
    every { orderRepository.countOrdersByDateRange(any(), any(), 5L, null) } returns 10L
    every {
      orderRepository.countOrdersByDateRange(any(), any(), 5L, listOf(OrderModelStatus.COMPLETED))
    } returns 3L
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
    every {
      orderRepository.sumAmountCentsByStatusAndDateRange(listOf(OrderModelStatus.COMPLETED), any(), any(), 5L)
    } returns 8000L

    val stats = service.getTodayStatistics(5L)

    assertEquals(10, stats.totalOrders)
    assertEquals(3, stats.completedOrders)
    assertEquals(2, stats.unfinishedOrders)
    assertEquals(8000L, stats.earningCents)
  }

  @Test
  @DisplayName("getOrderById 命中返回,未命中抛 NotFoundException")
  fun `getOrderById returns order or throws`() {
    val order = order(1L)
    every { orderRepository.findById(1L) } returns Optional.of(order)
    every { orderRepository.findById(9L) } returns Optional.empty()

    assertEquals(order, service.getOrderById(1L))
    assertFailsWith<NotFoundException> { service.getOrderById(9L) }
  }

  @Test
  @DisplayName("getWeekStatistics 生成最近7天日报并汇总")
  fun `getWeekStatistics builds 7 daily stats`() {
    every { orderRepository.countOrdersByDateRange(any(), any(), 5L, listOf(OrderModelStatus.COMPLETED)) } returnsMany
      listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L)
    every {
      orderRepository.sumAmountCentsByStatusAndDateRange(listOf(OrderModelStatus.COMPLETED), any(), any(), 5L)
    } returnsMany
      listOf(100L, 200L, 300L, 400L, 500L, 600L, 700L)

    val stats = service.getWeekStatistics(5L)

    assertEquals(7, stats.dailyStats.size)
    assertEquals(28, stats.totalOrders)
    assertEquals(2800L, stats.totalEarningCents)
    assertEquals(
      "MM/dd".length,
      stats.dailyStats
        .first()
        .date.length,
    )
    verify(exactly = 7) { orderRepository.countOrdersByDateRange(any(), any(), 5L, listOf(OrderModelStatus.COMPLETED)) }
  }

  @Test
  @DisplayName("getWeekStatistics 空数据日按 0 计入")
  fun `getWeekStatistics handles zero days`() {
    every { orderRepository.countOrdersByDateRange(any(), any(), null, listOf(OrderModelStatus.COMPLETED)) } returns 0L
    every {
      orderRepository.sumAmountCentsByStatusAndDateRange(listOf(OrderModelStatus.COMPLETED), any(), any(), null)
    } returns 0L

    val stats = service.getWeekStatistics(null)

    assertEquals(7, stats.dailyStats.size)
    assertEquals(0, stats.totalOrders)
    assertEquals(0L, stats.totalEarningCents)
  }
}
