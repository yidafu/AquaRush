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

package dev.yidafu.aqua.analytics.reconciliation.service.impl

import dev.yidafu.aqua.analytics.reconciliation.domain.repository.ReconciliationDiscrepancyRepository
import dev.yidafu.aqua.analytics.reconciliation.domain.repository.ReconciliationReportRepository
import dev.yidafu.aqua.analytics.reconciliation.domain.repository.ReconciliationTaskRepository
import dev.yidafu.aqua.analytics.reconciliation.external.config.ReconciliationConfig
import dev.yidafu.aqua.analytics.reconciliation.external.wechat.WeChatReconciliationApi
import dev.yidafu.aqua.analytics.reconciliation.external.wechat.dto.WeChatTransactionRecord
import dev.yidafu.aqua.api.service.payment.PaymentQueryService
import dev.yidafu.aqua.api.service.payment.PaymentService
import dev.yidafu.aqua.common.domain.model.PaymentModel
import dev.yidafu.aqua.common.domain.model.ReconciliationDiscrepancyModel
import dev.yidafu.aqua.common.domain.model.ReconciliationTaskModel
import dev.yidafu.aqua.common.domain.model.enums.DiscrepancyStatus
import dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskStatus
import dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskType
import dev.yidafu.aqua.common.messaging.service.SimplifiedEventPublishService
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional
import java.util.concurrent.Executor
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 对账服务核心逻辑单元测试: 任务创建、支付/退款/结算对账执行(成功与失败)、差异处理、报表生成
 */
class ReconciliationServiceImplTest {
  private lateinit var taskRepository: ReconciliationTaskRepository
  private lateinit var discrepancyRepository: ReconciliationDiscrepancyRepository
  private lateinit var reportRepository: ReconciliationReportRepository
  private lateinit var paymentService: PaymentService
  private lateinit var paymentQueryService: PaymentQueryService
  private lateinit var weChatApi: WeChatReconciliationApi
  private lateinit var eventPublishService: SimplifiedEventPublishService
  private lateinit var service: ReconciliationServiceImpl

  private val date = LocalDate.of(2025, 6, 1)

  @BeforeEach
  fun setUp() {
    taskRepository = mockk(relaxed = true)
    discrepancyRepository = mockk(relaxed = true)
    reportRepository = mockk(relaxed = true)
    paymentService = mockk(relaxed = true)
    paymentQueryService = mockk(relaxed = true)
    weChatApi = mockk(relaxed = true)
    eventPublishService = mockk(relaxed = true)
    val config = ReconciliationConfig()
    // 同步执行,便于断言 CompletableFuture 结果
    val executor = Executor { it.run() }
    service =
      ReconciliationServiceImpl(
        taskRepository,
        discrepancyRepository,
        reportRepository,
        paymentService,
        paymentQueryService,
        weChatApi,
        eventPublishService,
        config,
        executor,
      )
    // relaxed mock 的泛型 save() 会返回裸 Object 导致 ClassCastException,
    // 统一让 save/saveAll 返回入参
    every { taskRepository.save(any()) } answers { firstArg() }
    every { discrepancyRepository.save(any()) } answers { firstArg() }
    every { reportRepository.save(any()) } answers { firstArg() }
    every { discrepancyRepository.saveAll(any<List<ReconciliationDiscrepancyModel>>()) } answers { firstArg() }
  }

  @Test
  fun `createPaymentReconciliationTask creates and saves payment task`() {
    every { taskRepository.save(any()) } answers { firstArg() }

    val task = service.createPaymentReconciliationTask(date)

    assertEquals(ReconciliationTaskType.PAYMENT, task.taskType)
    assertEquals(date.atStartOfDay(), task.taskDate)
    assertEquals(ReconciliationTaskStatus.PENDING, task.status)
    assertTrue(task.taskId.isNotBlank())
    verify(exactly = 1) { taskRepository.save(task) }
  }

  @Test
  fun `createRefundReconciliationTask creates refund task`() {
    every { taskRepository.save(any()) } answers { firstArg() }

    val task = service.createRefundReconciliationTask(date)

    assertEquals(ReconciliationTaskType.REFUND, task.taskType)
    verify(exactly = 1) { taskRepository.save(task) }
  }

  @Test
  fun `createSettlementReconciliationTask creates settlement task`() {
    every { taskRepository.save(any()) } answers { firstArg() }

    val task = service.createSettlementReconciliationTask(date)

    assertEquals(ReconciliationTaskType.SETTLEMENT, task.taskType)
    verify(exactly = 1) { taskRepository.save(task) }
  }

  @Test
  fun `executePaymentReconciliation succeeds when all records match`() {
    val task = newTask()
    val payment = PaymentModel(orderId = 1L, userId = 1L, amount = 1000L, transactionId = "wx1")
    val weChatTx = newWeChatTransaction(transactionId = "wx1", amount = 1000L)
    every { paymentQueryService.getByDateRange(any(), any()) } returns listOf(payment)
    every { weChatApi.fetchTransactions(date) } returns listOf(weChatTx)

    val result = service.executePaymentReconciliation(task).join()

    assertEquals(ReconciliationTaskStatus.SUCCESS, result.status)
    assertEquals(2, result.totalRecords)
    assertEquals(1, result.matchedRecords)
    assertEquals(0, result.unmatchedRecords)
    assertNull(result.errorMessage)
    verify { eventPublishService.publishDomainEvent("RECONCILIATION_COMPLETED", task.taskId, any()) }
    verify { reportRepository.save(any()) }
    verify { discrepancyRepository.saveAll(any<List<ReconciliationDiscrepancyModel>>()) }
  }

  @Test
  fun `executePaymentReconciliation marks FAILED when internal payment missing in wechat`() {
    val task = newTask()
    val payment = PaymentModel(orderId = 1L, userId = 1L, amount = 1000L, transactionId = "internal-only")
    every { paymentQueryService.getByDateRange(any(), any()) } returns listOf(payment)
    every { weChatApi.fetchTransactions(date) } returns emptyList()

    val result = service.executePaymentReconciliation(task).join()

    assertEquals(ReconciliationTaskStatus.FAILED, result.status)
    assertEquals(1, result.unmatchedRecords)
    assertNotNull(result.errorMessage)
    verify { eventPublishService.publishDomainEvent("RECONCILIATION_FAILED", task.taskId, any()) }
    val saved = slot<List<ReconciliationDiscrepancyModel>>()
    verify { discrepancyRepository.saveAll(capture(saved)) }
    assertEquals(1, saved.captured.size)
  }

  @Test
  fun `executePaymentReconciliation records mismatch when amounts differ`() {
    val task = newTask()
    val payment = PaymentModel(orderId = 1L, userId = 1L, amount = 1000L, transactionId = "wx1")
    val weChatTx = newWeChatTransaction(transactionId = "wx1", amount = 950L)
    every { paymentQueryService.getByDateRange(any(), any()) } returns listOf(payment)
    every { weChatApi.fetchTransactions(date) } returns listOf(weChatTx)

    val result = service.executePaymentReconciliation(task).join()

    assertEquals(ReconciliationTaskStatus.FAILED, result.status)
    val saved = slot<List<ReconciliationDiscrepancyModel>>()
    verify { discrepancyRepository.saveAll(capture(saved)) }
    assertEquals(1, saved.captured.size)
    assertEquals(ReconciliationDiscrepancyModel::class.java, saved.captured[0].javaClass)
    assertEquals(DiscrepancyStatus.UNRESOLVED, saved.captured[0].status)
    assertTrue(saved.captured[0].recordDetails!!.containsKey("amountDifferenceCents"))
  }

  @Test
  fun `executePaymentReconciliation records extra wechat transaction`() {
    val task = newTask()
    val weChatTx = newWeChatTransaction(transactionId = "wechat-only", amount = 1000L)
    every { paymentQueryService.getByDateRange(any(), any()) } returns emptyList()
    every { weChatApi.fetchTransactions(date) } returns listOf(weChatTx)

    val result = service.executePaymentReconciliation(task).join()

    assertEquals(ReconciliationTaskStatus.FAILED, result.status)
    val saved = slot<List<ReconciliationDiscrepancyModel>>()
    verify { discrepancyRepository.saveAll(capture(saved)) }
    assertEquals(1, saved.captured.size)
    assertTrue(saved.captured[0].recordDetails!!.containsKey("wechat"))
  }

  @Test
  fun `executePaymentReconciliation rethrows and marks failed on exception`() {
    val task = newTask()
    every { paymentQueryService.getByDateRange(any(), any()) } throws RuntimeException("boom")

    val future = service.executePaymentReconciliation(task)

    assertTrue(future.isCompletedExceptionally)
    assertEquals(ReconciliationTaskStatus.FAILED, task.status)
    assertNotNull(task.errorMessage)
    verify { eventPublishService.publishDomainEvent("RECONCILIATION_FAILED", task.taskId, any()) }
  }

  @Test
  fun `executeRefundReconciliation succeeds with zero records`() {
    val task = newTask(type = ReconciliationTaskType.REFUND)
    every { weChatApi.fetchRefunds(date) } returns emptyList()

    val result = service.executeRefundReconciliation(task).join()

    assertEquals(ReconciliationTaskStatus.SUCCESS, result.status)
    assertEquals(0, result.totalRecords)
    verify { eventPublishService.publishDomainEvent("RECONCILIATION_COMPLETED", task.taskId, any()) }
  }

  @Test
  fun `executeRefundReconciliation marks failed on exception`() {
    val task = newTask(type = ReconciliationTaskType.REFUND)
    every { weChatApi.fetchRefunds(date) } throws RuntimeException("boom")

    val future = service.executeRefundReconciliation(task)

    assertTrue(future.isCompletedExceptionally)
    assertEquals(ReconciliationTaskStatus.FAILED, task.status)
  }

  @Test
  fun `executeSettlementReconciliation matches all fetched settlements`() {
    val task = newTask(type = ReconciliationTaskType.SETTLEMENT)
    every { weChatApi.fetchSettlements(any()) } returns
      listOf(
        mockk(relaxed = true),
        mockk(relaxed = true),
      )

    val result = service.executeSettlementReconciliation(task).join()

    assertEquals(ReconciliationTaskStatus.SUCCESS, result.status)
    assertEquals(2, result.totalRecords)
    assertEquals(2, result.matchedRecords)
    assertEquals(0, result.unmatchedRecords)
  }

  @Test
  fun `executeSettlementReconciliation marks failed on exception`() {
    val task = newTask(type = ReconciliationTaskType.SETTLEMENT)
    every { weChatApi.fetchSettlements(any()) } throws RuntimeException("boom")

    val future = service.executeSettlementReconciliation(task)

    assertTrue(future.isCompletedExceptionally)
    assertEquals(ReconciliationTaskStatus.FAILED, task.status)
    assertNotNull(task.errorMessage)
  }

  @Test
  fun `getReconciliationTasks delegates to repository`() {
    val pageable =
      org.springframework.data.domain.PageRequest
        .of(0, 10)
    val page = mockk<org.springframework.data.domain.Page<ReconciliationTaskModel>>(relaxed = true)
    every { taskRepository.findAll(pageable) } returns page

    assertEquals(page, service.getReconciliationTasks(pageable))
  }

  @Test
  fun `getReconciliationTasksByDateRange delegates to repository`() {
    val start = LocalDate.of(2025, 5, 1)
    val end = LocalDate.of(2025, 5, 31)
    val expected = listOf(newTask())
    every { taskRepository.findByTaskDateBetween(any(), any()) } returns expected

    val result = service.getReconciliationTasksByDateRange(start, end)

    assertEquals(expected, result)
    verify { taskRepository.findByTaskDateBetween(start.atStartOfDay(), end.atTime(23, 59, 59, 999999999)) }
  }

  @Test
  fun `getReconciliationTask returns task by id`() {
    val task = newTask()
    every { taskRepository.findByTaskId("T1") } returns task
    every { taskRepository.findByTaskId("missing") } returns null

    assertEquals(task, service.getReconciliationTask("T1"))
    assertNull(service.getReconciliationTask("missing"))
  }

  @Test
  fun `hasRunningTask is true when running or pending tasks exist`() {
    every { taskRepository.findByStatusIn(any()) } returns listOf(newTask())

    assertTrue(service.hasRunningTask())
  }

  @Test
  fun `hasRunningTask is false when no tasks running`() {
    every { taskRepository.findByStatusIn(any()) } returns emptyList()

    assertFalse(service.hasRunningTask())
  }

  @Test
  fun `getDiscrepancies delegates to repository`() {
    val expected = listOf(mockk<ReconciliationDiscrepancyModel>(relaxed = true))
    every { discrepancyRepository.findByTaskId("T1") } returns expected

    assertEquals(expected, service.getDiscrepancies("T1"))
  }

  @Test
  fun `getUnresolvedDiscrepancies delegates to repository`() {
    val expected = listOf(mockk<ReconciliationDiscrepancyModel>(relaxed = true))
    every { discrepancyRepository.findByStatus("UNRESOLVED") } returns expected

    assertEquals(expected, service.getUnresolvedDiscrepancies())
  }

  @Test
  fun `resolveDiscrepancy marks resolved and publishes event`() {
    val discrepancy =
      ReconciliationDiscrepancyModel().apply {
        taskId = "T1"
        discrepancyType = dev.yidafu.aqua.common.domain.model.enums.DiscrepancyType.MISSING
        sourceSystem = dev.yidafu.aqua.common.domain.model.enums.SourceSystem.WECHAT
        recordId = "R1"
        status = DiscrepancyStatus.UNRESOLVED
      }
    every { discrepancyRepository.findById(1L) } returns Optional.of(discrepancy)

    val resolved = service.resolveDiscrepancy(1L, "核对无误", "admin")

    assertTrue(resolved)
    assertEquals(DiscrepancyStatus.RESOLVED, discrepancy.status)
    assertEquals("核对无误", discrepancy.resolutionNotes)
    assertEquals("admin", discrepancy.resolvedBy)
    assertNotNull(discrepancy.resolvedAt)
    verify { discrepancyRepository.save(discrepancy) }
    verify { eventPublishService.publishDomainEvent("DISCREPANCY_RESOLVED", "T1", any()) }
  }

  @Test
  fun `resolveDiscrepancy returns false when discrepancy missing`() {
    every { discrepancyRepository.findById(99L) } returns Optional.empty()

    assertFalse(service.resolveDiscrepancy(99L, "notes", "admin"))
  }

  @Test
  fun `resolveDiscrepancy returns false on repository exception`() {
    every { discrepancyRepository.findById(1L) } throws RuntimeException("db down")

    assertFalse(service.resolveDiscrepancy(1L, "notes", "admin"))
  }

  private fun newTask(type: ReconciliationTaskType = ReconciliationTaskType.PAYMENT): ReconciliationTaskModel {
    val task =
      when (type) {
        ReconciliationTaskType.PAYMENT -> ReconciliationTaskModel.createPaymentTask(date.atStartOfDay())
        ReconciliationTaskType.REFUND -> ReconciliationTaskModel.createRefundTask(date.atStartOfDay())
        ReconciliationTaskType.SETTLEMENT -> ReconciliationTaskModel.createSettlementTask(date.atStartOfDay())
      }
    task.id = 1L
    return task
  }

  private fun newWeChatTransaction(
    transactionId: String,
    amount: Long,
  ): WeChatTransactionRecord =
    WeChatTransactionRecord(
      transactionId = transactionId,
      outTradeNo = "OUT_$transactionId",
      transactionType = "JSAPI",
      tradeState = "SUCCESS",
      bankType = null,
      totalFee = null,
      feeType = null,
      cashFee = null,
      body = "矿泉水",
      detail = null,
      attach = null,
      deviceInfo = null,
      goodsTag = null,
      timeEnd = LocalDateTime.of(2025, 6, 1, 10, 0),
      tradeCreateTime = LocalDateTime.of(2025, 6, 1, 9, 59),
      tradePayTime = null,
      settlementTotalFee = null,
      promotionInfo = null,
      amount = amount,
      openId = null,
      subOpenId = null,
      tradeBill = null,
      goodsDetail = null,
      costType = null,
      profitSharing = null,
      returnCode = "SUCCESS",
      returnMsg = "获取成功",
      errorCode = null,
      errorCodeDes = null,
    )
}
