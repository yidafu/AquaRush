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

package dev.yidafu.aqua.analytics.reconciliation.resolvers

import dev.yidafu.aqua.analytics.reconciliation.dto.OrderStatsDTO
import dev.yidafu.aqua.analytics.reconciliation.dto.PaymentStatsDTO
import dev.yidafu.aqua.analytics.reconciliation.service.DailyCollectionService
import dev.yidafu.aqua.api.service.admin.AdminService
import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.common.domain.model.DailyCollectionRecordModel
import dev.yidafu.aqua.common.domain.model.DailyReconciliationModel
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import dev.yidafu.aqua.common.domain.model.enums.DailyCollectionStatus
import dev.yidafu.aqua.common.exception.UnauthorizedException
import dev.yidafu.aqua.common.exception.UserNotFoundException
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.core.userdetails.UserDetails
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * 管理员每日收款查询解析器单元测试: 登录校验、日期解析、今日预计算(已有记录/无记录两条路径)
 */
class AdminDailyCollectionQueryResolverTest {
  private lateinit var dailyCollectionService: DailyCollectionService
  private lateinit var adminService: AdminService
  private lateinit var resolver: AdminDailyCollectionQueryResolver

  private val user = mockk<UserDetails>(relaxed = true)

  @BeforeEach
  fun setUp() {
    dailyCollectionService = mockk(relaxed = true)
    adminService = mockk(relaxed = true)
    resolver = AdminDailyCollectionQueryResolver(dailyCollectionService, adminService)
    every { user.username } returns "admin1"
  }

  private fun admin(): AdminModel =
    AdminModel(username = "admin1", passwordHash = "hash", role = AdminRoleModel.ADMIN).apply {
      id = 1L
    }

  @Test
  fun `dailyCollections requires login`() {
    assertFailsWith<UnauthorizedException> { resolver.dailyCollections(null, null) }
  }

  @Test
  fun `dailyCollections without date returns all collections`() {
    val records = listOf(DailyCollectionRecordModel(deliveryWorkerId = 1L))
    every { dailyCollectionService.getAllCollections() } returns records

    val result = resolver.dailyCollections(null, user)

    assertEquals(records, result)
  }

  @Test
  fun `dailyCollections with date delegates by admin`() {
    val records = listOf(DailyCollectionRecordModel(deliveryWorkerId = 1L))
    every { adminService.findByUsername("admin1") } returns admin()
    every { dailyCollectionService.getCollectionsByDate(1L, LocalDate.of(2025, 6, 1)) } returns records

    val result = resolver.dailyCollections("2025-06-01", user)

    assertEquals(records, result)
  }

  @Test
  fun `dailyCollections throws when admin missing`() {
    every { adminService.findByUsername("admin1") } returns null

    assertFailsWith<UserNotFoundException> { resolver.dailyCollections("2025-06-01", user) }
  }

  @Test
  fun `dailyCollections throws when admin id missing`() {
    val admin = AdminModel(username = "admin1", passwordHash = "hash", role = AdminRoleModel.ADMIN)
    every { adminService.findByUsername("admin1") } returns admin

    assertFailsWith<dev.yidafu.aqua.common.exception.BadRequestException> { resolver.dailyCollections("2025-06-01", user) }
  }

  @Test
  fun `dailyReconciliations sorts by date desc`() {
    val older = DailyReconciliationModel(reconciliationDate = LocalDate.of(2025, 5, 1))
    val newer = DailyReconciliationModel(reconciliationDate = LocalDate.of(2025, 6, 1))
    every { dailyCollectionService.getAllReconciliations() } returns listOf(older, newer)

    val result = resolver.dailyReconciliations(page = 0, size = 20, userDetails = user)

    assertEquals(listOf(newer, older), result)
  }

  @Test
  fun `dailyReconciliations requires login`() {
    assertFailsWith<UnauthorizedException> { resolver.dailyReconciliations(0, 20, null) }
  }

  @Test
  fun `myTodayCollection uses existing record when present`() {
    every { adminService.findByUsername("admin1") } returns admin()
    every { dailyCollectionService.getTodayOrderStats(1L, any()) } returns OrderStatsDTO(orderCount = 3, orderAmountCents = 600L)
    every { dailyCollectionService.getCollectionByWorkerAndDate(1L, any()) } returns
      DailyCollectionRecordModel(
        deliveryWorkerId = 1L,
        cashAmountCents = 600L,
        waterTicketCount = 1,
        qrCodeAmountCents = 0L,
        orderCount = 3,
        orderAmountCents = 600L,
        differenceAmountCents = 0L,
        status = DailyCollectionStatus.SUBMITTED,
        calculatedCashAmountCents = 600L,
        calculatedQrCodeAmountCents = 0L,
        calculatedWaterTicketCount = 1,
        calculatedOrderAmountCents = 600L,
      )

    val result = resolver.myTodayCollection(user)

    assertNotNull(result)
    assertEquals(3, result.orderCount)
    assertEquals(600L, result.cashAmountCents)
  }

  @Test
  fun `myTodayCollection falls back to payment stats when no record`() {
    every { adminService.findByUsername("admin1") } returns admin()
    every { dailyCollectionService.getTodayOrderStats(1L, any()) } returns OrderStatsDTO(orderCount = 2, orderAmountCents = 400L)
    every { dailyCollectionService.getCollectionByWorkerAndDate(1L, any()) } returns null
    every { dailyCollectionService.getTodayPaymentStats(1L, any()) } returns
      PaymentStatsDTO(cashAmountCents = 300L, waterTicketCount = 1, qrCodeAmountCents = 100L)

    val result = resolver.myTodayCollection(user)

    assertNotNull(result)
    assertEquals(2, result.orderCount)
    assertEquals(300L, result.cashAmountCents)
    assertNull(result.differenceAmountCents)
    assertNull(result.status)
  }

  @Test
  fun `myTodayCollection throws when worker missing`() {
    every { adminService.findByUsername("admin1") } returns null

    assertFailsWith<UserNotFoundException> { resolver.myTodayCollection(user) }
  }
}
