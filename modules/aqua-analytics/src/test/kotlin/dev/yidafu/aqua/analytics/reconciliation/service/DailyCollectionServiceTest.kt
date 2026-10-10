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

package dev.yidafu.aqua.analytics.reconciliation.service

import dev.yidafu.aqua.api.service.order.OrderQueryService
import dev.yidafu.aqua.api.service.reconciliation.DailyCollectionMutationService
import dev.yidafu.aqua.api.service.reconciliation.DailyCollectionQueryApiService
import dev.yidafu.aqua.api.service.reconciliation.DailyReconciliationMutationService
import dev.yidafu.aqua.api.service.reconciliation.DailyReconciliationQueryApiService
import dev.yidafu.aqua.common.domain.model.DailyCollectionRecordModel
import dev.yidafu.aqua.common.domain.model.DailyReconciliationModel
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.enums.DailyCollectionStatus
import dev.yidafu.aqua.common.domain.model.enums.DailyReconciliationStatus
import dev.yidafu.aqua.common.domain.model.enums.PaymentType
import dev.yidafu.aqua.common.exception.BadRequestException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDate

/**
 * 每日收款/对账服务单元测试: 按支付类型统计、收款提交容差判定(差异口径)、
 * 确认/复核、对账汇总、分页查询、汇总卡、对账逐单明细
 */
class DailyCollectionServiceTest :
  FunSpec({
    var dailyCollectionQuery: DailyCollectionQueryApiService = mockk()
    var dailyReconciliationQuery: DailyReconciliationQueryApiService = mockk()
    var orderQueryService: OrderQueryService = mockk()
    var dailyCollectionMutation: DailyCollectionMutationService = mockk()
    var dailyReconciliationMutation: DailyReconciliationMutationService = mockk()
    var service: DailyCollectionService = mockk()
    val date = LocalDate.of(2025, 6, 1)

    fun stubPaymentStats(
      cash: Array<Long> = arrayOf(1L, 1000L),
      ticket: Array<Long> = arrayOf(1L, 0L),
      qr: Array<Long> = arrayOf(1L, 500L),
    ) {
      // 已送达口径(R-ORD-11):含 待确认收货 + 已完成,按送达时间计入
      every {
        orderQueryService.countAndSumDeliveredByPaymentType(1L, PaymentType.CASH, any(), any())
      } returns cash
      every {
        orderQueryService.countAndSumDeliveredByPaymentType(1L, PaymentType.WATER_TICKET, any(), any())
      } returns ticket
      every {
        orderQueryService.countAndSumDeliveredByPaymentType(1L, PaymentType.QR_CODE, any(), any())
      } returns qr
    }

    fun record(
      id: Long? = null,
      deliveryWorkerId: Long = 1L,
      collectionDate: LocalDate = date,
      cashAmountCents: Long = 0L,
      waterTicketCount: Int = 0,
      qrCodeAmountCents: Long = 0L,
      orderCount: Int = 0,
      orderAmountCents: Long = 0L,
      differenceAmountCents: Long = 0L,
      status: DailyCollectionStatus = DailyCollectionStatus.SUBMITTED,
      calculatedCashAmountCents: Long = 0L,
      calculatedQrCodeAmountCents: Long = 0L,
      calculatedWaterTicketCount: Int = 0,
    ): DailyCollectionRecordModel =
      DailyCollectionRecordModel(
        id = id,
        deliveryWorkerId = deliveryWorkerId,
        collectionDate = collectionDate,
        cashAmountCents = cashAmountCents,
        waterTicketCount = waterTicketCount,
        qrCodeAmountCents = qrCodeAmountCents,
        orderCount = orderCount,
        orderAmountCents = orderAmountCents,
        differenceAmountCents = differenceAmountCents,
        status = status,
        calculatedCashAmountCents = calculatedCashAmountCents,
        calculatedQrCodeAmountCents = calculatedQrCodeAmountCents,
        calculatedWaterTicketCount = calculatedWaterTicketCount,
      )

    beforeTest {
      dailyCollectionQuery = mockk(relaxed = true)
      dailyReconciliationQuery = mockk(relaxed = true)
      orderQueryService = mockk(relaxed = true)
      dailyCollectionMutation = mockk(relaxed = true)
      dailyReconciliationMutation = mockk(relaxed = true)
      service =
        DailyCollectionService(
          dailyCollectionQuery,
          dailyReconciliationQuery,
          orderQueryService,
          dailyCollectionMutation,
          dailyReconciliationMutation,
        )
      // relaxed mock 的泛型 save() 会返回裸 Object 导致 ClassCastException,
      // 统一让 save 返回入参
      every { dailyCollectionMutation.save(any()) } answers { firstArg() }
      every { dailyReconciliationMutation.save(any()) } answers { firstArg() }
    }
    test("getTodayPaymentStats aggregates cash ticket and qr") {
      stubPaymentStats(cash = arrayOf(3L, 3000L), ticket = arrayOf(2L, 0L), qr = arrayOf(5L, 700L))

      val stats = service.getTodayPaymentStats(1L, date)

      stats.cashAmountCents shouldBe 3000L
      stats.waterTicketCount shouldBe 2
      stats.qrCodeAmountCents shouldBe 700L
    }
    test("getTodayPaymentStats returns zero when no cash orders") {
      stubPaymentStats(cash = arrayOf(0L, 0L), ticket = arrayOf(0L, 0L), qr = arrayOf(0L, 0L))

      val stats = service.getTodayPaymentStats(1L, date)

      stats.cashAmountCents shouldBe 0L
      stats.waterTicketCount shouldBe 0
      stats.qrCodeAmountCents shouldBe 0L
    }
    test("getTodayOrderStats aggregates completed orders") {
      every { orderQueryService.countDeliveredByDateRange(any(), any(), any()) } returns 6L
      every { orderQueryService.sumDeliveredAmountCentsByDateRange(any(), any(), any()) } returns 1200L

      val stats = service.getTodayOrderStats(1L, date)

      stats.orderCount shouldBe 6
      stats.orderAmountCents shouldBe 1200L
    }
    test("submitDailyCollection creates record marked submitted within tolerance") {
      stubPaymentStats()
      every { orderQueryService.countDeliveredByDateRange(any(), any(), any()) } returns 3L
      every { orderQueryService.sumDeliveredAmountCentsByDateRange(any(), any(), any()) } returns 1400L
      every { dailyCollectionQuery.findByDeliveryWorkerIdAndCollectionDate(1L, date) } returns null
      every { dailyCollectionMutation.save(any()) } answers { firstArg() }

      // 填报 cash 1000 + qr 500 = 1500,系统计算 cash 1000 + qr 500 = 1500 → 差异 0 → SUBMITTED
      val record =
        service.submitDailyCollection(
          1L,
          cashAmountCents = 1000L,
          waterTicketCount = 1,
          qrCodeAmountCents = 500L,
          collectionDate = date,
        )

      record.status shouldBe DailyCollectionStatus.SUBMITTED
      record.orderCount shouldBe 3
      record.orderAmountCents shouldBe 1400L
      record.differenceAmountCents shouldBe 0L
    }
    test("submitDailyCollection marks disputed when difference exceeds tolerance") {
      // 填报 cash 1000 + qr 500 = 1500,系统计算 cash 500 + qr 200 = 700 → 差异 800 > 100 → DISPUTED(G28)
      stubPaymentStats(cash = arrayOf(1L, 500L), qr = arrayOf(1L, 200L))
      every { orderQueryService.countDeliveredByDateRange(any(), any(), any()) } returns 3L
      every { orderQueryService.sumDeliveredAmountCentsByDateRange(any(), any(), any()) } returns 800L
      every { dailyCollectionQuery.findByDeliveryWorkerIdAndCollectionDate(1L, date) } returns null
      every { dailyCollectionMutation.save(any()) } answers { firstArg() }

      val record =
        service.submitDailyCollection(
          1L,
          cashAmountCents = 1000L,
          waterTicketCount = 1,
          qrCodeAmountCents = 500L,
          collectionDate = date,
        )

      record.status shouldBe DailyCollectionStatus.DISPUTED
      record.differenceAmountCents shouldBe 800L
    }
    test("submitDailyCollection marks disputed when water ticket count mismatches") {
      // 金额一致但水票张数不符(R-COL-02 水票精确匹配)→ DISPUTED
      stubPaymentStats()
      every { orderQueryService.countDeliveredByDateRange(any(), any(), any()) } returns 3L
      every { orderQueryService.sumDeliveredAmountCentsByDateRange(any(), any(), any()) } returns 1400L
      every { dailyCollectionQuery.findByDeliveryWorkerIdAndCollectionDate(1L, date) } returns null
      every { dailyCollectionMutation.save(any()) } answers { firstArg() }

      val record =
        service.submitDailyCollection(
          1L,
          cashAmountCents = 1000L,
          waterTicketCount = 2, // 系统计算为 1 张
          qrCodeAmountCents = 500L,
          collectionDate = date,
        )

      record.status shouldBe DailyCollectionStatus.DISPUTED
      record.differenceAmountCents shouldBe 0L
    }
    test("submitDailyCollection excludes water ticket order amount from difference base") {
      // 口径回归:订单金额(1900,含水票订单 400)不参与差异基数;
      // 填报(现金+扫码) 与 系统计算(现金+扫码) 一致 → 差异 0,不被误判为「有争议」
      stubPaymentStats(cash = arrayOf(2L, 1000L), ticket = arrayOf(1L, 0L), qr = arrayOf(1L, 500L))
      every { orderQueryService.countDeliveredByDateRange(any(), any(), any()) } returns 4L
      every { orderQueryService.sumDeliveredAmountCentsByDateRange(any(), any(), any()) } returns 1900L
      every { dailyCollectionQuery.findByDeliveryWorkerIdAndCollectionDate(1L, date) } returns null
      every { dailyCollectionMutation.save(any()) } answers { firstArg() }

      val record =
        service.submitDailyCollection(
          1L,
          cashAmountCents = 1000L,
          waterTicketCount = 1,
          qrCodeAmountCents = 500L,
          collectionDate = date,
        )

      record.status shouldBe DailyCollectionStatus.SUBMITTED
      record.differenceAmountCents shouldBe 0L
      record.orderAmountCents shouldBe 1900L // 订单金额仍展示全量(含水票订单)
    }
    test("submitDailyCollection updates existing record and stores notes, clears reject trail") {
      stubPaymentStats()
      every { orderQueryService.countDeliveredByDateRange(any(), any(), any()) } returns 3L
      every { orderQueryService.sumDeliveredAmountCentsByDateRange(any(), any(), any()) } returns 1400L
      val existing =
        record(
          deliveryWorkerId = 1L,
          collectionDate = date,
          status = DailyCollectionStatus.DRAFT,
          differenceAmountCents = 500L,
        ).apply {
          rejectedAt = java.time.LocalDateTime.now()
          rejectedBy = 9L
          rejectReason = "金额不符"
        }
      every { dailyCollectionQuery.findByDeliveryWorkerIdAndCollectionDate(1L, date) } returns existing
      every { dailyCollectionMutation.save(any()) } answers { firstArg() }

      val updated =
        service.submitDailyCollection(
          1L,
          cashAmountCents = 1000L,
          waterTicketCount = 1,
          qrCodeAmountCents = 500L,
          collectionDate = date,
          notes = "重新提交",
        )

      updated.status shouldBe DailyCollectionStatus.SUBMITTED
      updated.differenceAmountCents shouldBe 0L
      updated.calculatedCashAmountCents shouldBe 1000L
      updated.notes shouldBe "重新提交"
      // 驳回退回草稿可改可重提,重新提交清空驳回留痕(G28)
      updated.rejectedAt.shouldBeNull()
      updated.rejectedBy.shouldBeNull()
      updated.rejectReason.shouldBeNull()
    }
    test("confirmCollection marks record confirmed") {
      val existing = record(deliveryWorkerId = 1L, collectionDate = date, status = DailyCollectionStatus.SUBMITTED)
      every { dailyCollectionQuery.findById(9L) } returns existing
      every { dailyCollectionMutation.save(any()) } answers { firstArg() }

      val confirmed = service.confirmCollection(9L, confirmedBy = 5L, notes = "无误")

      confirmed.status shouldBe DailyCollectionStatus.CONFIRMED
      confirmed.confirmedBy shouldBe 5L
      confirmed.notes shouldBe "无误"
      confirmed.confirmedAt.shouldNotBeNull()
    }
    test("confirmCollection throws when record missing") {
      every { dailyCollectionQuery.findById(9L) } returns null

      shouldThrow<IllegalArgumentException> { service.confirmCollection(9L, confirmedBy = 5L) }
    }
    test("rejectCollection rejects submitted record back to draft with trail") {
      val existing = record(deliveryWorkerId = 1L, collectionDate = date, status = DailyCollectionStatus.SUBMITTED)
      every { dailyCollectionQuery.findById(9L) } returns existing
      every { dailyCollectionMutation.save(any()) } answers { firstArg() }

      val rejected = service.rejectCollection(9L, rejectedBy = 5L, reason = "金额不符")

      rejected.status shouldBe DailyCollectionStatus.DRAFT
      rejected.rejectedBy shouldBe 5L
      rejected.rejectReason shouldBe "金额不符"
      rejected.rejectedAt.shouldNotBeNull()
    }
    test("rejectCollection rejects disputed record back to draft with trail") {
      val existing = record(deliveryWorkerId = 1L, collectionDate = date, status = DailyCollectionStatus.DISPUTED)
      every { dailyCollectionQuery.findById(9L) } returns existing
      every { dailyCollectionMutation.save(any()) } answers { firstArg() }

      val rejected = service.rejectCollection(9L, rejectedBy = 5L, reason = "扫码金额对不上")

      rejected.status shouldBe DailyCollectionStatus.DRAFT
      rejected.rejectedBy shouldBe 5L
      rejected.rejectReason shouldBe "扫码金额对不上"
    }
    test("rejectCollection throws when record missing") {
      every { dailyCollectionQuery.findById(9L) } returns null

      shouldThrow<BadRequestException> { service.rejectCollection(9L, rejectedBy = 5L, reason = "金额不符") }
    }
    test("rejectCollection throws when reason blank") {
      val existing = record(deliveryWorkerId = 1L, collectionDate = date, status = DailyCollectionStatus.SUBMITTED)
      every { dailyCollectionQuery.findById(9L) } returns existing

      shouldThrow<BadRequestException> { service.rejectCollection(9L, rejectedBy = 5L, reason = "  ") }
    }
    test("rejectCollection throws for confirmed or draft records") {
      val confirmed = record(deliveryWorkerId = 1L, collectionDate = date, status = DailyCollectionStatus.CONFIRMED)
      val draft = record(deliveryWorkerId = 1L, collectionDate = date, status = DailyCollectionStatus.DRAFT)
      every { dailyCollectionQuery.findById(9L) } returns confirmed
      every { dailyCollectionQuery.findById(10L) } returns draft

      shouldThrow<BadRequestException> { service.rejectCollection(9L, rejectedBy = 5L, reason = "金额不符") }
      shouldThrow<BadRequestException> { service.rejectCollection(10L, rejectedBy = 5L, reason = "金额不符") }
    }
    test("getCollectionByWorkerAndDate delegates to repository") {
      val expected = record(deliveryWorkerId = 1L, collectionDate = date)
      every { dailyCollectionQuery.findByDeliveryWorkerIdAndCollectionDate(1L, date) } returns expected

      service.getCollectionByWorkerAndDate(1L, date) shouldBe expected
    }
    test("getCollectionsByDate delegates and guards empty") {
      every { dailyCollectionQuery.findByCollectionDate(date) } returns emptyList()

      service.getCollectionsByDate(1L, date) shouldBe emptyList()
    }
    test("getCollectionsByDateRange delegates") {
      val start = LocalDate.of(2025, 6, 1)
      val end = LocalDate.of(2025, 6, 30)
      val expected = listOf(record())
      every { dailyCollectionQuery.findByCollectionDateBetween(start, end) } returns expected

      service.getCollectionsByDateRange(start, end) shouldBe expected
    }
    test("getCollectionsPage with date and worker filters") {
      val expected: Page<DailyCollectionRecordModel> = PageImpl(listOf(record()))
      every { dailyCollectionQuery.findByDeliveryWorkerIdAndCollectionDate(1L, date, any()) } returns expected

      val page = service.getCollectionsPage(collectionDate = date, deliveryWorkerId = 1L, page = 0, size = 10)

      page shouldBe expected
      verify { dailyCollectionQuery.findByDeliveryWorkerIdAndCollectionDate(1L, date, any()) }
    }
    test("getCollectionsPage with date only filters") {
      val expected: Page<DailyCollectionRecordModel> = PageImpl(listOf(record()))
      every { dailyCollectionQuery.findByCollectionDate(date, any()) } returns expected

      val page = service.getCollectionsPage(collectionDate = date, deliveryWorkerId = null, page = 0, size = 10)

      page shouldBe expected
      verify { dailyCollectionQuery.findByCollectionDate(date, any()) }
    }
    test("getCollectionsPage with worker only filters") {
      val expected: Page<DailyCollectionRecordModel> = PageImpl(listOf(record()))
      every { dailyCollectionQuery.findByDeliveryWorkerId(1L, any()) } returns expected

      val page = service.getCollectionsPage(collectionDate = null, deliveryWorkerId = 1L, page = 0, size = 10)

      page shouldBe expected
      verify { dailyCollectionQuery.findByDeliveryWorkerId(1L, any()) }
    }
    test("getCollectionsPage without filters pages all records") {
      val expected: Page<DailyCollectionRecordModel> = PageImpl(listOf(record()))
      every { dailyCollectionQuery.findAll(any<PageRequest>()) } returns expected

      val page = service.getCollectionsPage(collectionDate = null, deliveryWorkerId = null, page = 0, size = 10)

      page shouldBe expected
      verify { dailyCollectionQuery.findAll(any<PageRequest>()) }
    }
    test("getReconciliationByDate delegates") {
      val expected = DailyReconciliationModel()
      every { dailyReconciliationQuery.findByReconciliationDate(date) } returns expected

      every { dailyReconciliationQuery.findByReconciliationDate(LocalDate.of(2020, 1, 1)) } returns null

      service.getReconciliationByDate(date) shouldBe expected
      service.getReconciliationByDate(LocalDate.of(2020, 1, 1)).shouldBeNull()
    }
    test("getReconciliationsPage pages all records") {
      val expected: Page<DailyReconciliationModel> = PageImpl(listOf(DailyReconciliationModel()))
      every { dailyReconciliationQuery.findAll(any<PageRequest>()) } returns expected

      val page = service.getReconciliationsPage(0, 10)

      page shouldBe expected
    }
    test("getDailyCollectionSummary aggregates records excluding drafts from submitted count") {
      val records =
        listOf(
          record(
            deliveryWorkerId = 1L,
            cashAmountCents = 1000L,
            waterTicketCount = 1,
            qrCodeAmountCents = 500L,
            orderCount = 3,
            orderAmountCents = 1900L,
          ),
          record(
            deliveryWorkerId = 2L,
            cashAmountCents = 200L,
            waterTicketCount = 0,
            qrCodeAmountCents = 0L,
            orderCount = 1,
            orderAmountCents = 200L,
            status = DailyCollectionStatus.DRAFT,
          ),
          record(
            deliveryWorkerId = 3L,
            cashAmountCents = 0L,
            waterTicketCount = 2,
            qrCodeAmountCents = 0L,
            orderCount = 2,
            orderAmountCents = 400L,
          ),
        )
      every { dailyCollectionQuery.findByCollectionDate(date) } returns records

      val summary = service.getDailyCollectionSummary(date)

      summary.totalCashAmountCents shouldBe 1200L
      summary.totalQrCodeAmountCents shouldBe 500L
      summary.totalWaterTicketCount shouldBe 3
      summary.totalOrderCount shouldBe 6
      summary.totalOrderAmountCents shouldBe 2500L
      summary.totalSubmittedCount shouldBe 2
    }
    test("getReconciliationOrderDetails delegates to order repository") {
      val expected: Page<OrderModel> = PageImpl(listOf(OrderModel(orderNo = "ORD1")))
      every { orderQueryService.findDeliveredOrdersByDateRange(any(), any(), any(), any()) } returns expected

      val page = service.getReconciliationOrderDetails(reconciliationDate = date, deliveryWorkerId = null, page = 0, size = 20)

      page shouldBe expected
      verify { orderQueryService.findDeliveredOrdersByDateRange(null, date.atStartOfDay(), date.atTime(java.time.LocalTime.MAX), any()) }
    }
    test("executeDailyReconciliation creates matched reconciliation") {
      val collections =
        listOf(
          record(
            deliveryWorkerId = 1L,
            collectionDate = date,
            cashAmountCents = 1000L,
            qrCodeAmountCents = 500L,
            orderAmountCents = 1400L,
            calculatedCashAmountCents = 1000L,
            calculatedQrCodeAmountCents = 500L,
          ),
          record(
            deliveryWorkerId = 2L,
            collectionDate = date,
            cashAmountCents = 200L,
            qrCodeAmountCents = 0L,
            orderAmountCents = 300L,
            calculatedCashAmountCents = 200L,
            calculatedQrCodeAmountCents = 0L,
          ),
        )
      every { dailyCollectionQuery.findByCollectionDate(date) } returns collections
      every { dailyReconciliationQuery.findByReconciliationDate(date) } returns null
      every { dailyReconciliationMutation.save(any()) } answers { firstArg() }

      service.executeDailyReconciliation(date)

      verify { dailyReconciliationMutation.save(match { it.status == DailyReconciliationStatus.MATCHED }) }
    }
    test("executeDailyReconciliation marks discrepancy") {
      // 填报 cash 1000 + qr 500 = 1500,系统计算 cash 1000 + qr 200 = 1200 → 差异 300 > 100 → DISCREPANCY
      val collections =
        listOf(
          record(
            deliveryWorkerId = 1L,
            collectionDate = date,
            cashAmountCents = 1000L,
            qrCodeAmountCents = 500L,
            orderAmountCents = 500L,
            calculatedCashAmountCents = 1000L,
            calculatedQrCodeAmountCents = 200L,
          ),
        )
      every { dailyCollectionQuery.findByCollectionDate(date) } returns collections
      every { dailyReconciliationQuery.findByReconciliationDate(date) } returns null
      every { dailyReconciliationMutation.save(any()) } answers { firstArg() }

      service.executeDailyReconciliation(date)

      verify { dailyReconciliationMutation.save(match { it.status == DailyReconciliationStatus.DISCREPANCY }) }
    }
    test("executeDailyReconciliation marks discrepancy on water ticket mismatch") {
      val collections =
        listOf(
          record(
            deliveryWorkerId = 1L,
            collectionDate = date,
            cashAmountCents = 1000L,
            waterTicketCount = 3,
            qrCodeAmountCents = 500L,
            orderAmountCents = 1400L,
            calculatedCashAmountCents = 1000L,
            calculatedQrCodeAmountCents = 500L,
            calculatedWaterTicketCount = 1,
          ),
        )
      every { dailyCollectionQuery.findByCollectionDate(date) } returns collections
      every { dailyReconciliationQuery.findByReconciliationDate(date) } returns null
      every { dailyReconciliationMutation.save(any()) } answers { firstArg() }

      service.executeDailyReconciliation(date)

      verify { dailyReconciliationMutation.save(match { it.status == DailyReconciliationStatus.DISCREPANCY }) }
    }
    test("executeDailyReconciliation excludes water ticket order amount from discrepancy base") {
      // 口径回归:订单金额含水票订单价值(400),但差异基数只比 填报(现金+扫码) vs 计算(现金+扫码)
      val collections =
        listOf(
          record(
            deliveryWorkerId = 1L,
            collectionDate = date,
            cashAmountCents = 1000L,
            waterTicketCount = 1,
            qrCodeAmountCents = 500L,
            orderAmountCents = 1900L,
            calculatedCashAmountCents = 1000L,
            calculatedQrCodeAmountCents = 500L,
            calculatedWaterTicketCount = 1,
          ),
        )
      every { dailyCollectionQuery.findByCollectionDate(date) } returns collections
      every { dailyReconciliationQuery.findByReconciliationDate(date) } returns null
      every { dailyReconciliationMutation.save(any()) } answers { firstArg() }

      service.executeDailyReconciliation(date)

      verify {
        dailyReconciliationMutation.save(
          match {
            it.status == DailyReconciliationStatus.MATCHED &&
              it.discrepancyAmountCents == 0L &&
              it.totalOrderAmountCents == 1900L
          },
        )
      }
    }
    test("executeDailyReconciliation updates existing reconciliation and clears review trail") {
      val collections =
        listOf(
          record(
            deliveryWorkerId = 1L,
            collectionDate = date,
            cashAmountCents = 1000L,
            qrCodeAmountCents = 500L,
            orderAmountCents = 1400L,
            calculatedCashAmountCents = 1000L,
            calculatedQrCodeAmountCents = 500L,
          ),
        )
      val existing =
        DailyReconciliationModel(status = DailyReconciliationStatus.REVIEWED).apply {
          reviewedAt = java.time.LocalDateTime.now()
          reviewedBy = 7L
          reviewNotes = "OK"
        }
      every { dailyCollectionQuery.findByCollectionDate(date) } returns collections
      every { dailyReconciliationQuery.findByReconciliationDate(date) } returns existing
      every { dailyReconciliationMutation.save(any()) } answers { firstArg() }

      service.executeDailyReconciliation(date)

      existing.status shouldBe DailyReconciliationStatus.MATCHED
      existing.totalCollectionAmountCents shouldBe 1500L
      existing.totalOrderAmountCents shouldBe 1400L
      existing.discrepancyAmountCents shouldBe 0L
      // 重新执行对账会重算结果,清除复核痕迹,避免状态与复核字段不一致
      existing.reviewedAt.shouldBeNull()
      existing.reviewedBy.shouldBeNull()
      existing.reviewNotes.shouldBeNull()
    }
    test("reviewReconciliation marks reviewed") {
      val existing = DailyReconciliationModel(status = DailyReconciliationStatus.MATCHED)
      every { dailyReconciliationQuery.findById(3L) } returns existing
      every { dailyReconciliationMutation.save(any()) } answers { firstArg() }

      val reviewed = service.reviewReconciliation(3L, reviewedBy = 7L, reviewNotes = "OK")

      reviewed.status shouldBe DailyReconciliationStatus.REVIEWED
      reviewed.reviewedBy shouldBe 7L
      reviewed.reviewNotes shouldBe "OK"
      reviewed.reviewedAt.shouldNotBeNull()
    }
    test("reviewReconciliation throws when missing") {
      every { dailyReconciliationQuery.findById(3L) } returns null

      shouldThrow<IllegalArgumentException> { service.reviewReconciliation(3L, reviewedBy = 7L, reviewNotes = "OK") }
    }
  })
