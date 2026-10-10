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

import dev.yidafu.aqua.analytics.reconciliation.mapper.toGraphQL
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
import dev.yidafu.aqua.common.graphql.generated.RejectDailyCollectionInput
import dev.yidafu.aqua.common.graphql.generated.ReviewReconciliationInput
import dev.yidafu.aqua.common.graphql.generated.SubmitDailyCollectionInput
import dev.yidafu.aqua.logging.util.BizLogger
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.security.core.userdetails.UserDetails
import java.time.LocalDate

/**
 * 管理员每日收款变更解析器单元测试: 提交/确认/复核/手动对账的成功与校验分支
 */
class AdminDailyCollectionMutationResolverTest :
  FunSpec({
    var dailyCollectionService: DailyCollectionService = mockk()
    var adminService: AdminService = mockk()
    var bizLogger: BizLogger = mockk()
    var resolver: AdminDailyCollectionMutationResolver = mockk()
    val user = mockk<UserDetails>(relaxed = true)

    fun admin(): AdminModel =
      AdminModel(username = "admin1", passwordHash = "hash", role = AdminRoleModel.DELIVERY_WORKER).apply {
        id = 1L
      }

    beforeTest {
      dailyCollectionService = mockk(relaxed = true)
      adminService = mockk(relaxed = true)
      bizLogger = mockk(relaxed = true)
      resolver = AdminDailyCollectionMutationResolver(dailyCollectionService, adminService, bizLogger)
      every { user.username } returns "admin1"
    }
    test("submitDailyCollection resolves worker and logs") {
      every { adminService.findByUsername("admin1") } returns admin()
      val record = DailyCollectionRecordModel(deliveryWorkerId = 1L, status = DailyCollectionStatus.SUBMITTED)
      every { dailyCollectionService.submitDailyCollection(any(), any(), any(), any(), any(), any()) } returns record

      val result =
        resolver.submitDailyCollection(
          SubmitDailyCollectionInput(
            cashAmountCents = 1000L,
            qrCodeAmountCents = 200L,
            waterTicketCount = 1,
            collectionDate = "2025-06-01",
            notes = "差异说明",
          ),
          user,
        )

      result shouldBe record.toGraphQL()
      verify { bizLogger.logOperation("RECON_DAILY_SUBMIT", "RECONCILIATION", "SUCCESS", any(), 1L, "admin1", any()) }
    }
    test("submitDailyCollection uses today when date omitted") {
      every { adminService.findByUsername("admin1") } returns admin()
      every { dailyCollectionService.submitDailyCollection(any(), any(), any(), any(), any(), any()) } returns
        DailyCollectionRecordModel(deliveryWorkerId = 1L)

      resolver.submitDailyCollection(
        SubmitDailyCollectionInput(cashAmountCents = 100L, qrCodeAmountCents = 0L, waterTicketCount = 0),
        user,
      )

      verify { dailyCollectionService.submitDailyCollection(1L, 100L, 0, 0L, LocalDate.now(), null) }
    }
    test("submitDailyCollection throws when user missing") {
      every { adminService.findByUsername("admin1") } returns null

      shouldThrow<BadRequestException> {
        resolver.submitDailyCollection(SubmitDailyCollectionInput(cashAmountCents = 1L, qrCodeAmountCents = 0L, waterTicketCount = 0), user)
      }
    }
    test("confirmDailyCollection requires login") {
      shouldThrow<UnauthorizedException> { resolver.confirmDailyCollection(null, null) }
    }
    test("confirmDailyCollection rejects null input") {
      shouldThrow<BadRequestException> { resolver.confirmDailyCollection(null, user) }
    }
    test("confirmDailyCollection confirms record and logs") {
      every { adminService.findByUsername("admin1") } returns admin()
      val record = DailyCollectionRecordModel(deliveryWorkerId = 1L, status = DailyCollectionStatus.CONFIRMED)
      every { dailyCollectionService.confirmCollection(9L, 1L, "OK") } returns record

      val result = resolver.confirmDailyCollection(ConfirmDailyCollectionInput(recordId = 9L, notes = "OK"), user)

      result shouldBe record.toGraphQL()
      verify { bizLogger.logOperation("RECON_DAILY_CONFIRM", "RECONCILIATION", "SUCCESS", any(), 1L, "admin1", any()) }
    }
    test("reviewReconciliation requires login") {
      shouldThrow<UnauthorizedException> { resolver.reviewReconciliation(null, null) }
    }
    test("rejectDailyCollection requires login") {
      shouldThrow<UnauthorizedException> { resolver.rejectDailyCollection(null, null) }
    }
    test("rejectDailyCollection rejects null input") {
      shouldThrow<BadRequestException> { resolver.rejectDailyCollection(null, user) }
    }
    test("rejectDailyCollection rejects blank reason") {
      shouldThrow<BadRequestException> {
        resolver.rejectDailyCollection(RejectDailyCollectionInput(recordId = 9L, reason = "  "), user)
      }
    }
    test("rejectDailyCollection rejects record and logs") {
      every { adminService.findByUsername("admin1") } returns admin()
      val record = DailyCollectionRecordModel(deliveryWorkerId = 1L, status = DailyCollectionStatus.DRAFT)
      every { dailyCollectionService.rejectCollection(9L, 1L, "金额不符") } returns record

      val result = resolver.rejectDailyCollection(RejectDailyCollectionInput(recordId = 9L, reason = "金额不符"), user)

      result shouldBe record.toGraphQL()
      verify { bizLogger.logOperation("RECON_DAILY_REJECT", "RECONCILIATION", "SUCCESS", any(), 1L, "admin1", any()) }
    }
    test("reviewReconciliation rejects blank notes") {
      shouldThrow<BadRequestException> {
        resolver.reviewReconciliation(ReviewReconciliationInput(reconciliationId = 3L, reviewNotes = "  "), user)
      }
    }
    test("reviewReconciliation reviews and logs") {
      every { adminService.findByUsername("admin1") } returns admin()
      val model = DailyReconciliationModel(status = DailyReconciliationStatus.REVIEWED)
      every { dailyCollectionService.reviewReconciliation(3L, 1L, "无误") } returns model

      val result = resolver.reviewReconciliation(ReviewReconciliationInput(reconciliationId = 3L, reviewNotes = "无误"), user)

      result shouldBe model.toGraphQL()
      verify { bizLogger.logOperation("RECON_REVIEW", "RECONCILIATION", "SUCCESS", any(), 1L, "admin1", any()) }
    }
    test("executeDailyReconciliation runs for explicit date") {
      every { adminService.findByUsername("admin1") } returns admin()
      every { dailyCollectionService.getReconciliationByDate(any()) } returns
        DailyReconciliationModel(reconciliationDate = LocalDate.of(2025, 6, 1))

      val result = resolver.executeDailyReconciliation("2025-06-01", user)

      result.shouldNotBeNull()
      verify { dailyCollectionService.executeDailyReconciliation(LocalDate.of(2025, 6, 1)) }
    }
    test("executeDailyReconciliation defaults to yesterday") {
      every { adminService.findByUsername("admin1") } returns admin()
      every { dailyCollectionService.getReconciliationByDate(any()) } returns
        DailyReconciliationModel(reconciliationDate = LocalDate.now().minusDays(1))

      val result = resolver.executeDailyReconciliation(null, user)

      result.shouldNotBeNull()
      verify { dailyCollectionService.executeDailyReconciliation(LocalDate.now().minusDays(1)) }
    }
    test("executeDailyReconciliation throws when reconciliation missing") {
      every { adminService.findByUsername("admin1") } returns admin()
      every { dailyCollectionService.getReconciliationByDate(any()) } returns null

      shouldThrow<BadRequestException> { resolver.executeDailyReconciliation("2025-06-01", user) }
    }
    test("executeDailyReconciliation requires login") {
      shouldThrow<UnauthorizedException> { resolver.executeDailyReconciliation(null, null) }
    }
  })
