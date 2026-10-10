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
import dev.yidafu.aqua.api.dto.TodayStatisticsDTO
import dev.yidafu.aqua.api.dto.WeekStatisticsDTO
import dev.yidafu.aqua.api.service.admin.AdminService
import dev.yidafu.aqua.api.service.order.DeliveryOrderQueryService
import dev.yidafu.aqua.api.service.order.OrderQueryService
import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.exception.UserNotFoundException
import dev.yidafu.aqua.common.graphql.generated.OrderStatus
import dev.yidafu.aqua.common.graphql.generated.OrderVo
import dev.yidafu.aqua.common.security.UserPrincipal
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.time.LocalDateTime

class WeappOrderQueryResolverTest :
  FunSpec({
    var deliveryTaskQueryService: DeliveryOrderQueryService = mockk()
    var orderQueryService: OrderQueryService = mockk()
    var adminService: AdminService = mockk()
    var adminOrderMapper: AdminOrderMapper = mockk()
    var resolver: WeappOrderQueryResolver = mockk()
    val order: OrderVo = mockk(relaxed = true)

    fun createSampleAdmin(role: AdminRoleModel = AdminRoleModel.DELIVERY_WORKER): AdminModel =
      AdminModel(
        id = 1L,
        username = "worker1",
        passwordHash = "hashed_password",
        realName = "送水员甲",
        phone = "13900139000",
        role = role,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )

    fun createSampleOrder(): OrderModel =
      OrderModel(
        id = 1L,
        orderNo = "ORD-20250101-001",
        userId = 1L,
        status = OrderModelStatus.DELIVERING,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )

    fun principal(): UserPrincipal =
      UserPrincipal(
        id = 1L,
        _username = "worker1",
        userType = "WORKER",
        _authorities = listOf(SimpleGrantedAuthority("ROLE_WORKER")),
      )

    beforeTest {
      deliveryTaskQueryService = mockk(relaxed = true)
      orderQueryService = mockk(relaxed = true)
      adminService = mockk(relaxed = true)
      adminOrderMapper = mockk(relaxed = true)
      resolver = WeappOrderQueryResolver(deliveryTaskQueryService, orderQueryService, adminService, adminOrderMapper)
      every { adminOrderMapper.map(any()) } returns order
    }
    test("pendingDeliveryOrders should return mapped orders") {
      // Given
      every { deliveryTaskQueryService.getPendingDeliveryOrders() } returns listOf(createSampleOrder())

      // When
      val result = resolver.pendingDeliveryOrders()

      // Then
      result.size shouldBe 1
      verify { deliveryTaskQueryService.getPendingDeliveryOrders() }
    }
    test("assignedOrders should use resolved admin id") {
      // Given
      every { adminService.findByUsername("worker1") } returns createSampleAdmin()
      every { deliveryTaskQueryService.getAssignedOrders(1L) } returns listOf(createSampleOrder())

      // When
      val result = resolver.assignedOrders(principal())

      // Then
      result.size shouldBe 1
      verify { deliveryTaskQueryService.getAssignedOrders(1L) }
    }
    test("assignedOrders should throw when admin not found") {
      // Given
      every { adminService.findByUsername("unknown") } returns null
      val user = UserPrincipal(2L, "unknown", "WORKER", emptyList())

      // When & Then
      shouldThrow<UserNotFoundException> { resolver.assignedOrders(user) }
    }
    test("deliveringOrders should query delivering status") {
      // Given
      every { adminService.findByUsername("worker1") } returns createSampleAdmin()
      every { deliveryTaskQueryService.getOrdersByStatus(1L, OrderModelStatus.DELIVERING) } returns listOf(createSampleOrder())

      // When
      val result = resolver.deliveringOrders(principal())

      // Then
      result.size shouldBe 1
      verify { deliveryTaskQueryService.getOrdersByStatus(1L, OrderModelStatus.DELIVERING) }
    }
    test("todayStatistics should map stats for worker") {
      // Given
      every { adminService.findByUsername("worker1") } returns createSampleAdmin()
      every { deliveryTaskQueryService.getTodayStatistics(1L) } returns
        TodayStatisticsDTO(totalOrders = 5, completedOrders = 3, unfinishedOrders = 2, earningCents = 6600)

      // When
      val result = resolver.todayStatistics(principal())

      // Then
      result.completedOrders shouldBe 3
      verify { deliveryTaskQueryService.getTodayStatistics(1L) }
    }
    test("weekStatistics should map weekly stats") {
      // Given
      every { adminService.findByUsername("worker1") } returns createSampleAdmin()
      every { deliveryTaskQueryService.getWeekStatistics(1L) } returns
        WeekStatisticsDTO(dailyStats = emptyList(), totalOrders = 20, totalEarningCents = 40000)

      // When
      val result = resolver.weekStatistics(principal())

      // Then
      result.totalOrders shouldBe 20
      verify { deliveryTaskQueryService.getWeekStatistics(1L) }
    }
    test("deliveryWorkerHistoryOrders should resolve worker id for delivery role") {
      // Given
      every { adminService.findByUsername("worker1") } returns createSampleAdmin(AdminRoleModel.DELIVERY_WORKER)
      every {
        orderQueryService.getDeliveryWorkerHistoryOrders(1L, OrderModelStatus.COMPLETED, "关键词", 0, 20)
      } returns PageImpl(listOf(createSampleOrder()))

      // When
      val result = resolver.deliveryWorkerHistoryOrders(OrderStatus.COMPLETED, "关键词", 0, 20, principal())

      // Then
      result.content.size shouldBe 1
      verify { orderQueryService.getDeliveryWorkerHistoryOrders(1L, OrderModelStatus.COMPLETED, "关键词", 0, 20) }
    }
    test("deliveryWorkerHistoryOrders should pass null worker id for non delivery roles") {
      // Given
      every { adminService.findByUsername("manager") } returns createSampleAdmin(AdminRoleModel.SUPER_ADMIN)
      every { orderQueryService.getDeliveryWorkerHistoryOrders(null, null, null, 0, 20) } returns PageImpl(emptyList())
      val user = UserPrincipal(2L, "manager", "ADMIN", emptyList())

      // When
      val result = resolver.deliveryWorkerHistoryOrders(null, null, 0, 20, user)

      // Then
      result.content.size shouldBe 0
      verify { orderQueryService.getDeliveryWorkerHistoryOrders(null, null, null, 0, 20) }
    }
  })
