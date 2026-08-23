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
import dev.yidafu.aqua.common.domain.model.DailyCollectionRecordModel
import dev.yidafu.aqua.common.domain.model.DailyReconciliationModel
import dev.yidafu.aqua.common.domain.model.enums.DailyCollectionStatus
import dev.yidafu.aqua.common.domain.model.enums.DailyReconciliationStatus
import dev.yidafu.aqua.common.domain.model.enums.PaymentType
import dev.yidafu.aqua.delivery.domain.repository.DailyCollectionRecordRepository
import dev.yidafu.aqua.delivery.domain.repository.DailyReconciliationRepository
import dev.yidafu.aqua.order.domain.repository.OrderRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.util.Optional

/**
 * 每日收款/对账服务单元测试: 按支付类型统计、收款提交容差判定、确认/复核、对账汇总
 */
class DailyCollectionServiceTest :
  FunSpec({
    var collectionRecordRepository: DailyCollectionRecordRepository = mockk()
    var reconciliationRepository: DailyReconciliationRepository = mockk()
    var orderQueryService: OrderQueryService = mockk()
    var orderRepository: OrderRepository = mockk()
    var service: DailyCollectionService = mockk()
    val date = LocalDate.of(2025, 6, 1)

    fun stubPaymentStats(
      cash: Array<Long> = arrayOf(1L, 1000L),
      ticket: Array<Long> = arrayOf(1L, 0L),
      qr: Array<Long> = arrayOf(1L, 500L),
    ) {
      // 已送达口径(R-ORD-11):含 待确认收货 + 已完成,按送达时间计入
      every {
        orderRepository.countAndSumDeliveredByPaymentType(1L, PaymentType.CASH, any(), any())
      } returns cash
      every {
        orderRepository.countAndSumDeliveredByPaymentType(1L, PaymentType.WATER_TICKET, any(), any())
      } returns ticket
      every {
        orderRepository.countAndSumDeliveredByPaymentType(1L, PaymentType.QR_CODE, any(), any())
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
      )

    beforeTest {
      collectionRecordRepository = mockk(relaxed = true)
      reconciliationRepository = mockk(relaxed = true)
      orderQueryService = mockk(relaxed = true)
      orderRepository = mockk(relaxed = true)
      service = DailyCollectionService(collectionRecordRepository, reconciliationRepository, orderQueryService, orderRepository)
      // relaxed mock 的泛型 save() 会返回裸 Object 导致 ClassCastException,
      // 统一让 save 返回入参
      every { collectionRecordRepository.save(any()) } answers { firstArg() }
      every { reconciliationRepository.save(any()) } answers { firstArg() }
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
      every { orderRepository.countDeliveredByDateRange(any(), any(), any()) } returns 6L
      every { orderRepository.sumDeliveredAmountCentsByDateRange(any(), any(), any()) } returns 1200L

      val stats = service.getTodayOrderStats(1L, date)

      stats.orderCount shouldBe 6
      stats.orderAmountCents shouldBe 1200L
    }
    test("submitDailyCollection creates record marked submitted within tolerance") {
      stubPaymentStats()
      every { orderRepository.countDeliveredByDateRange(any(), any(), any()) } returns 3L
      every { orderRepository.sumDeliveredAmountCentsByDateRange(any(), any(), any()) } returns 1400L
      every { collectionRecordRepository.findByDeliveryWorkerIdAndCollectionDate(1L, date) } returns null
      every { collectionRecordRepository.save(any()) } answers { firstArg() }

      // cash 1000 + qr 500 - order 1400 = 100, 在容差(100)内 → SUBMITTED
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
      record.differenceAmountCents shouldBe 100L
    }
    test("submitDailyCollection marks draft when difference exceeds tolerance") {
      stubPaymentStats()
      every { orderRepository.countDeliveredByDateRange(any(), any(), any()) } returns 3L
      every { orderRepository.sumDeliveredAmountCentsByDateRange(any(), any(), any()) } returns 800L
      every { collectionRecordRepository.findByDeliveryWorkerIdAndCollectionDate(1L, date) } returns null
      every { collectionRecordRepository.save(any()) } answers { firstArg() }

      val record =
        service.submitDailyCollection(
          1L,
          cashAmountCents = 1000L,
          waterTicketCount = 1,
          qrCodeAmountCents = 500L,
          collectionDate = date,
        )

      record.status shouldBe DailyCollectionStatus.DRAFT
      record.differenceAmountCents shouldBe 700L
    }
    test("submitDailyCollection updates existing record") {
      stubPaymentStats()
      every { orderRepository.countDeliveredByDateRange(any(), any(), any()) } returns 3L
      every { orderRepository.sumDeliveredAmountCentsByDateRange(any(), any(), any()) } returns 1400L
      val existing = record(deliveryWorkerId = 1L, collectionDate = date, status = DailyCollectionStatus.DRAFT)
      every { collectionRecordRepository.findByDeliveryWorkerIdAndCollectionDate(1L, date) } returns existing
      every { collectionRecordRepository.save(any()) } answers { firstArg() }

      val updated =
        service.submitDailyCollection(
          1L,
          cashAmountCents = 1000L,
          waterTicketCount = 1,
          qrCodeAmountCents = 500L,
          collectionDate = date,
        )

      updated.status shouldBe DailyCollectionStatus.SUBMITTED
      updated.orderAmountCents shouldBe 1400L
      updated.calculatedCashAmountCents shouldBe 1000L
    }
    test("confirmCollection marks record confirmed") {
      val existing = record(deliveryWorkerId = 1L, collectionDate = date, status = DailyCollectionStatus.SUBMITTED)
      every { collectionRecordRepository.findById(9L) } returns Optional.of(existing)
      every { collectionRecordRepository.save(any()) } answers { firstArg() }

      val confirmed = service.confirmCollection(9L, confirmedBy = 5L, notes = "无误")

      confirmed.status shouldBe DailyCollectionStatus.CONFIRMED
      confirmed.confirmedBy shouldBe 5L
      confirmed.notes shouldBe "无误"
      confirmed.confirmedAt.shouldNotBeNull()
    }
    test("confirmCollection throws when record missing") {
      every { collectionRecordRepository.findById(9L) } returns Optional.empty()

      shouldThrow<IllegalArgumentException> { service.confirmCollection(9L, confirmedBy = 5L) }
    }
    test("getCollectionByWorkerAndDate delegates to repository") {
      val expected = record(deliveryWorkerId = 1L, collectionDate = date)
      every { collectionRecordRepository.findByDeliveryWorkerIdAndCollectionDate(1L, date) } returns expected

      service.getCollectionByWorkerAndDate(1L, date) shouldBe expected
    }
    test("getCollectionsByDate delegates and guards empty") {
      every { collectionRecordRepository.findByCollectionDate(date) } returns emptyList()

      service.getCollectionsByDate(1L, date) shouldBe emptyList()
    }
    test("getAllCollections delegates and guards empty") {
      every { collectionRecordRepository.findAll() } returns emptyList()

      service.getAllCollections() shouldBe emptyList()
    }
    test("getCollectionsByDateRange delegates") {
      val start = LocalDate.of(2025, 6, 1)
      val end = LocalDate.of(2025, 6, 30)
      val expected = listOf(record())
      every { collectionRecordRepository.findByCollectionDateBetween(start, end) } returns expected

      service.getCollectionsByDateRange(start, end) shouldBe expected
    }
    test("getReconciliationByDate delegates") {
      val expected = DailyReconciliationModel()
      every { reconciliationRepository.findByReconciliationDate(date) } returns expected

      every { reconciliationRepository.findByReconciliationDate(LocalDate.of(2020, 1, 1)) } returns null

      service.getReconciliationByDate(date) shouldBe expected
      service.getReconciliationByDate(LocalDate.of(2020, 1, 1)).shouldBeNull()
    }
    test("getAllReconciliations delegates and guards empty") {
      every { reconciliationRepository.findAll() } returns emptyList()

      service.getAllReconciliations() shouldBe emptyList()
    }
    test("executeDailyReconciliation creates matched reconciliation") {
      val collections =
        listOf(
          record(deliveryWorkerId = 1L, collectionDate = date, cashAmountCents = 1000L, qrCodeAmountCents = 500L, orderAmountCents = 1400L),
          record(deliveryWorkerId = 2L, collectionDate = date, cashAmountCents = 200L, qrCodeAmountCents = 0L, orderAmountCents = 300L),
        )
      every { collectionRecordRepository.findByCollectionDate(date) } returns collections
      every { reconciliationRepository.findByReconciliationDate(date) } returns null
      every { reconciliationRepository.save(any()) } answers { firstArg() }

      service.executeDailyReconciliation(date)

      verify { reconciliationRepository.save(match { it.status == DailyReconciliationStatus.MATCHED }) }
    }
    test("executeDailyReconciliation marks discrepancy") {
      val collections =
        listOf(
          record(deliveryWorkerId = 1L, collectionDate = date, cashAmountCents = 1000L, qrCodeAmountCents = 500L, orderAmountCents = 500L),
        )
      every { collectionRecordRepository.findByCollectionDate(date) } returns collections
      every { reconciliationRepository.findByReconciliationDate(date) } returns null
      every { reconciliationRepository.save(any()) } answers { firstArg() }

      service.executeDailyReconciliation(date)

      verify { reconciliationRepository.save(match { it.status == DailyReconciliationStatus.DISCREPANCY }) }
    }
    test("executeDailyReconciliation updates existing reconciliation") {
      val collections =
        listOf(
          record(deliveryWorkerId = 1L, collectionDate = date, cashAmountCents = 1000L, qrCodeAmountCents = 500L, orderAmountCents = 1400L),
        )
      val existing = DailyReconciliationModel()
      every { collectionRecordRepository.findByCollectionDate(date) } returns collections
      every { reconciliationRepository.findByReconciliationDate(date) } returns existing
      every { reconciliationRepository.save(any()) } answers { firstArg() }

      service.executeDailyReconciliation(date)

      existing.status shouldBe DailyReconciliationStatus.MATCHED
      existing.totalCollectionAmountCents shouldBe 1500L
      existing.totalOrderAmountCents shouldBe 1400L
      existing.discrepancyAmountCents shouldBe 100L
    }
    test("reviewReconciliation marks reviewed") {
      val existing = DailyReconciliationModel(status = DailyReconciliationStatus.MATCHED)
      every { reconciliationRepository.findById(3L) } returns Optional.of(existing)
      every { reconciliationRepository.save(any()) } answers { firstArg() }

      val reviewed = service.reviewReconciliation(3L, reviewedBy = 7L, reviewNotes = "OK")

      reviewed.status shouldBe DailyReconciliationStatus.REVIEWED
      reviewed.reviewedBy shouldBe 7L
      reviewed.reviewNotes shouldBe "OK"
      reviewed.reviewedAt.shouldNotBeNull()
    }
    test("reviewReconciliation throws when missing") {
      every { reconciliationRepository.findById(3L) } returns Optional.empty()

      shouldThrow<IllegalArgumentException> { service.reviewReconciliation(3L, reviewedBy = 7L, reviewNotes = "OK") }
    }
  })
