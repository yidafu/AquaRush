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

import dev.yidafu.aqua.analytics.reconciliation.service.DailyCollectionService
import dev.yidafu.aqua.api.service.admin.AdminService
import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.common.domain.model.DailyCollectionRecordModel
import dev.yidafu.aqua.common.domain.model.DailyReconciliationModel
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import dev.yidafu.aqua.common.domain.model.enums.DailyCollectionStatus
import dev.yidafu.aqua.common.domain.model.enums.DailyReconciliationStatus
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.exception.UnauthorizedException
import dev.yidafu.aqua.common.graphql.generated.ConfirmDailyCollectionInput
import dev.yidafu.aqua.common.graphql.generated.ReviewReconciliationInput
import dev.yidafu.aqua.common.graphql.generated.SubmitDailyCollectionInput
import dev.yidafu.aqua.logging.util.BizLogger
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.core.userdetails.UserDetails
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

/**
 * 管理员每日收款变更解析器单元测试: 提交/确认/复核/手动对账的成功与校验分支
 */
class AdminDailyCollectionMutationResolverTest {
  private lateinit var dailyCollectionService: DailyCollectionService
  private lateinit var adminService: AdminService
  private lateinit var bizLogger: BizLogger
  private lateinit var resolver: AdminDailyCollectionMutationResolver

  private val user = mockk<UserDetails>(relaxed = true)

  @BeforeEach
  fun setUp() {
    dailyCollectionService = mockk(relaxed = true)
    adminService = mockk(relaxed = true)
    bizLogger = mockk(relaxed = true)
    resolver = AdminDailyCollectionMutationResolver(dailyCollectionService, adminService, bizLogger)
    every { user.username } returns "admin1"
  }

  private fun admin(): AdminModel =
    AdminModel(username = "admin1", passwordHash = "hash", role = AdminRoleModel.DELIVERY_WORKER).apply {
      id = 1L
    }

  @Test
  fun `submitDailyCollection resolves worker and logs`() {
    every { adminService.findByUsername("admin1") } returns admin()
    val record = DailyCollectionRecordModel(deliveryWorkerId = 1L, status = DailyCollectionStatus.SUBMITTED)
    every { dailyCollectionService.submitDailyCollection(any(), any(), any(), any(), any()) } returns record

    val result =
      resolver.submitDailyCollection(
        SubmitDailyCollectionInput(cashAmountCents = 1000L, qrCodeAmountCents = 200L, waterTicketCount = 1, collectionDate = "2025-06-01"),
        user,
      )

    assertEquals(record, result)
    verify { bizLogger.logOperation("RECON_DAILY_SUBMIT", "RECONCILIATION", "SUCCESS", any(), 1L, "admin1", any()) }
  }

  @Test
  fun `submitDailyCollection uses today when date omitted`() {
    every { adminService.findByUsername("admin1") } returns admin()
    every { dailyCollectionService.submitDailyCollection(any(), any(), any(), any(), any()) } returns
      DailyCollectionRecordModel(deliveryWorkerId = 1L)

    resolver.submitDailyCollection(
      SubmitDailyCollectionInput(cashAmountCents = 100L, qrCodeAmountCents = 0L, waterTicketCount = 0),
      user,
    )

    verify { dailyCollectionService.submitDailyCollection(1L, 100L, 0, 0L, LocalDate.now()) }
  }

  @Test
  fun `submitDailyCollection throws when user missing`() {
    every { adminService.findByUsername("admin1") } returns null

    assertFailsWith<BadRequestException> {
      resolver.submitDailyCollection(SubmitDailyCollectionInput(cashAmountCents = 1L, qrCodeAmountCents = 0L, waterTicketCount = 0), user)
    }
  }

  @Test
  fun `confirmDailyCollection requires login`() {
    assertFailsWith<UnauthorizedException> { resolver.confirmDailyCollection(null, null) }
  }

  @Test
  fun `confirmDailyCollection rejects null input`() {
    assertFailsWith<BadRequestException> { resolver.confirmDailyCollection(null, user) }
  }

  @Test
  fun `confirmDailyCollection confirms record and logs`() {
    every { adminService.findByUsername("admin1") } returns admin()
    val record = DailyCollectionRecordModel(deliveryWorkerId = 1L, status = DailyCollectionStatus.CONFIRMED)
    every { dailyCollectionService.confirmCollection(9L, 1L, "OK") } returns record

    val result = resolver.confirmDailyCollection(ConfirmDailyCollectionInput(recordId = 9L, notes = "OK"), user)

    assertEquals(record, result)
    verify { bizLogger.logOperation("RECON_DAILY_CONFIRM", "RECONCILIATION", "SUCCESS", any(), 1L, "admin1", any()) }
  }

  @Test
  fun `reviewReconciliation requires login`() {
    assertFailsWith<UnauthorizedException> { resolver.reviewReconciliation(null, null) }
  }

  @Test
  fun `reviewReconciliation rejects blank notes`() {
    assertFailsWith<BadRequestException> {
      resolver.reviewReconciliation(ReviewReconciliationInput(reconciliationId = 3L, reviewNotes = "  "), user)
    }
  }

  @Test
  fun `reviewReconciliation reviews and logs`() {
    every { adminService.findByUsername("admin1") } returns admin()
    val model = DailyReconciliationModel(status = DailyReconciliationStatus.REVIEWED)
    every { dailyCollectionService.reviewReconciliation(3L, 1L, "无误") } returns model

    val result = resolver.reviewReconciliation(ReviewReconciliationInput(reconciliationId = 3L, reviewNotes = "无误"), user)

    assertEquals(model, result)
    verify { bizLogger.logOperation("RECON_REVIEW", "RECONCILIATION", "SUCCESS", any(), 1L, "admin1", any()) }
  }

  @Test
  fun `executeDailyReconciliation runs for explicit date`() {
    every { adminService.findByUsername("admin1") } returns admin()
    every { dailyCollectionService.getReconciliationByDate(any()) } returns
      DailyReconciliationModel(reconciliationDate = LocalDate.of(2025, 6, 1))

    val result = resolver.executeDailyReconciliation("2025-06-01", user)

    assertNotNull(result)
    verify { dailyCollectionService.executeDailyReconciliation(LocalDate.of(2025, 6, 1)) }
  }

  @Test
  fun `executeDailyReconciliation defaults to yesterday`() {
    every { adminService.findByUsername("admin1") } returns admin()
    every { dailyCollectionService.getReconciliationByDate(any()) } returns
      DailyReconciliationModel(reconciliationDate = LocalDate.now().minusDays(1))

    val result = resolver.executeDailyReconciliation(null, user)

    assertNotNull(result)
    verify { dailyCollectionService.executeDailyReconciliation(LocalDate.now().minusDays(1)) }
  }

  @Test
  fun `executeDailyReconciliation throws when reconciliation missing`() {
    every { adminService.findByUsername("admin1") } returns admin()
    every { dailyCollectionService.getReconciliationByDate(any()) } returns null

    assertFailsWith<BadRequestException> { resolver.executeDailyReconciliation("2025-06-01", user) }
  }

  @Test
  fun `executeDailyReconciliation requires login`() {
    assertFailsWith<UnauthorizedException> { resolver.executeDailyReconciliation(null, null) }
  }
}
