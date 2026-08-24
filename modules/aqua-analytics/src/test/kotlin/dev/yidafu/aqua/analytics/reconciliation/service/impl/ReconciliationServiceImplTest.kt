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
import dev.yidafu.aqua.analytics.reconciliation.external.wechat.dto.WeChatRefundRecord
import dev.yidafu.aqua.analytics.reconciliation.external.wechat.dto.WeChatSettlementRecord
import dev.yidafu.aqua.analytics.reconciliation.external.wechat.dto.WeChatTransactionRecord
import dev.yidafu.aqua.api.service.payment.PaymentQueryService
import dev.yidafu.aqua.api.service.payment.PaymentService
import dev.yidafu.aqua.common.domain.model.PaymentModel
import dev.yidafu.aqua.common.domain.model.PaymentRefundModel
import dev.yidafu.aqua.common.domain.model.ReconciliationDiscrepancyModel
import dev.yidafu.aqua.common.domain.model.ReconciliationReportModel
import dev.yidafu.aqua.common.domain.model.ReconciliationTaskModel
import dev.yidafu.aqua.common.domain.model.enums.DiscrepancyStatus
import dev.yidafu.aqua.common.domain.model.enums.DiscrepancyType
import dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskStatus
import dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskType
import dev.yidafu.aqua.common.domain.model.enums.SourceSystem
import dev.yidafu.aqua.common.graphql.generated.RefundStatus
import dev.yidafu.aqua.common.graphql.generated.RefundType
import dev.yidafu.aqua.common.messaging.service.SimplifiedEventPublishService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional
import java.util.concurrent.Executor

/**
 * 对账服务核心逻辑单元测试: 任务创建、支付/退款/结算对账执行(成功与失败)、差异处理、报表生成
 */
class ReconciliationServiceImplTest :
  FunSpec({
    var taskRepository: ReconciliationTaskRepository = mockk()
    var discrepancyRepository: ReconciliationDiscrepancyRepository = mockk()
    var reportRepository: ReconciliationReportRepository = mockk()
    var paymentService: PaymentService = mockk()
    var paymentQueryService: PaymentQueryService = mockk()
    var weChatApi: WeChatReconciliationApi = mockk()
    var eventPublishService: SimplifiedEventPublishService = mockk()
    var service: ReconciliationServiceImpl = mockk()
    val date = LocalDate.of(2025, 6, 1)

    fun newTask(type: ReconciliationTaskType = ReconciliationTaskType.PAYMENT): ReconciliationTaskModel {
      val task =
        when (type) {
          ReconciliationTaskType.PAYMENT -> ReconciliationTaskModel.createPaymentTask(date.atStartOfDay())
          ReconciliationTaskType.REFUND -> ReconciliationTaskModel.createRefundTask(date.atStartOfDay())
          ReconciliationTaskType.SETTLEMENT -> ReconciliationTaskModel.createSettlementTask(date.atStartOfDay())
        }
      task.id = 1L
      return task
    }

    fun newWeChatTransaction(
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

    fun newWeChatRefund(
      refundId: String,
      outRefundNo: String,
      refundFee: Long,
      transactionId: String = "tx-$refundId",
    ): WeChatRefundRecord =
      WeChatRefundRecord(
        transactionId = transactionId,
        outTradeNo = "OUT_$transactionId",
        outRefundNo = outRefundNo,
        refundId = refundId,
        refundChannel = null,
        refundFee = BigDecimal.valueOf(refundFee),
        settlementRefundFee = null,
        totalFee = null,
        settlementTotalFee = null,
        couponType = null,
        amount = BigDecimal.valueOf(refundFee),
        refundRecvAccount = null,
        refundAccount = null,
        refundRequestSource = null,
        refundSuccessTime = LocalDateTime.of(2025, 6, 1, 11, 0),
        refundStatus = "SUCCESS",
        createTime = LocalDateTime.of(2025, 6, 1, 10, 0),
        returnCode = "SUCCESS",
        returnMsg = "获取成功",
        errorCode = null,
        errorCodeDes = null,
      )

    fun newInternalRefund(
      refundId: String?,
      outRefundNo: String?,
      amount: Long,
    ): PaymentRefundModel =
      PaymentRefundModel(
        paymentId = 1L,
        orderId = 1L,
        userId = 1L,
        refundAmount = amount,
        refundId = refundId,
        outRefundNo = outRefundNo,
        status = RefundStatus.COMPLETED,
        refundType = RefundType.ORDER_REFUND,
        refundedAt = LocalDateTime.of(2025, 6, 1, 10, 30),
      )

    fun newWeChatSettlement(
      transactionId: String,
      outTradeNo: String,
      amount: Long,
    ): WeChatSettlementRecord =
      WeChatSettlementRecord(
        settlementId = "settle-$transactionId",
        transactionId = transactionId,
        outTradeNo = outTradeNo,
        amount = BigDecimal.valueOf(amount),
        settlementAmount = BigDecimal.valueOf(amount),
        mchId = "mch1",
        subMchId = null,
        deviceInfo = null,
        billType = "ALL",
        tradeType = "JSAPI",
        tradeState = "SUCCESS",
        bankType = null,
        feeType = null,
        totalFee = null,
        settlementTotalFee = null,
        rate = null,
        cashFee = null,
        refundFee = BigDecimal.ZERO,
        settlementRefundFee = BigDecimal.ZERO,
        body = "矿泉水",
        detail = null,
        attach = null,
        goodsTag = null,
        timeEnd = LocalDateTime.of(2025, 6, 1, 10, 0),
        settlementTime = LocalDateTime.of(2025, 6, 1, 12, 0),
        tradeCreateTime = LocalDateTime.of(2025, 6, 1, 9, 59),
        tradePayTime = LocalDateTime.of(2025, 6, 1, 10, 0),
        promotionInfo = null,
        createTime = LocalDateTime.of(2025, 6, 1, 12, 0),
        returnCode = "SUCCESS",
        returnMsg = "获取成功",
        errorCode = null,
        errorCodeDes = null,
      )

    beforeTest {
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
    test("createPaymentReconciliationTask creates and saves payment task") {
      every { taskRepository.save(any()) } answers { firstArg() }

      val task = service.createPaymentReconciliationTask(date)

      task.taskType shouldBe ReconciliationTaskType.PAYMENT
      task.taskDate shouldBe date.atStartOfDay()
      task.status shouldBe ReconciliationTaskStatus.PENDING
      task.taskId.isNotBlank() shouldBe true
      verify(exactly = 1) { taskRepository.save(task) }
    }
    test("createRefundReconciliationTask creates refund task") {
      every { taskRepository.save(any()) } answers { firstArg() }

      val task = service.createRefundReconciliationTask(date)

      task.taskType shouldBe ReconciliationTaskType.REFUND
      verify(exactly = 1) { taskRepository.save(task) }
    }
    test("createSettlementReconciliationTask creates settlement task") {
      every { taskRepository.save(any()) } answers { firstArg() }

      val task = service.createSettlementReconciliationTask(date)

      task.taskType shouldBe ReconciliationTaskType.SETTLEMENT
      verify(exactly = 1) { taskRepository.save(task) }
    }
    test("executePaymentReconciliation succeeds when all records match") {
      val task = newTask()
      val payment = PaymentModel(orderId = 1L, userId = 1L, amount = 1000L, transactionId = "wx1")
      val weChatTx = newWeChatTransaction(transactionId = "wx1", amount = 1000L)
      every { paymentQueryService.getByDateRange(any(), any()) } returns listOf(payment)
      every { weChatApi.fetchTransactions(date) } returns listOf(weChatTx)

      val result = service.executePaymentReconciliation(task).join()

      result.status shouldBe ReconciliationTaskStatus.SUCCESS
      result.totalRecords shouldBe 2
      result.matchedRecords shouldBe 1
      result.unmatchedRecords shouldBe 0
      result.errorMessage.shouldBeNull()
      verify { eventPublishService.publishDomainEvent("RECONCILIATION_COMPLETED", task.taskId, any()) }
      verify { reportRepository.save(any()) }
      verify { discrepancyRepository.saveAll(any<List<ReconciliationDiscrepancyModel>>()) }
    }
    test("executePaymentReconciliation marks FAILED when internal payment missing in wechat") {
      val task = newTask()
      val payment = PaymentModel(orderId = 1L, userId = 1L, amount = 1000L, transactionId = "internal-only")
      every { paymentQueryService.getByDateRange(any(), any()) } returns listOf(payment)
      every { weChatApi.fetchTransactions(date) } returns emptyList()

      val result = service.executePaymentReconciliation(task).join()

      result.status shouldBe ReconciliationTaskStatus.FAILED
      result.unmatchedRecords shouldBe 1
      result.errorMessage.shouldNotBeNull()
      verify { eventPublishService.publishDomainEvent("RECONCILIATION_FAILED", task.taskId, any()) }
      val saved = slot<List<ReconciliationDiscrepancyModel>>()
      verify { discrepancyRepository.saveAll(capture(saved)) }
      saved.captured.size shouldBe 1
    }
    test("executePaymentReconciliation records mismatch when amounts differ") {
      val task = newTask()
      val payment = PaymentModel(orderId = 1L, userId = 1L, amount = 1000L, transactionId = "wx1")
      val weChatTx = newWeChatTransaction(transactionId = "wx1", amount = 950L)
      every { paymentQueryService.getByDateRange(any(), any()) } returns listOf(payment)
      every { weChatApi.fetchTransactions(date) } returns listOf(weChatTx)

      val result = service.executePaymentReconciliation(task).join()

      result.status shouldBe ReconciliationTaskStatus.FAILED
      val saved = slot<List<ReconciliationDiscrepancyModel>>()
      verify { discrepancyRepository.saveAll(capture(saved)) }
      saved.captured.size shouldBe 1
      saved.captured[0].javaClass shouldBe ReconciliationDiscrepancyModel::class.java
      saved.captured[0].status shouldBe DiscrepancyStatus.UNRESOLVED
      saved.captured[0].recordDetails!!.containsKey("amountDifferenceCents") shouldBe true
    }
    test("executePaymentReconciliation records extra wechat transaction") {
      val task = newTask()
      val weChatTx = newWeChatTransaction(transactionId = "wechat-only", amount = 1000L)
      every { paymentQueryService.getByDateRange(any(), any()) } returns emptyList()
      every { weChatApi.fetchTransactions(date) } returns listOf(weChatTx)

      val result = service.executePaymentReconciliation(task).join()

      result.status shouldBe ReconciliationTaskStatus.FAILED
      val saved = slot<List<ReconciliationDiscrepancyModel>>()
      verify { discrepancyRepository.saveAll(capture(saved)) }
      saved.captured.size shouldBe 1
      saved.captured[0].recordDetails!!.containsKey("wechat") shouldBe true
    }
    test("executePaymentReconciliation rethrows and marks failed on exception") {
      val task = newTask()
      every { paymentQueryService.getByDateRange(any(), any()) } throws RuntimeException("boom")

      val future = service.executePaymentReconciliation(task)

      future.isCompletedExceptionally shouldBe true
      task.status shouldBe ReconciliationTaskStatus.FAILED
      task.errorMessage.shouldNotBeNull()
      verify { eventPublishService.publishDomainEvent("RECONCILIATION_FAILED", task.taskId, any()) }
    }
    test("executeRefundReconciliation succeeds with zero records") {
      val task = newTask(type = ReconciliationTaskType.REFUND)
      every { paymentQueryService.getRefundedByDateRange(any(), any(), any(), any()) } returns
        PageImpl(emptyList<PaymentRefundModel>(), PageRequest.of(0, 100), 0)
      every { weChatApi.fetchRefunds(date) } returns emptyList()

      val result = service.executeRefundReconciliation(task).join()

      result.status shouldBe ReconciliationTaskStatus.SUCCESS
      result.totalRecords shouldBe 0
      result.unmatchedRecords shouldBe 0
      verify { eventPublishService.publishDomainEvent("RECONCILIATION_COMPLETED", task.taskId, any()) }
      verify { reportRepository.save(any()) }
    }
    test("executeRefundReconciliation succeeds when all refunds match") {
      val task = newTask(type = ReconciliationTaskType.REFUND)
      val internalRefund = newInternalRefund(refundId = "refund1", outRefundNo = "out1", amount = 1000L)
      val weChatRefund = newWeChatRefund(refundId = "refund1", outRefundNo = "out1", refundFee = 1000L)
      every { paymentQueryService.getRefundedByDateRange(any(), any(), any(), any()) } returns
        PageImpl(listOf(internalRefund), PageRequest.of(0, 100), 1)
      every { weChatApi.fetchRefunds(date) } returns listOf(weChatRefund)

      val result = service.executeRefundReconciliation(task).join()

      result.status shouldBe ReconciliationTaskStatus.SUCCESS
      result.totalRecords shouldBe 2
      result.matchedRecords shouldBe 1
      result.unmatchedRecords shouldBe 0
      result.errorMessage.shouldBeNull()
      verify { eventPublishService.publishDomainEvent("RECONCILIATION_COMPLETED", task.taskId, any()) }
      verify { reportRepository.save(any()) }
    }
    test("executeRefundReconciliation matches by outRefundNo fallback") {
      val task = newTask(type = ReconciliationTaskType.REFUND)
      // 内部退款无微信退款单号,以商户退款单号(outRefundNo)匹配
      val internalRefund = newInternalRefund(refundId = null, outRefundNo = "out1", amount = 1000L)
      val weChatRefund = newWeChatRefund(refundId = "refund1", outRefundNo = "out1", refundFee = 1000L)
      every { paymentQueryService.getRefundedByDateRange(any(), any(), any(), any()) } returns
        PageImpl(listOf(internalRefund), PageRequest.of(0, 100), 1)
      every { weChatApi.fetchRefunds(date) } returns listOf(weChatRefund)

      val result = service.executeRefundReconciliation(task).join()

      result.status shouldBe ReconciliationTaskStatus.SUCCESS
      result.matchedRecords shouldBe 1
      result.unmatchedRecords shouldBe 0
    }
    test("executeRefundReconciliation records MISMATCH when refund amounts differ") {
      val task = newTask(type = ReconciliationTaskType.REFUND)
      val internalRefund = newInternalRefund(refundId = "refund1", outRefundNo = "out1", amount = 1000L)
      val weChatRefund = newWeChatRefund(refundId = "refund1", outRefundNo = "out1", refundFee = 950L)
      every { paymentQueryService.getRefundedByDateRange(any(), any(), any(), any()) } returns
        PageImpl(listOf(internalRefund), PageRequest.of(0, 100), 1)
      every { weChatApi.fetchRefunds(date) } returns listOf(weChatRefund)

      val result = service.executeRefundReconciliation(task).join()

      result.status shouldBe ReconciliationTaskStatus.FAILED
      val saved = slot<List<ReconciliationDiscrepancyModel>>()
      verify { discrepancyRepository.saveAll(capture(saved)) }
      saved.captured.size shouldBe 1
      saved.captured[0].discrepancyType shouldBe DiscrepancyType.MISMATCH
      saved.captured[0].status shouldBe DiscrepancyStatus.UNRESOLVED
      saved.captured[0].recordDetails!!.containsKey("amountDifferenceCents") shouldBe true
    }
    test("executeRefundReconciliation records MISSING when internal refund absent in wechat") {
      val task = newTask(type = ReconciliationTaskType.REFUND)
      val internalRefund = newInternalRefund(refundId = "refund-only", outRefundNo = "out-only", amount = 1000L)
      every { paymentQueryService.getRefundedByDateRange(any(), any(), any(), any()) } returns
        PageImpl(listOf(internalRefund), PageRequest.of(0, 100), 1)
      every { weChatApi.fetchRefunds(date) } returns emptyList()

      val result = service.executeRefundReconciliation(task).join()

      result.status shouldBe ReconciliationTaskStatus.FAILED
      val saved = slot<List<ReconciliationDiscrepancyModel>>()
      verify { discrepancyRepository.saveAll(capture(saved)) }
      saved.captured.size shouldBe 1
      saved.captured[0].discrepancyType shouldBe DiscrepancyType.MISSING
      saved.captured[0].sourceSystem shouldBe SourceSystem.WECHAT
    }
    test("executeRefundReconciliation records MISSING for extra wechat refund") {
      val task = newTask(type = ReconciliationTaskType.REFUND)
      val weChatRefund = newWeChatRefund(refundId = "wechat-only", outRefundNo = "out-wechat-only", refundFee = 1000L)
      every { paymentQueryService.getRefundedByDateRange(any(), any(), any(), any()) } returns
        PageImpl(emptyList<PaymentRefundModel>(), PageRequest.of(0, 100), 0)
      every { weChatApi.fetchRefunds(date) } returns listOf(weChatRefund)

      val result = service.executeRefundReconciliation(task).join()

      result.status shouldBe ReconciliationTaskStatus.FAILED
      val saved = slot<List<ReconciliationDiscrepancyModel>>()
      verify { discrepancyRepository.saveAll(capture(saved)) }
      saved.captured.size shouldBe 1
      saved.captured[0].sourceSystem shouldBe SourceSystem.INTERNAL
      saved.captured[0].recordDetails!!.containsKey("wechat") shouldBe true
    }
    test("executeRefundReconciliation marks failed on exception") {
      val task = newTask(type = ReconciliationTaskType.REFUND)
      every { weChatApi.fetchRefunds(date) } throws RuntimeException("boom")

      val future = service.executeRefundReconciliation(task)

      future.isCompletedExceptionally shouldBe true
      task.status shouldBe ReconciliationTaskStatus.FAILED
    }
    test("executeSettlementReconciliation succeeds when all settlements match internal payments") {
      val task = newTask(type = ReconciliationTaskType.SETTLEMENT)
      val payment = PaymentModel(orderId = 1L, userId = 1L, amount = 1000L, transactionId = "wx1")
      val settlement = newWeChatSettlement(transactionId = "wx1", outTradeNo = "OUT_wx1", amount = 1000L)
      every { paymentQueryService.getByDateRange(any(), any()) } returns listOf(payment)
      every { weChatApi.fetchSettlements(any()) } returns listOf(settlement)

      val result = service.executeSettlementReconciliation(task).join()

      result.status shouldBe ReconciliationTaskStatus.SUCCESS
      result.totalRecords shouldBe 2
      result.matchedRecords shouldBe 1
      result.unmatchedRecords shouldBe 0
      result.errorMessage.shouldBeNull()
      verify { eventPublishService.publishDomainEvent("RECONCILIATION_COMPLETED", task.taskId, any()) }
      verify { reportRepository.save(any()) }
    }
    test("executeSettlementReconciliation records MISMATCH when settlement amount differs") {
      val task = newTask(type = ReconciliationTaskType.SETTLEMENT)
      val payment = PaymentModel(orderId = 1L, userId = 1L, amount = 1000L, transactionId = "wx1")
      val settlement = newWeChatSettlement(transactionId = "wx1", outTradeNo = "OUT_wx1", amount = 980L)
      every { paymentQueryService.getByDateRange(any(), any()) } returns listOf(payment)
      every { weChatApi.fetchSettlements(any()) } returns listOf(settlement)

      val result = service.executeSettlementReconciliation(task).join()

      result.status shouldBe ReconciliationTaskStatus.FAILED
      val saved = slot<List<ReconciliationDiscrepancyModel>>()
      verify { discrepancyRepository.saveAll(capture(saved)) }
      saved.captured.size shouldBe 1
      saved.captured[0].discrepancyType shouldBe DiscrepancyType.MISMATCH
    }
    test("executeSettlementReconciliation records MISSING for wechat-only settlement") {
      val task = newTask(type = ReconciliationTaskType.SETTLEMENT)
      val settlement = newWeChatSettlement(transactionId = "wechat-only", outTradeNo = "OUT_wechat-only", amount = 1000L)
      every { paymentQueryService.getByDateRange(any(), any()) } returns emptyList()
      every { weChatApi.fetchSettlements(any()) } returns listOf(settlement)

      val result = service.executeSettlementReconciliation(task).join()

      result.status shouldBe ReconciliationTaskStatus.FAILED
      val saved = slot<List<ReconciliationDiscrepancyModel>>()
      verify { discrepancyRepository.saveAll(capture(saved)) }
      saved.captured.size shouldBe 1
      saved.captured[0].sourceSystem shouldBe SourceSystem.INTERNAL
      saved.captured[0].recordDetails!!.containsKey("wechat") shouldBe true
    }
    test("executeSettlementReconciliation marks failed on exception") {
      val task = newTask(type = ReconciliationTaskType.SETTLEMENT)
      every { weChatApi.fetchSettlements(any()) } throws RuntimeException("boom")

      val future = service.executeSettlementReconciliation(task)

      future.isCompletedExceptionally shouldBe true
      task.status shouldBe ReconciliationTaskStatus.FAILED
      task.errorMessage.shouldNotBeNull()
    }
    test("getReconciliationTasks delegates to repository") {
      val pageable =
        org.springframework.data.domain.PageRequest
          .of(0, 10)
      val page = mockk<org.springframework.data.domain.Page<ReconciliationTaskModel>>(relaxed = true)
      every { taskRepository.findAll(pageable) } returns page

      service.getReconciliationTasks(pageable) shouldBe page
    }
    test("getReconciliationTasksByDateRange delegates to repository") {
      val start = LocalDate.of(2025, 5, 1)
      val end = LocalDate.of(2025, 5, 31)
      val expected = listOf(newTask())
      every { taskRepository.findByTaskDateBetween(any(), any()) } returns expected

      val result = service.getReconciliationTasksByDateRange(start, end)

      result shouldBe expected
      verify { taskRepository.findByTaskDateBetween(start.atStartOfDay(), end.atTime(23, 59, 59, 999999999)) }
    }
    test("getReconciliationTask returns task by id") {
      val task = newTask()
      every { taskRepository.findByTaskId("T1") } returns task
      every { taskRepository.findByTaskId("missing") } returns null

      service.getReconciliationTask("T1") shouldBe task
      service.getReconciliationTask("missing").shouldBeNull()
    }
    test("hasRunningTask is true when running or pending tasks exist") {
      every { taskRepository.findByStatusIn(any()) } returns listOf(newTask())

      service.hasRunningTask() shouldBe true
    }
    test("hasRunningTask is false when no tasks running") {
      every { taskRepository.findByStatusIn(any()) } returns emptyList()

      service.hasRunningTask() shouldBe false
    }
    test("getDiscrepancies delegates to repository") {
      val expected = listOf(mockk<ReconciliationDiscrepancyModel>(relaxed = true))
      every { discrepancyRepository.findByTaskId("T1") } returns expected

      service.getDiscrepancies("T1") shouldBe expected
    }
    test("getUnresolvedDiscrepancies delegates to repository") {
      val expected = listOf(mockk<ReconciliationDiscrepancyModel>(relaxed = true))
      every { discrepancyRepository.findByStatus("UNRESOLVED") } returns expected

      service.getUnresolvedDiscrepancies() shouldBe expected
    }
    test("getReconciliationTasksPaged delegates to repository with date window") {
      val pageable = PageRequest.of(0, 10)
      val expected = PageImpl(listOf(newTask()), pageable, 1)
      every {
        taskRepository.findTasksPaged(
          ReconciliationTaskType.PAYMENT,
          ReconciliationTaskStatus.SUCCESS,
          LocalDate.of(2025, 6, 1).atStartOfDay(),
          LocalDate.of(2025, 6, 1).atTime(23, 59, 59, 999999999),
          pageable,
        )
      } returns expected

      val result =
        service.getReconciliationTasksPaged(
          ReconciliationTaskType.PAYMENT,
          ReconciliationTaskStatus.SUCCESS,
          LocalDate.of(2025, 6, 1),
          LocalDate.of(2025, 6, 1),
          pageable,
        )

      result shouldBe expected
    }
    test("getReconciliationTasksPaged passes null filters through") {
      val pageable = PageRequest.of(0, 10)
      val expected = PageImpl(listOf(newTask()), pageable, 1)
      every { taskRepository.findTasksPaged(null, null, null, null, pageable) } returns expected

      val result = service.getReconciliationTasksPaged(null, null, null, null, pageable)

      result shouldBe expected
    }
    test("getDiscrepanciesPaged delegates to repository") {
      val pageable = PageRequest.of(0, 20)
      val expected = PageImpl(listOf(mockk<ReconciliationDiscrepancyModel>(relaxed = true)), pageable, 1)
      every {
        discrepancyRepository.findDiscrepanciesPaged("T1", DiscrepancyStatus.UNRESOLVED, pageable)
      } returns expected

      val result =
        service.getDiscrepanciesPaged(
          "T1",
          DiscrepancyStatus.UNRESOLVED,
          pageable,
        )

      result shouldBe expected
    }
    test("getReports returns all reports when reportType is null") {
      val expected = listOf(mockk<ReconciliationReportModel>(relaxed = true))
      every { reportRepository.findByTaskId("T1") } returns expected

      service.getReports("T1", null) shouldBe expected
    }
    test("getReports filters by reportType") {
      val expected = mockk<ReconciliationReportModel>(relaxed = true)
      every { reportRepository.findByTaskIdAndReportType("T1", "SUMMARY") } returns expected

      service.getReports("T1", "SUMMARY") shouldBe listOf(expected)
    }
    test("getReports returns empty when reportType not found") {
      every { reportRepository.findByTaskIdAndReportType("T1", "SUMMARY") } returns null

      service.getReports("T1", "SUMMARY") shouldBe emptyList()
    }
    test("resolveDiscrepancy marks resolved and publishes event") {
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

      resolved shouldBe true
      discrepancy.status shouldBe DiscrepancyStatus.RESOLVED
      discrepancy.resolutionNotes shouldBe "核对无误"
      discrepancy.resolvedBy shouldBe "admin"
      discrepancy.resolvedAt.shouldNotBeNull()
      verify { discrepancyRepository.save(discrepancy) }
      verify { eventPublishService.publishDomainEvent("DISCREPANCY_RESOLVED", "T1", any()) }
    }
    test("resolveDiscrepancy returns false when discrepancy missing") {
      every { discrepancyRepository.findById(99L) } returns Optional.empty()

      service.resolveDiscrepancy(99L, "notes", "admin") shouldBe false
    }
    test("resolveDiscrepancy returns false on repository exception") {
      every { discrepancyRepository.findById(1L) } throws RuntimeException("db down")

      service.resolveDiscrepancy(1L, "notes", "admin") shouldBe false
    }
  })
