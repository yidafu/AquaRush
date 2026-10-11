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
import dev.yidafu.aqua.api.service.reconciliation.ReconciliationService
import dev.yidafu.aqua.common.domain.model.PaymentModel
import dev.yidafu.aqua.common.domain.model.PaymentRefundModel
import dev.yidafu.aqua.common.domain.model.ReconciliationDiscrepancyModel
import dev.yidafu.aqua.common.domain.model.ReconciliationReportModel
import dev.yidafu.aqua.common.domain.model.ReconciliationTaskModel
import dev.yidafu.aqua.common.domain.model.enums.DiscrepancyResolveAction
import dev.yidafu.aqua.common.domain.model.enums.DiscrepancyStatus
import dev.yidafu.aqua.common.domain.model.enums.DiscrepancyType
import dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskStatus
import dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskType
import dev.yidafu.aqua.common.domain.model.enums.ReconciliationTriggerType
import dev.yidafu.aqua.common.domain.model.enums.SourceSystem
import dev.yidafu.aqua.common.messaging.service.SimplifiedEventPublishService
import dev.yidafu.aqua.common.util.MoneyUtils
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import kotlin.math.abs

/**
 * 对账服务核心实现
 * 支付/退款/结算三类对账:拉取微信账单 → 与内部流水(支付记录/退款记录)比对 → 差异落库 → 生成报表。
 */
@Service
class ReconciliationServiceImpl(
  private val reconciliationTaskRepository: ReconciliationTaskRepository,
  private val reconciliationDiscrepancyRepository: ReconciliationDiscrepancyRepository,
  private val reconciliationReportRepository: ReconciliationReportRepository,
  private val paymentService: PaymentService,
  private val paymentQueryService: PaymentQueryService,
  private val weChatReconciliationApi: WeChatReconciliationApi,
  private val eventPublishService: SimplifiedEventPublishService,
  private val config: ReconciliationConfig,
  private val taskExecutor: Executor,
) : ReconciliationService {
  private val logger = LoggerFactory.getLogger(ReconciliationService::class.java)

  /**
   * 创建支付对账任务(triggerType/executedBy:调度为 SCHEDULED/null,手动触发为 MANUAL/管理员 id)
   */
  override fun createPaymentReconciliationTask(
    date: LocalDate,
    triggerType: ReconciliationTriggerType,
    executedBy: Long?,
  ): ReconciliationTaskModel {
    logger.info("创建支付对账任务，日期: $date, 触发: $triggerType")

    val task = ReconciliationTaskModel.createPaymentTask(date.atStartOfDay(), triggerType, executedBy)
    return reconciliationTaskRepository.save(task)
  }

  /**
   * 创建退款对账任务
   */
  override fun createRefundReconciliationTask(
    date: LocalDate,
    triggerType: ReconciliationTriggerType,
    executedBy: Long?,
  ): ReconciliationTaskModel {
    logger.info("创建退款对账任务，日期: $date, 触发: $triggerType")

    val task = ReconciliationTaskModel.createRefundTask(date.atStartOfDay(), triggerType, executedBy)
    return reconciliationTaskRepository.save(task)
  }

  /**
   * 创建结算对账任务
   */
  override fun createSettlementReconciliationTask(
    date: LocalDate,
    triggerType: ReconciliationTriggerType,
    executedBy: Long?,
  ): ReconciliationTaskModel {
    logger.info("创建结算对账任务，日期: $date, 触发: $triggerType")

    val task = ReconciliationTaskModel.createSettlementTask(date.atStartOfDay(), triggerType, executedBy)
    return reconciliationTaskRepository.save(task)
  }

  /**
   * 执行支付对账任务
   * 微信交易账单 vs 内部支付流水(payments,paid_at 落账单日且 status ∈ SUCCESS/REFUNDING/REFUNDED,
   * R-PAY-04 本地账实基准),按微信交易号(transactionId)匹配,金额按分比对。
   * 未配置真实商户参数(假支付模式)→ 任务置 SKIPPED(R-PAY-04)。
   */
  @Async("reconciliationExecutor")
  @Transactional
  override fun executePaymentReconciliation(task: ReconciliationTaskModel): CompletableFuture<ReconciliationTaskModel> =
    CompletableFuture.supplyAsync({
      try {
        logger.info("开始执行支付对账任务: ${task.taskId}")

        if (skipIfWeChatNotConfigured(task, "PAYMENT")) {
          return@supplyAsync task
        }

        markTaskRunning(task, "PAYMENT")

        // 获取内部支付记录(分页拉取,R-PAY-04 本地基准:paid_at 落账单日的成功支付)
        val dayStart = task.taskDate!!.toLocalDate().atStartOfDay()
        val dayEnd = task.taskDate!!.toLocalDate().atTime(23, 59, 59, 999999999)
        val internalPayments = fetchPaidByWindow(dayStart, dayEnd)

        // 获取微信支付记录
        val weChatTransactions = weChatReconciliationApi.fetchTransactions(task.taskDate!!.toLocalDate())

        // 执行对账
        val result = performPaymentReconciliation(internalPayments, weChatTransactions, task.taskId)

        // 收尾:状态/统计/报表/事件
        completeTask(task, "PAYMENT", result)

        logger.info(
          "支付对账任务执行完成: ${task.taskId}, 状态: ${task.status}, 总记录: ${result.totalRecords}, 匹配: ${result.matchedRecords}, 差异: ${result.unmatchedRecords}",
        )

        task
      } catch (e: Exception) {
        logger.error("支付对账任务执行失败: ${task.taskId}", e)
        failTask(task, "PAYMENT", e)
        throw e
      }
    }, taskExecutor)

  /**
   * 执行退款对账任务
   * 微信退款账单 vs 内部退款记录(payment_refunds,status=COMPLETED),
   * 按微信退款单号(refundId,备用商户退款单号 outRefundNo)匹配,金额按分比对。
   */
  @Async("reconciliationExecutor")
  @Transactional
  override fun executeRefundReconciliation(task: ReconciliationTaskModel): CompletableFuture<ReconciliationTaskModel> =
    CompletableFuture.supplyAsync({
      try {
        logger.info("开始执行退款对账任务: ${task.taskId}")

        if (skipIfWeChatNotConfigured(task, "REFUND")) {
          return@supplyAsync task
        }

        markTaskRunning(task, "REFUND")

        // 获取内部已退款记录(COMPLETED,按退款成功时间归属任务日期窗口,分页取全)
        val dayStart = task.taskDate!!.toLocalDate().atStartOfDay()
        val dayEnd = task.taskDate!!.toLocalDate().atTime(23, 59, 59, 999999999)
        val internalRefunds = mutableListOf<PaymentRefundModel>()
        var refundPage = 0
        do {
          val page = paymentQueryService.getRefundedByDateRange(dayStart, dayEnd, refundPage, REFUND_BATCH_SIZE)
          internalRefunds.addAll(page.content)
          refundPage++
        } while (refundPage < page.totalPages)

        // 获取微信退款记录
        val weChatRefunds = weChatReconciliationApi.fetchRefunds(task.taskDate!!.toLocalDate())

        // 执行退款对账
        val result = performRefundReconciliation(internalRefunds, weChatRefunds, task.taskId)

        // 收尾:状态/统计/报表/事件
        completeTask(task, "REFUND", result)

        logger.info(
          "退款对账任务执行完成: ${task.taskId}, 状态: ${task.status}, 总记录: ${result.totalRecords}, 匹配: ${result.matchedRecords}, 差异: ${result.unmatchedRecords}",
        )

        task
      } catch (e: Exception) {
        logger.error("退款对账任务执行失败: ${task.taskId}", e)
        failTask(task, "REFUND", e)
        throw e
      }
    }, taskExecutor)

  /**
   * 执行结算对账任务
   * 微信结算账单 vs 内部支付流水(payments),按微信交易号(transactionId,备用商户订单号 outTradeNo)匹配,
   * 金额按分比对(微信结算按结算日归属,内部按当日支付流水近似匹配)。
   */
  @Async("reconciliationExecutor")
  @Transactional
  override fun executeSettlementReconciliation(task: ReconciliationTaskModel): CompletableFuture<ReconciliationTaskModel> =
    CompletableFuture.supplyAsync({
      try {
        logger.info("开始执行结算对账任务: ${task.taskId}")

        if (skipIfWeChatNotConfigured(task, "SETTLEMENT")) {
          return@supplyAsync task
        }

        markTaskRunning(task, "SETTLEMENT")

        // 获取内部支付记录(分页拉取,与支付对账同基准口径)
        val dayStart = task.taskDate!!.toLocalDate().atStartOfDay()
        val dayEnd = task.taskDate!!.toLocalDate().atTime(23, 59, 59, 999999999)
        val internalPayments = fetchPaidByWindow(dayStart, dayEnd)

        // 获取微信结算记录
        val weChatSettlements = weChatReconciliationApi.fetchSettlements(task.taskDate!!.toLocalDate())

        // 执行结算对账
        val result = performSettlementReconciliation(internalPayments, weChatSettlements, task.taskId)

        // 收尾:状态/统计/报表/事件
        completeTask(task, "SETTLEMENT", result)

        logger.info(
          "结算对账任务执行完成: ${task.taskId}, 状态: ${task.status}, 总记录: ${result.totalRecords}, 匹配: ${result.matchedRecords}, 差异: ${result.unmatchedRecords}",
        )

        task
      } catch (e: Exception) {
        logger.error("结算对账任务执行失败: ${task.taskId}", e)
        failTask(task, "SETTLEMENT", e)
        throw e
      }
    }, taskExecutor)

  /**
   * 查询对账任务
   */
  override fun getReconciliationTasks(pageable: Pageable): Page<ReconciliationTaskModel> = reconciliationTaskRepository.findAll(pageable)

  /**
   * 根据日期查询对账任务
   */
  override fun getReconciliationTasksByDateRange(
    startDate: LocalDate,
    endDate: LocalDate,
  ): List<ReconciliationTaskModel> =
    reconciliationTaskRepository.findByTaskDateBetween(
      startDate.atStartOfDay(),
      endDate.atTime(23, 59, 59, 999999999),
    )

  /**
   * 对账任务分页查询(后台微信资金对账页)
   */
  override fun getReconciliationTasksPaged(
    taskType: ReconciliationTaskType?,
    status: ReconciliationTaskStatus?,
    dateFrom: LocalDate?,
    dateTo: LocalDate?,
    pageable: Pageable,
  ): Page<ReconciliationTaskModel> =
    reconciliationTaskRepository.findTasksPaged(
      taskType = taskType,
      status = status,
      startDate = dateFrom?.atStartOfDay(),
      endDate = dateTo?.atTime(23, 59, 59, 999999999),
      pageable = pageable,
    )

  /**
   * 获取对账任务详情
   */
  override fun getReconciliationTask(taskId: String): ReconciliationTaskModel? = reconciliationTaskRepository.findByTaskId(taskId)

  /**
   * 检查是否有正在运行的任务
   */
  override fun hasRunningTask(): Boolean {
    val runningTasks =
      reconciliationTaskRepository.findByStatusIn(
        listOf(ReconciliationTaskStatus.RUNNING, ReconciliationTaskStatus.PENDING),
      )
    return runningTasks.isNotEmpty()
  }

  /**
   * 获取对账差异列表
   */
  override fun getDiscrepancies(taskId: String): List<ReconciliationDiscrepancyModel> =
    reconciliationDiscrepancyRepository.findByTaskId(taskId)

  /**
   * 对账差异分页查询(后台微信资金对账页)
   */
  override fun getDiscrepanciesPaged(
    taskId: String?,
    discrepancyType: DiscrepancyType?,
    status: DiscrepancyStatus?,
    pageable: Pageable,
  ): Page<ReconciliationDiscrepancyModel> =
    reconciliationDiscrepancyRepository.findDiscrepanciesPaged(taskId, discrepancyType, status, pageable)

  /**
   * 获取未解决的差异
   */
  override fun getUnresolvedDiscrepancies(): List<ReconciliationDiscrepancyModel> =
    reconciliationDiscrepancyRepository.findByStatus("UNRESOLVED")

  /**
   * 获取对账报表列表(按任务,可选报表类型过滤)
   */
  override fun getReports(
    taskId: String,
    reportType: String?,
  ): List<ReconciliationReportModel> =
    if (reportType == null) {
      reconciliationReportRepository.findByTaskId(taskId)
    } else {
      listOfNotNull(reconciliationReportRepository.findByTaskIdAndReportType(taskId, reportType))
    }

  /**
   * 清理过期对账数据(R-PAY-04/设计 07 §3.4):reports 30 天;已处置(已解决/已忽略)差异 180 天。
   * 软删(UPDATE is_deleted=true),UNRESOLVED 差异永不清理;返回 (清理报表数, 清理差异数)。
   */
  override fun cleanupExpiredData(): Pair<Int, Int> {
    val now = LocalDateTime.now()
    val cleanedReports = reconciliationReportRepository.deleteReportsBefore(now.minusDays(REPORT_RETENTION_DAYS))
    val cleanedDiscrepancies =
      reconciliationDiscrepancyRepository.deleteResolvedBefore(now.minusDays(DISCREPANCY_RETENTION_DAYS))
    if (cleanedReports > 0 || cleanedDiscrepancies > 0) {
      logger.info("对账数据清理完成: reports={}, discrepancies={}", cleanedReports, cleanedDiscrepancies)
    }
    return cleanedReports to cleanedDiscrepancies
  }

  /**
   * 解决对账差异(action=RESOLVE/IGNORE,备注必填,处置留痕;已处置差异不可再改)
   */
  @Transactional
  override fun resolveDiscrepancy(
    discrepancyId: Long,
    action: DiscrepancyResolveAction,
    resolutionNotes: String,
    resolvedBy: String,
  ): Boolean {
    try {
      val discrepancy =
        reconciliationDiscrepancyRepository.findById(discrepancyId).orElse(null)
          ?: return false

      discrepancy.status =
        when (action) {
          DiscrepancyResolveAction.RESOLVE -> DiscrepancyStatus.RESOLVED
          DiscrepancyResolveAction.IGNORE -> DiscrepancyStatus.IGNORED
        }
      discrepancy.resolutionNotes = resolutionNotes
      discrepancy.resolvedBy = resolvedBy
      discrepancy.resolvedAt = LocalDateTime.now()

      reconciliationDiscrepancyRepository.save(discrepancy)

      // 发布差异解决事件
      eventPublishService.publishDomainEvent(
        eventType = "DISCREPANCY_RESOLVED",
        aggregateId = discrepancy.taskId,
        eventData =
          mapOf<String, Any>(
            "discrepancyId" to discrepancyId,
            "discrepancyType" to discrepancy.discrepancyType.name,
            "action" to action.name,
            "resolvedBy" to resolvedBy,
            "resolvedAt" to (discrepancy.resolvedAt ?: LocalDateTime.now()),
            "resolutionNotes" to resolutionNotes,
          ),
      )

      logger.info("对账差异已处置: $discrepancyId, 动作: $action, 处置人: $resolvedBy")
      return true
    } catch (e: Exception) {
      logger.error("处置对账差异失败: $discrepancyId", e)
      return false
    }
  }

  /**
   * 执行支付对账核心逻辑
   */
  private fun performPaymentReconciliation(
    internalPayments: List<PaymentModel>,
    weChatTransactions: List<WeChatTransactionRecord>,
    taskId: String,
  ): ReconciliationResult {
    val internalMap = internalPayments.associateBy { it.transactionId }
    val weChatMap = weChatTransactions.associateBy { it.transactionId }

    val discrepancies = mutableListOf<ReconciliationDiscrepancyModel>()
    var matchedCount = 0

    // 检查内部支付记录在微信中是否存在
    internalPayments.forEach { payment ->
      val weChatTx = weChatMap[payment.transactionId]
      when {
        weChatTx == null -> {
          // 内部有支付，微信没有记录
          discrepancies.add(
            ReconciliationDiscrepancyModel.createMissingRecord(
              taskId = taskId,
              sourceSystem = SourceSystem.WECHAT,
              recordId = payment.transactionId ?: "",
              recordDetails =
                mapOf(
                  "internal" to
                    mapOf(
                      "transactionId" to payment.transactionId,
                      "orderId" to payment.orderId,
                      "amount" to MoneyUtils.fromCents(payment.amount),
                      "amountCents" to payment.amount,
                      "status" to payment.status.name,
                      "createdAt" to payment.createdAt,
                    ),
                  "amount" to MoneyUtils.fromCents(payment.amount),
                  "amountCents" to payment.amount,
                  "createdAt" to payment.createdAt,
                ),
            ),
          )
        }

        payment.amount != weChatTx.amount -> {
          // 金额不匹配 (both amounts are in cents)
          val internalAmountYuan = MoneyUtils.fromCents(payment.amount)
          val wechatAmountYuan = MoneyUtils.fromCents(weChatTx.amount)
          discrepancies.add(
            ReconciliationDiscrepancyModel.createMismatchRecord(
              taskId = taskId,
              sourceSystem = SourceSystem.INTERNAL,
              recordId = payment.transactionId ?: "",
              recordDetails =
                mapOf(
                  "internal" to
                    mapOf(
                      "transactionId" to payment.transactionId,
                      "orderId" to payment.orderId,
                      "amount" to internalAmountYuan,
                      "amountCents" to payment.amount,
                      "status" to payment.status.name,
                    ),
                  "wechat" to
                    mapOf(
                      "transactionId" to weChatTx.transactionId,
                      "outTradeNo" to weChatTx.outTradeNo,
                      "amount" to wechatAmountYuan,
                      "amountCents" to weChatTx.amount,
                      "tradeState" to weChatTx.tradeState,
                    ),
                  "amountDifference" to MoneyUtils.fromCents(abs(payment.amount - weChatTx.amount)),
                  "amountDifferenceCents" to abs(payment.amount - weChatTx.amount),
                ),
            ),
          )
        }

        else -> {
          // 匹配成功
          matchedCount++
        }
      }
    }

    // 检查微信记录在内部是否存在
    weChatTransactions.forEach { weChatTx ->
      val internalPayment = internalMap[weChatTx.transactionId]
      if (internalPayment == null) {
        // 微信有记录，内部没有
        val amountYuan = MoneyUtils.fromCents(weChatTx.amount)
        discrepancies.add(
          ReconciliationDiscrepancyModel.createMissingRecord(
            taskId = taskId,
            sourceSystem = SourceSystem.INTERNAL,
            recordId = weChatTx.transactionId,
            recordDetails =
              mapOf(
                "wechat" to
                  mapOf(
                    "transactionId" to weChatTx.transactionId,
                    "outTradeNo" to weChatTx.outTradeNo,
                    "amount" to amountYuan,
                    "amountCents" to weChatTx.amount,
                    "tradeState" to weChatTx.tradeState,
                    "timeEnd" to weChatTx.timeEnd,
                  ),
                "amount" to amountYuan,
                "amountCents" to weChatTx.amount,
              ),
          ),
        )
      }
    }

    return finalizeDiscrepancies(
      discrepancies,
      internalPayments.size + weChatTransactions.size,
      matchedCount,
      wechatTotalCount = weChatTransactions.size,
      wechatTotalAmountCents = weChatTransactions.sumOf { it.amount },
      localTotalCount = internalPayments.size,
      localTotalAmountCents = internalPayments.sumOf { it.amount },
    )
  }

  /**
   * 执行退款对账核心逻辑
   */
  private fun performRefundReconciliation(
    internalRefunds: List<PaymentRefundModel>,
    weChatRefunds: List<WeChatRefundRecord>,
    taskId: String,
  ): ReconciliationResult {
    // 内部退款:优先按微信退款单号(refundId),备用商户退款单号(outRefundNo)
    val weChatMap = weChatRefunds.associateBy { it.refundId }

    val discrepancies = mutableListOf<ReconciliationDiscrepancyModel>()
    var matchedCount = 0

    internalRefunds.forEach { refund ->
      val weChatRefund = weChatMap[refund.refundId] ?: weChatRefunds.firstOrNull { it.outRefundNo == refund.outRefundNo }
      when {
        weChatRefund == null -> {
          // 内部有退款，微信账单没有记录
          discrepancies.add(
            ReconciliationDiscrepancyModel.createMissingRecord(
              taskId = taskId,
              sourceSystem = SourceSystem.WECHAT,
              recordId = refund.refundId ?: refund.outRefundNo ?: refund.id?.toString() ?: "",
              recordDetails =
                mapOf(
                  "internal" to
                    mapOf(
                      "refundId" to refund.refundId,
                      "outRefundNo" to refund.outRefundNo,
                      "orderId" to refund.orderId,
                      "paymentId" to refund.paymentId,
                      "refundAmountCents" to refund.refundAmount,
                      "refundAmount" to MoneyUtils.fromCents(refund.refundAmount),
                      "status" to refund.status.name,
                      "refundedAt" to refund.refundedAt,
                    ),
                  "amountCents" to refund.refundAmount,
                  "refundedAt" to (refund.refundedAt?.toString() ?: ""),
                ),
            ),
          )
        }

        refund.refundAmount != weChatRefund.refundFee.toLong() -> {
          // 金额不匹配 (both amounts are in cents)
          val internalAmountYuan = MoneyUtils.fromCents(refund.refundAmount)
          val wechatAmountYuan = MoneyUtils.fromCents(weChatRefund.refundFee.toLong())
          discrepancies.add(
            ReconciliationDiscrepancyModel.createMismatchRecord(
              taskId = taskId,
              sourceSystem = SourceSystem.INTERNAL,
              recordId = weChatRefund.refundId,
              recordDetails =
                mapOf(
                  "internal" to
                    mapOf(
                      "refundId" to refund.refundId,
                      "outRefundNo" to refund.outRefundNo,
                      "orderId" to refund.orderId,
                      "refundAmountCents" to refund.refundAmount,
                      "refundAmount" to internalAmountYuan,
                      "status" to refund.status.name,
                    ),
                  "wechat" to
                    mapOf(
                      "refundId" to weChatRefund.refundId,
                      "outRefundNo" to weChatRefund.outRefundNo,
                      "transactionId" to weChatRefund.transactionId,
                      "outTradeNo" to weChatRefund.outTradeNo,
                      "refundFeeCents" to weChatRefund.refundFee.toLong(),
                      "refundFee" to wechatAmountYuan,
                      "refundStatus" to weChatRefund.refundStatus,
                    ),
                  "amountDifference" to MoneyUtils.fromCents(abs(refund.refundAmount - weChatRefund.refundFee.toLong())),
                  "amountDifferenceCents" to abs(refund.refundAmount - weChatRefund.refundFee.toLong()),
                ),
            ),
          )
        }

        else -> {
          // 匹配成功
          matchedCount++
        }
      }
    }

    // 检查微信退款记录在内部是否存在
    weChatRefunds.forEach { weChatRefund ->
      val internalRefund =
        internalRefunds.firstOrNull {
          it.refundId == weChatRefund.refundId || it.outRefundNo == weChatRefund.outRefundNo
        }
      if (internalRefund == null) {
        // 微信有退款，内部没有
        val amountYuan = MoneyUtils.fromCents(weChatRefund.refundFee.toLong())
        discrepancies.add(
          ReconciliationDiscrepancyModel.createMissingRecord(
            taskId = taskId,
            sourceSystem = SourceSystem.INTERNAL,
            recordId = weChatRefund.refundId,
            recordDetails =
              mapOf(
                "wechat" to
                  mapOf(
                    "refundId" to weChatRefund.refundId,
                    "outRefundNo" to weChatRefund.outRefundNo,
                    "transactionId" to weChatRefund.transactionId,
                    "outTradeNo" to weChatRefund.outTradeNo,
                    "refundFeeCents" to weChatRefund.refundFee.toLong(),
                    "refundFee" to amountYuan,
                    "refundStatus" to weChatRefund.refundStatus,
                    "refundSuccessTime" to weChatRefund.refundSuccessTime,
                  ),
                "amountCents" to weChatRefund.refundFee.toLong(),
              ),
          ),
        )
      }
    }

    return finalizeDiscrepancies(
      discrepancies,
      internalRefunds.size + weChatRefunds.size,
      matchedCount,
      wechatTotalCount = weChatRefunds.size,
      wechatTotalAmountCents = weChatRefunds.sumOf { it.refundFee.toLong() },
      localTotalCount = internalRefunds.size,
      localTotalAmountCents = internalRefunds.sumOf { it.refundAmount },
    )
  }

  /**
   * 执行结算对账核心逻辑
   */
  private fun performSettlementReconciliation(
    internalPayments: List<PaymentModel>,
    weChatSettlements: List<WeChatSettlementRecord>,
    taskId: String,
  ): ReconciliationResult {
    val internalByTransactionId = internalPayments.associateBy { it.transactionId }

    val discrepancies = mutableListOf<ReconciliationDiscrepancyModel>()
    var matchedCount = 0

    weChatSettlements.forEach { settlement ->
      val internalPayment =
        internalByTransactionId[settlement.transactionId]
          ?: internalPayments.firstOrNull { it.transactionId == settlement.outTradeNo }
      when {
        internalPayment == null -> {
          // 微信有结算，内部没有对应支付记录
          val amountYuan = MoneyUtils.fromCents(settlement.amount.toLong())
          discrepancies.add(
            ReconciliationDiscrepancyModel.createMissingRecord(
              taskId = taskId,
              sourceSystem = SourceSystem.INTERNAL,
              recordId = settlement.transactionId,
              recordDetails =
                mapOf(
                  "wechat" to
                    mapOf(
                      "settlementId" to settlement.settlementId,
                      "transactionId" to settlement.transactionId,
                      "outTradeNo" to settlement.outTradeNo,
                      "amountCents" to settlement.amount.toLong(),
                      "amount" to amountYuan,
                      "tradeState" to settlement.tradeState,
                      "settlementTime" to settlement.settlementTime,
                    ),
                  "amountCents" to settlement.amount.toLong(),
                ),
            ),
          )
        }

        internalPayment.amount != settlement.amount.toLong() -> {
          // 金额不匹配 (both amounts are in cents)
          val internalAmountYuan = MoneyUtils.fromCents(internalPayment.amount)
          val wechatAmountYuan = MoneyUtils.fromCents(settlement.amount.toLong())
          discrepancies.add(
            ReconciliationDiscrepancyModel.createMismatchRecord(
              taskId = taskId,
              sourceSystem = SourceSystem.INTERNAL,
              recordId = settlement.transactionId,
              recordDetails =
                mapOf(
                  "internal" to
                    mapOf(
                      "transactionId" to internalPayment.transactionId,
                      "orderId" to internalPayment.orderId,
                      "amountCents" to internalPayment.amount,
                      "amount" to internalAmountYuan,
                      "status" to internalPayment.status.name,
                    ),
                  "wechat" to
                    mapOf(
                      "settlementId" to settlement.settlementId,
                      "transactionId" to settlement.transactionId,
                      "outTradeNo" to settlement.outTradeNo,
                      "amountCents" to settlement.amount.toLong(),
                      "amount" to wechatAmountYuan,
                      "tradeState" to settlement.tradeState,
                    ),
                  "amountDifference" to MoneyUtils.fromCents(abs(internalPayment.amount - settlement.amount.toLong())),
                  "amountDifferenceCents" to abs(internalPayment.amount - settlement.amount.toLong()),
                ),
            ),
          )
        }

        else -> {
          // 匹配成功
          matchedCount++
        }
      }
    }

    // 检查内部支付记录在微信结算账单中是否存在
    internalPayments.forEach { payment ->
      val settlement =
        weChatSettlements.firstOrNull {
          it.transactionId == payment.transactionId || it.outTradeNo == payment.transactionId
        }
      if (settlement == null) {
        // 内部有支付，微信结算账单没有记录
        discrepancies.add(
          ReconciliationDiscrepancyModel.createMissingRecord(
            taskId = taskId,
            sourceSystem = SourceSystem.WECHAT,
            recordId = payment.transactionId ?: "",
            recordDetails =
              mapOf(
                "internal" to
                  mapOf(
                    "transactionId" to payment.transactionId,
                    "orderId" to payment.orderId,
                    "amountCents" to payment.amount,
                    "amount" to MoneyUtils.fromCents(payment.amount),
                    "status" to payment.status.name,
                    "createdAt" to payment.createdAt,
                  ),
                "amountCents" to payment.amount,
              ),
          ),
        )
      }
    }

    return finalizeDiscrepancies(
      discrepancies,
      internalPayments.size + weChatSettlements.size,
      matchedCount,
      wechatTotalCount = weChatSettlements.size,
      wechatTotalAmountCents = weChatSettlements.sumOf { it.amount.toLong() },
      localTotalCount = internalPayments.size,
      localTotalAmountCents = internalPayments.sumOf { it.amount },
    )
  }

  /**
   * 差异落库并汇总结果(含微信/本地两侧三方校验汇总,R-PAY-04)
   */
  private fun finalizeDiscrepancies(
    discrepancies: List<ReconciliationDiscrepancyModel>,
    totalRecords: Int,
    matchedCount: Int,
    wechatTotalCount: Int,
    wechatTotalAmountCents: Long,
    localTotalCount: Int,
    localTotalAmountCents: Long,
  ): ReconciliationResult {
    // 保存差异记录
    reconciliationDiscrepancyRepository.saveAll(discrepancies)

    return ReconciliationResult(
      totalRecords = totalRecords,
      matchedRecords = matchedCount,
      unmatchedRecords = discrepancies.size,
      wechatTotalCount = wechatTotalCount,
      wechatTotalAmountCents = wechatTotalAmountCents,
      localTotalCount = localTotalCount,
      localTotalAmountCents = localTotalAmountCents,
      discrepancies = discrepancies,
    )
  }

  /**
   * 未配置真实商户参数(假支付模式)→ 任务置 SKIPPED 并说明原因(R-PAY-04,设计 07 §6-2)。
   * 返回 true 表示已跳过。
   */
  private fun skipIfWeChatNotConfigured(
    task: ReconciliationTaskModel,
    taskType: String,
  ): Boolean {
    if (config.weChatConfigured) {
      return false
    }
    task.status = ReconciliationTaskStatus.SKIPPED
    task.endTime = LocalDateTime.now()
    task.errorMessage = SKIPPED_REASON
    reconciliationTaskRepository.save(task)

    eventPublishService.publishDomainEvent(
      eventType = "RECONCILIATION_SKIPPED",
      aggregateId = task.taskId,
      eventData =
        mapOf<String, Any>(
          "taskType" to taskType,
          "status" to ReconciliationTaskStatus.SKIPPED.name,
          "reason" to SKIPPED_REASON,
          "endTime" to (task.endTime ?: LocalDateTime.now()),
        ),
    )
    logger.warn("对账任务跳过(未配置真实商户参数): ${task.taskId}")
    return true
  }

  /**
   * 分页拉取账单日窗口内本地成功支付流水(R-PAY-04 本地账实基准,paid_at 落账单日且
   * status ∈ SUCCESS/REFUNDING/REFUNDED;规则 10 禁全量,逐页拉取至取完)。
   */
  private fun fetchPaidByWindow(
    dayStart: LocalDateTime,
    dayEnd: LocalDateTime,
  ): List<PaymentModel> {
    val payments = mutableListOf<PaymentModel>()
    var page = 0
    do {
      val result = paymentQueryService.getPaidByDateRange(dayStart, dayEnd, page, PAYMENT_BATCH_SIZE)
      payments.addAll(result.content)
      page++
    } while (page < result.totalPages)
    return payments
  }

  /**
   * 任务置为运行中并发布开始事件
   */
  private fun markTaskRunning(
    task: ReconciliationTaskModel,
    taskType: String,
  ) {
    task.status = ReconciliationTaskStatus.RUNNING
    task.startTime = LocalDateTime.now()
    reconciliationTaskRepository.save(task)

    // 发布对账开始事件
    eventPublishService.publishDomainEvent(
      eventType = "RECONCILIATION_STARTED",
      aggregateId = task.taskId,
      eventData =
        mapOf<String, Any>(
          "taskType" to taskType,
          "taskDate" to (task.taskDate ?: LocalDateTime.now()),
          "startTime" to (task.startTime ?: LocalDateTime.now()),
        ),
    )
  }

  /**
   * 任务收尾:状态/统计落库、生成报表、发布完成/失败事件。
   * R-PAY-04 口径:差异数 > 0 的任务状态仍为「成功」(对账完成 ≠ 无差异,差异经差异列表人工处置);
   * 仅执行异常(由 failTask 处理)才置 FAILED。
   */
  private fun completeTask(
    task: ReconciliationTaskModel,
    taskType: String,
    result: ReconciliationResult,
  ) {
    task.status = ReconciliationTaskStatus.SUCCESS
    task.endTime = LocalDateTime.now()
    task.totalRecords = result.totalRecords
    task.matchedRecords = result.matchedRecords
    task.unmatchedRecords = result.unmatchedRecords
    task.wechatTotalCount = result.wechatTotalCount
    task.wechatTotalAmountCents = result.wechatTotalAmountCents
    task.localTotalCount = result.localTotalCount
    task.localTotalAmountCents = result.localTotalAmountCents

    reconciliationTaskRepository.save(task)

    // 生成对账报表
    generateReconciliationReport(task, taskType, result)

    // 发布对账完成事件
    eventPublishService.publishDomainEvent(
      eventType = if (task.status == ReconciliationTaskStatus.SUCCESS) "RECONCILIATION_COMPLETED" else "RECONCILIATION_FAILED",
      aggregateId = task.taskId,
      eventData =
        mapOf<String, Any>(
          "taskType" to taskType,
          "status" to task.status.name,
          "totalRecords" to result.totalRecords,
          "matchedRecords" to result.matchedRecords,
          "unmatchedRecords" to result.unmatchedRecords,
          "discrepancies" to result.discrepancies.size,
          "endTime" to (task.endTime ?: LocalDateTime.now()),
        ),
    )
  }

  /**
   * 任务失败:状态 FAILED、发布失败事件(不重新抛出——由调用方抛出原异常)
   */
  private fun failTask(
    task: ReconciliationTaskModel,
    taskType: String,
    e: Exception,
  ) {
    // 更新任务状态为失败
    task.status = ReconciliationTaskStatus.FAILED
    task.endTime = LocalDateTime.now()
    task.errorMessage = e.message
    reconciliationTaskRepository.save(task)

    // 发布对账失败事件
    eventPublishService.publishDomainEvent(
      eventType = "RECONCILIATION_FAILED",
      aggregateId = task.taskId,
      eventData =
        mapOf<String, Any>(
          "taskType" to taskType,
          "status" to ReconciliationTaskStatus.FAILED.name,
          "error" to (e.message ?: ""),
          "endTime" to (task.endTime ?: LocalDateTime.now()),
        ),
    )
  }

  /**
   * 生成对账报表(汇总 + 有差异时明细)
   */
  private fun generateReconciliationReport(
    task: ReconciliationTaskModel,
    taskType: String,
    result: ReconciliationResult,
  ) {
    val summaryReport =
      ReconciliationReportModel.createSummaryReport(
        taskId = task.taskId!!,
        reportData =
          mapOf<String, Any>(
            "taskType" to taskType,
            "taskDate" to (task.taskDate ?: LocalDateTime.now()).toString(),
            "status" to task.status.name,
            "triggerType" to task.triggerType.name,
            "executedBy" to (task.executedBy?.toString() ?: ""),
            "totalRecords" to result.totalRecords,
            "matchedRecords" to result.matchedRecords,
            "unmatchedRecords" to result.unmatchedRecords,
            // 三方校验汇总(R-PAY-04):微信账单侧 / 本地账实侧(逐笔解析合计与微信账单汇总同源)
            "wechatTotalCount" to result.wechatTotalCount,
            "wechatTotalAmountCents" to result.wechatTotalAmountCents,
            "localTotalCount" to result.localTotalCount,
            "localTotalAmountCents" to result.localTotalAmountCents,
            "discrepancies" to result.discrepancies.groupingBy { it.discrepancyType.name }.eachCount(),
            "discrepancyDetails" to
              result.discrepancies.take(50).map {
                mapOf(
                  "id" to it.id,
                  "type" to it.discrepancyType.name,
                  "recordId" to it.recordId,
                  "sourceSystem" to it.sourceSystem.name,
                )
              },
            "executionTime" to
              if (task.startTime != null && task.endTime != null) {
                java.time.Duration
                  .between(task.startTime, task.endTime)
                  .toMillis()
              } else {
                0L
              },
          ),
      )

    reconciliationReportRepository.save(summaryReport)

    // 如果有差异，生成详细报表
    if (result.discrepancies.isNotEmpty()) {
      val detailReport =
        ReconciliationReportModel.createDetailReport(
          taskId = task.taskId!!,
          reportData =
            mapOf(
              "taskType" to taskType,
              "discrepancies" to
                result.discrepancies.map {
                  mapOf(
                    "id" to it.id,
                    "discrepancyType" to it.discrepancyType.name,
                    "sourceSystem" to it.sourceSystem.name,
                    "recordId" to it.recordId,
                    "recordDetails" to it.recordDetails,
                    "status" to it.status,
                    "createdAt" to it.createdAt.toString(),
                    "resolvedAt" to it.resolvedAt?.toString(),
                    "resolutionNotes" to it.resolutionNotes,
                  )
                },
            ),
        )

      reconciliationReportRepository.save(detailReport)
    }
  }

  /**
   * 对账结果数据类(支付/退款/结算共用;含微信/本地两侧三方校验汇总)
   */
  private data class ReconciliationResult(
    val totalRecords: Int,
    val matchedRecords: Int,
    val unmatchedRecords: Int,
    val wechatTotalCount: Int,
    val wechatTotalAmountCents: Long,
    val localTotalCount: Int,
    val localTotalAmountCents: Long,
    val discrepancies: List<ReconciliationDiscrepancyModel>,
  )

  companion object {
    /** 退款对账内部分批拉取每页大小(规则 #13:禁止全表,分批处理) */
    private const val REFUND_BATCH_SIZE = 100

    /** 支付对账内部分批拉取每页大小(规则 #13:禁止全表,分批处理) */
    private const val PAYMENT_BATCH_SIZE = 100

    /** 报表快照保留天数(R-PAY-04/设计 07 §3.4:30 天滚动清理) */
    private const val REPORT_RETENTION_DAYS = 30L

    /** 已处置差异保留天数(已解决/已忽略;UNRESOLVED 永不清理) */
    private const val DISCREPANCY_RETENTION_DAYS = 180L

    /** 未配置真实商户参数时的跳过原因(R-PAY-04:假支付模式) */
    private const val SKIPPED_REASON = "未配置真实商户参数(假支付模式),对账跳过"
  }
}
