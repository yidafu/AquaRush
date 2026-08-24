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

import dev.yidafu.aqua.analytics.reconciliation.dto.DailyCollectionSummaryDTO
import dev.yidafu.aqua.analytics.reconciliation.dto.OrderStatsDTO
import dev.yidafu.aqua.analytics.reconciliation.dto.PaymentStatsDTO
import dev.yidafu.aqua.analytics.reconciliation.service.DailyCollectionService
import dev.yidafu.aqua.api.service.admin.AdminService
import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.common.domain.model.DailyCollectionRecordModel
import dev.yidafu.aqua.common.domain.model.DailyReconciliationModel
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import dev.yidafu.aqua.common.domain.model.enums.DailyCollectionStatus
import dev.yidafu.aqua.common.domain.model.enums.DailyReconciliationStatus
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.domain.model.enums.PaymentType
import dev.yidafu.aqua.common.exception.UnauthorizedException
import dev.yidafu.aqua.common.exception.UserNotFoundException
import dev.yidafu.aqua.common.graphql.generated.DailyCollectionListInput
import dev.yidafu.aqua.common.graphql.generated.DailyReconciliationListInput
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.data.domain.PageImpl
import org.springframework.security.core.userdetails.UserDetails
import java.time.LocalDate

/**
 * 管理员每日收款查询解析器单元测试: 登录校验、分页查询(日期/送水工筛选)、
 * 汇总卡、对账逐单明细、今日预计算(已有记录/无记录两条路径)
 */
class AdminDailyCollectionQueryResolverTest :
  FunSpec({
    var dailyCollectionService: DailyCollectionService = mockk()
    var adminService: AdminService = mockk()
    var resolver: AdminDailyCollectionQueryResolver = mockk()
    val user = mockk<UserDetails>(relaxed = true)

    fun admin(): AdminModel =
      AdminModel(username = "admin1", passwordHash = "hash", role = AdminRoleModel.ADMIN).apply {
        id = 1L
      }

    beforeTest {
      dailyCollectionService = mockk(relaxed = true)
      adminService = mockk(relaxed = true)
      resolver = AdminDailyCollectionQueryResolver(dailyCollectionService, adminService)
      every { user.username } returns "admin1"
    }
    test("dailyCollections requires login") {
      shouldThrow<UnauthorizedException> { resolver.dailyCollections(null, null) }
    }
    test("dailyCollections pages records without filters") {
      val records = listOf(DailyCollectionRecordModel(deliveryWorkerId = 1L))
      every { dailyCollectionService.getCollectionsPage(null, null, 0, 10) } returns PageImpl(records)

      val result = resolver.dailyCollections(DailyCollectionListInput(), user)

      result.list.size shouldBe 1
      result.pageInfo.total shouldBe 1
    }
    test("dailyCollections filters by date and worker") {
      val records = listOf(DailyCollectionRecordModel(deliveryWorkerId = 1L))
      every { dailyCollectionService.getCollectionsPage(LocalDate.of(2025, 6, 1), 1L, 0, 10) } returns PageImpl(records)

      val result =
        resolver.dailyCollections(
          DailyCollectionListInput(collectionDate = "2025-06-01", deliveryWorkerId = 1L),
          user,
        )

      result.list.size shouldBe 1
    }
    test("dailyReconciliations pages records sorted by service") {
      val newer = DailyReconciliationModel(reconciliationDate = LocalDate.of(2025, 6, 1), status = DailyReconciliationStatus.MATCHED)
      every { dailyCollectionService.getReconciliationsPage(0, 10) } returns PageImpl(listOf(newer))

      val result = resolver.dailyReconciliations(DailyReconciliationListInput(), user)

      result.list.size shouldBe 1
      result.list[0].reconciliationDate shouldBe "2025-06-01"
      result.pageInfo.total shouldBe 1
    }
    test("dailyReconciliations requires login") {
      shouldThrow<UnauthorizedException> { resolver.dailyReconciliations(null, null) }
    }
    test("dailyCollectionSummary defaults to today when date omitted") {
      every { dailyCollectionService.getDailyCollectionSummary(any()) } returns
        DailyCollectionSummaryDTO(
          collectionDate = LocalDate.now(),
          totalCashAmountCents = 1000L,
          totalQrCodeAmountCents = 500L,
          totalWaterTicketCount = 1,
          totalOrderCount = 3,
          totalOrderAmountCents = 1900L,
          totalSubmittedCount = 2,
        )

      val result = resolver.dailyCollectionSummary(null, user)

      result.totalCashAmountCents shouldBe 1000L
      result.totalSubmittedCount shouldBe 2
    }
    test("dailyReconciliationOrderDetails returns mapped order rows") {
      val order =
        OrderModel(
          orderNo = "ORD20260601001",
          amountCents = 2000L,
          status = OrderModelStatus.COMPLETED,
          paymentType = PaymentType.CASH,
        ).apply { id = 42L }
      every {
        dailyCollectionService.getReconciliationOrderDetails(LocalDate.of(2025, 6, 1), null, 0, 20)
      } returns PageImpl(listOf(order))

      val result =
        resolver.dailyReconciliationOrderDetails(
          reconciliationDate = "2025-06-01",
          deliveryWorkerId = null,
          page = 0,
          size = 20,
          userDetails = user,
        )

      result.list.size shouldBe 1
      result.list[0].orderId shouldBe 42L
      result.list[0].orderNo shouldBe "ORD20260601001"
      result.list[0].amountCents shouldBe 2000L
      result.list[0].status.name shouldBe "COMPLETED"
      result.list[0].paymentType?.name shouldBe "CASH"
    }
    test("myTodayCollection uses existing record when present") {
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

      result.shouldNotBeNull()
      result.orderCount shouldBe 3
      result.cashAmountCents shouldBe 600L
    }
    test("myTodayCollection falls back to payment stats when no record") {
      every { adminService.findByUsername("admin1") } returns admin()
      every { dailyCollectionService.getTodayOrderStats(1L, any()) } returns OrderStatsDTO(orderCount = 2, orderAmountCents = 400L)
      every { dailyCollectionService.getCollectionByWorkerAndDate(1L, any()) } returns null
      every { dailyCollectionService.getTodayPaymentStats(1L, any()) } returns
        PaymentStatsDTO(cashAmountCents = 300L, waterTicketCount = 1, qrCodeAmountCents = 100L)

      val result = resolver.myTodayCollection(user)

      result.shouldNotBeNull()
      result.orderCount shouldBe 2
      result.cashAmountCents shouldBe 300L
      result.differenceAmountCents.shouldBeNull()
      result.status.shouldBeNull()
    }
    test("myTodayCollection throws when worker missing") {
      every { adminService.findByUsername("admin1") } returns null

      shouldThrow<UserNotFoundException> { resolver.myTodayCollection(user) }
    }
  })
