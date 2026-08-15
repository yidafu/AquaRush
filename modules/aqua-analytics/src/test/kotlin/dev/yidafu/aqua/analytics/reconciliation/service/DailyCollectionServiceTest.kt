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
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * 每日收款/对账服务单元测试: 按支付类型统计、收款提交容差判定、确认/复核、对账汇总
 */
class DailyCollectionServiceTest {
  private lateinit var collectionRecordRepository: DailyCollectionRecordRepository
  private lateinit var reconciliationRepository: DailyReconciliationRepository
  private lateinit var orderQueryService: OrderQueryService
  private lateinit var orderRepository: OrderRepository
  private lateinit var service: DailyCollectionService

  private val date = LocalDate.of(2025, 6, 1)

  @BeforeEach
  fun setUp() {
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

  private fun stubPaymentStats(
    cash: Array<Long> = arrayOf(1L, 1000L),
    ticket: Array<Long> = arrayOf(1L, 0L),
    qr: Array<Long> = arrayOf(1L, 500L),
  ) {
    every {
      orderRepository.countAndSumByPaymentType(1L, any(), PaymentType.CASH, any(), any())
    } returns cash
    every {
      orderRepository.countAndSumByPaymentType(1L, any(), PaymentType.WATER_TICKET, any(), any())
    } returns ticket
    every {
      orderRepository.countAndSumByPaymentType(1L, any(), PaymentType.QR_CODE, any(), any())
    } returns qr
  }

  @Test
  fun `getTodayPaymentStats aggregates cash ticket and qr`() {
    stubPaymentStats(cash = arrayOf(3L, 3000L), ticket = arrayOf(2L, 0L), qr = arrayOf(5L, 700L))

    val stats = service.getTodayPaymentStats(1L, date)

    assertEquals(3000L, stats.cashAmountCents)
    assertEquals(2, stats.waterTicketCount)
    assertEquals(700L, stats.qrCodeAmountCents)
  }

  @Test
  fun `getTodayPaymentStats returns zero when no cash orders`() {
    stubPaymentStats(cash = arrayOf(0L, 0L), ticket = arrayOf(0L, 0L), qr = arrayOf(0L, 0L))

    val stats = service.getTodayPaymentStats(1L, date)

    assertEquals(0L, stats.cashAmountCents)
    assertEquals(0, stats.waterTicketCount)
    assertEquals(0L, stats.qrCodeAmountCents)
  }

  @Test
  fun `getTodayOrderStats aggregates completed orders`() {
    every { orderQueryService.countOrdersByDateRange(any(), any(), any(), any()) } returns 6L
    every { orderQueryService.sumAmountCentsByStatusAndDateRange(any(), any(), any(), any()) } returns 1200L

    val stats = service.getTodayOrderStats(1L, date)

    assertEquals(6, stats.orderCount)
    assertEquals(1200L, stats.orderAmountCents)
  }

  @Test
  fun `submitDailyCollection creates record marked submitted within tolerance`() {
    stubPaymentStats()
    every { orderQueryService.countOrdersByDateRange(any(), any(), any(), any()) } returns 3L
    every { orderQueryService.sumAmountCentsByStatusAndDateRange(any(), any(), any(), any()) } returns 1400L
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

    assertEquals(DailyCollectionStatus.SUBMITTED, record.status)
    assertEquals(3, record.orderCount)
    assertEquals(1400L, record.orderAmountCents)
    assertEquals(100L, record.differenceAmountCents)
  }

  @Test
  fun `submitDailyCollection marks draft when difference exceeds tolerance`() {
    stubPaymentStats()
    every { orderQueryService.countOrdersByDateRange(any(), any(), any(), any()) } returns 3L
    every { orderQueryService.sumAmountCentsByStatusAndDateRange(any(), any(), any(), any()) } returns 800L
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

    assertEquals(DailyCollectionStatus.DRAFT, record.status)
    assertEquals(700L, record.differenceAmountCents)
  }

  @Test
  fun `submitDailyCollection updates existing record`() {
    stubPaymentStats()
    every { orderQueryService.countOrdersByDateRange(any(), any(), any(), any()) } returns 3L
    every { orderQueryService.sumAmountCentsByStatusAndDateRange(any(), any(), any(), any()) } returns 1400L
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

    assertEquals(DailyCollectionStatus.SUBMITTED, updated.status)
    assertEquals(1400L, updated.orderAmountCents)
    assertEquals(1000L, updated.calculatedCashAmountCents)
  }

  @Test
  fun `confirmCollection marks record confirmed`() {
    val existing = record(deliveryWorkerId = 1L, collectionDate = date, status = DailyCollectionStatus.SUBMITTED)
    every { collectionRecordRepository.findById(9L) } returns Optional.of(existing)
    every { collectionRecordRepository.save(any()) } answers { firstArg() }

    val confirmed = service.confirmCollection(9L, confirmedBy = 5L, notes = "无误")

    assertEquals(DailyCollectionStatus.CONFIRMED, confirmed.status)
    assertEquals(5L, confirmed.confirmedBy)
    assertEquals("无误", confirmed.notes)
    assertNotNull(confirmed.confirmedAt)
  }

  @Test
  fun `confirmCollection throws when record missing`() {
    every { collectionRecordRepository.findById(9L) } returns Optional.empty()

    assertFailsWith<IllegalArgumentException> { service.confirmCollection(9L, confirmedBy = 5L) }
  }

  @Test
  fun `getCollectionByWorkerAndDate delegates to repository`() {
    val expected = record(deliveryWorkerId = 1L, collectionDate = date)
    every { collectionRecordRepository.findByDeliveryWorkerIdAndCollectionDate(1L, date) } returns expected

    assertEquals(expected, service.getCollectionByWorkerAndDate(1L, date))
  }

  @Test
  fun `getCollectionsByDate delegates and guards empty`() {
    every { collectionRecordRepository.findByCollectionDate(date) } returns emptyList()

    assertEquals(emptyList(), service.getCollectionsByDate(1L, date))
  }

  @Test
  fun `getAllCollections delegates and guards empty`() {
    every { collectionRecordRepository.findAll() } returns emptyList()

    assertEquals(emptyList(), service.getAllCollections())
  }

  @Test
  fun `getCollectionsByDateRange delegates`() {
    val start = LocalDate.of(2025, 6, 1)
    val end = LocalDate.of(2025, 6, 30)
    val expected = listOf(record())
    every { collectionRecordRepository.findByCollectionDateBetween(start, end) } returns expected

    assertEquals(expected, service.getCollectionsByDateRange(start, end))
  }

  @Test
  fun `getReconciliationByDate delegates`() {
    val expected = DailyReconciliationModel()
    every { reconciliationRepository.findByReconciliationDate(date) } returns expected

    every { reconciliationRepository.findByReconciliationDate(LocalDate.of(2020, 1, 1)) } returns null

    assertEquals(expected, service.getReconciliationByDate(date))
    assertNull(service.getReconciliationByDate(LocalDate.of(2020, 1, 1)))
  }

  @Test
  fun `getAllReconciliations delegates and guards empty`() {
    every { reconciliationRepository.findAll() } returns emptyList()

    assertEquals(emptyList(), service.getAllReconciliations())
  }

  @Test
  fun `executeDailyReconciliation creates matched reconciliation`() {
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

  @Test
  fun `executeDailyReconciliation marks discrepancy`() {
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

  @Test
  fun `executeDailyReconciliation updates existing reconciliation`() {
    val collections =
      listOf(
        record(deliveryWorkerId = 1L, collectionDate = date, cashAmountCents = 1000L, qrCodeAmountCents = 500L, orderAmountCents = 1400L),
      )
    val existing = DailyReconciliationModel()
    every { collectionRecordRepository.findByCollectionDate(date) } returns collections
    every { reconciliationRepository.findByReconciliationDate(date) } returns existing
    every { reconciliationRepository.save(any()) } answers { firstArg() }

    service.executeDailyReconciliation(date)

    assertEquals(DailyReconciliationStatus.MATCHED, existing.status)
    assertEquals(1500L, existing.totalCollectionAmountCents)
    assertEquals(1400L, existing.totalOrderAmountCents)
    assertEquals(100L, existing.discrepancyAmountCents)
  }

  @Test
  fun `reviewReconciliation marks reviewed`() {
    val existing = DailyReconciliationModel(status = DailyReconciliationStatus.MATCHED)
    every { reconciliationRepository.findById(3L) } returns Optional.of(existing)
    every { reconciliationRepository.save(any()) } answers { firstArg() }

    val reviewed = service.reviewReconciliation(3L, reviewedBy = 7L, reviewNotes = "OK")

    assertEquals(DailyReconciliationStatus.REVIEWED, reviewed.status)
    assertEquals(7L, reviewed.reviewedBy)
    assertEquals("OK", reviewed.reviewNotes)
    assertNotNull(reviewed.reviewedAt)
  }

  @Test
  fun `reviewReconciliation throws when missing`() {
    every { reconciliationRepository.findById(3L) } returns Optional.empty()

    assertFailsWith<IllegalArgumentException> { service.reviewReconciliation(3L, reviewedBy = 7L, reviewNotes = "OK") }
  }

  private fun record(
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
}
