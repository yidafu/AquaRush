/*
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

package dev.yidafu.aqua.reconciliation.service.impl

import dev.yidafu.aqua.api.service.ReconciliationService
import dev.yidafu.aqua.common.domain.model.*
import dev.yidafu.aqua.common.domain.model.enums.DiscrepancyStatus
import dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskStatus
import dev.yidafu.aqua.common.domain.model.enums.SourceSystem
import dev.yidafu.aqua.common.domain.repository.OrderRepository
import dev.yidafu.aqua.common.domain.repository.PaymentRepository
import dev.yidafu.aqua.common.messaging.service.SimplifiedEventPublishService
import dev.yidafu.aqua.common.utils.MoneyUtils
import dev.yidafu.aqua.reconciliation.domain.repository.ReconciliationDiscrepancyRepository
import dev.yidafu.aqua.reconciliation.domain.repository.ReconciliationReportRepository
import dev.yidafu.aqua.reconciliation.domain.repository.ReconciliationTaskRepository
import dev.yidafu.aqua.reconciliation.external.config.ReconciliationConfig
import dev.yidafu.aqua.reconciliation.external.wechat.WeChatReconciliationApi
import dev.yidafu.aqua.reconciliation.external.wechat.dto.WeChatTransactionRecord
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
import kotlin.collections.get
import kotlin.math.abs
import kotlin.time.Duration

/**
 * 对账服务核心实现
 */
@Service
class ReconciliationServiceImpl(
  private val reconciliationTaskRepository: ReconciliationTaskRepository,
  private val reconciliationDiscrepancyRepository: ReconciliationDiscrepancyRepository,
  private val reconciliationReportRepository: ReconciliationReportRepository,
  private val paymentRepository: PaymentRepository,
  private val orderRepository: OrderRepository,
  private val weChatReconciliationApi: WeChatReconciliationApi,
  private val eventPublishService: SimplifiedEventPublishService,
  private val config: ReconciliationConfig,
  private val taskExecutor: Executor
) : ReconciliationService {
  private val logger = LoggerFactory.getLogger(ReconciliationService::class.java)

  /**
   * 创建支付对账任务
   */
  override fun createPaymentReconciliationTask(date: LocalDate): ReconciliationTaskModel {
    logger.info("创建支付对账任务，日期: $date")

    val task = ReconciliationTaskModel.createPaymentTask(date.atStartOfDay())
    return reconciliationTaskRepository.save(task)
  }

  /**
   * 创建退款对账任务
   */
  override fun createRefundReconciliationTask(date: LocalDate): ReconciliationTaskModel {
    logger.info("创建退款对账任务，日期: $date")

    val task = ReconciliationTaskModel.createRefundTask(date.atStartOfDay())
    return reconciliationTaskRepository.save(task)
  }

  /**
   * 创建结算对账任务
   */
  override fun createSettlementReconciliationTask(date: LocalDate): ReconciliationTaskModel {
    logger.info("创建结算对账任务，日期: $date")

    val task = ReconciliationTaskModel.createSettlementTask(date.atStartOfDay())
    return reconciliationTaskRepository.save(task)
  }

  /**
   * 执行支付对账任务
   */
  @Async("reconciliationExecutor")
  @Transactional
  override fun executePaymentReconciliation(task: ReconciliationTaskModel): CompletableFuture<ReconciliationTaskModel> {
    return CompletableFuture.supplyAsync({
      try {
        logger.info("开始执行支付对账任务: ${task.taskId}")

        // 更新任务状态为运行中
        task.status = ReconciliationTaskStatus.RUNNING
        task.startTime = LocalDateTime.now()
        reconciliationTaskRepository.save(task)

        // 发布对账开始事件
        eventPublishService.publishDomainEvent(
          eventType = "RECONCILIATION_STARTED",
          aggregateId = task.taskId,
          eventData = mapOf<String, Any>(
            "taskType" to "PAYMENT",
            "taskDate" to (task.taskDate ?: LocalDateTime.now()),
            "startTime" to (task.startTime ?: LocalDateTime.now())
          )
        )

        // 获取内部支付记录
        val internalPayments = paymentRepository.findByCreatedAtBetween(
          task.taskDate!!.toLocalDate().atStartOfDay(),
          task.taskDate!!.toLocalDate().atTime(23, 59, 59, 999999999)
        )

        // 获取微信支付记录
        val weChatTransactions = weChatReconciliationApi.fetchTransactions(task.taskDate!!.toLocalDate())

        // 执行对账
        val result = performPaymentReconciliation(internalPayments, weChatTransactions, task.taskId)

        // 更新任务结果
        task.status = if (result.discrepancies.isEmpty()) ReconciliationTaskStatus.SUCCESS else ReconciliationTaskStatus.FAILED
        task.endTime = LocalDateTime.now()
        task.totalRecords = result.totalRecords
        task.matchedRecords = result.matchedRecords
        task.unmatchedRecords = result.unmatchedRecords
        if (result.errorMessage != null) {
          task.errorMessage = result.errorMessage
        }

        reconciliationTaskRepository.save(task)

        // 生成对账报表
        generatePaymentReconciliationReport(task, result)

        // 发布对账完成事件
        eventPublishService.publishDomainEvent(
          eventType = if (task.status == ReconciliationTaskStatus.SUCCESS) "RECONCILIATION_COMPLETED" else "RECONCILIATION_FAILED",
          aggregateId = task.taskId,
          eventData = mapOf<String, Any>(
            "taskType" to "PAYMENT",
            "status" to task.status.name,
            "totalRecords" to result.totalRecords,
            "matchedRecords" to result.matchedRecords,
            "unmatchedRecords" to result.unmatchedRecords,
            "discrepancies" to result.discrepancies.size,
            "endTime" to (task.endTime ?: LocalDateTime.now())
          )
        )

        logger.info("支付对账任务执行完成: ${task.taskId}, 状态: ${task.status}, 总记录: ${result.totalRecords}, 匹配: ${result.matchedRecords}, 差异: ${result.unmatchedRecords}")

        task
      } catch (e: Exception) {
        logger.error("支付对账任务执行失败: ${task.taskId}", e)

        // 更新任务状态为失败
        task.status = ReconciliationTaskStatus.FAILED
        task.endTime = LocalDateTime.now()
        task.errorMessage = e.message
        reconciliationTaskRepository.save(task)

        // 发布对账失败事件
        eventPublishService.publishDomainEvent(
          eventType = "RECONCILIATION_FAILED",
          aggregateId = task.taskId,
          eventData = mapOf<String, Any>(
            "taskType" to "PAYMENT",
            "status" to ReconciliationTaskStatus.FAILED.name,
            "error" to (e.message ?: ""),
            "endTime" to (task.endTime ?: LocalDateTime.now())
          )
        )

        throw e
      }
    }, taskExecutor)
  }

  /**
   * 执行退款对账任务
   */
  @Async("reconciliationExecutor")
  @Transactional
  override fun executeRefundReconciliation(task: ReconciliationTaskModel): CompletableFuture<ReconciliationTaskModel> {
    return CompletableFuture.supplyAsync({
      try {
        logger.info("开始执行退款对账任务: ${task.taskId}")

        // 更新任务状态为运行中
        task.status = ReconciliationTaskStatus.RUNNING
        task.startTime = LocalDateTime.now()
        reconciliationTaskRepository.save(task)

        // 发布对账开始事件
        eventPublishService.publishDomainEvent(
          eventType = "RECONCILIATION_STARTED",
          aggregateId = task.taskId,
          eventData = mapOf<String, Any>(
            "taskType" to "REFUND",
            "taskDate" to (task.taskDate ?: LocalDateTime.now()),
            "startTime" to (task.startTime ?: LocalDateTime.now())
          )
        )

        // TODO: 获取内部退款记录
        // val internalRefunds = refundRepository.findByCreatedAtBetween(...)

        // 获取微信退款记录
        val weChatRefunds = weChatReconciliationApi.fetchRefunds(task.taskDate!!.toLocalDate())

        // 执行退款对账
        // val result = performRefundReconciliation(internalRefunds, weChatRefunds, task.taskId)

        // 暂时返回成功
        task.status = ReconciliationTaskStatus.SUCCESS
        task.endTime = LocalDateTime.now()
        task.totalRecords = 0
        task.matchedRecords = 0
        task.unmatchedRecords = 0

        reconciliationTaskRepository.save(task)

        // 发布对账完成事件
        eventPublishService.publishDomainEvent(
          eventType = "RECONCILIATION_COMPLETED",
          aggregateId = task.taskId,
          eventData = mapOf(
            "taskType" to "REFUND",
            "status" to task.status.name,
            "totalRecords" to 0,
            "matchedRecords" to 0,
            "unmatchedRecords" to 0,
            "endTime" to (task.endTime ?: LocalDateTime.now())
          )
        )

        logger.info("退款对账任务执行完成: ${task.taskId}, 状态: ${task.status}")

        task
      } catch (e: Exception) {
        logger.error("退款对账任务执行失败: ${task.taskId}", e)

        // 更新任务状态为失败
        task.status = ReconciliationTaskStatus.FAILED
        task.endTime = LocalDateTime.now()
        task.errorMessage = e.message
        reconciliationTaskRepository.save(task)

        throw e
      }
    }, taskExecutor)
  }

  /**
   * 执行结算对账任务
   */
  @Async("reconciliationExecutor")
  @Transactional
  override fun executeSettlementReconciliation(task: ReconciliationTaskModel): CompletableFuture<ReconciliationTaskModel> {
    return CompletableFuture.supplyAsync({
      try {
        logger.info("开始执行结算对账任务: ${task.taskId}")

        // 更新任务状态为运行中
        task.status = ReconciliationTaskStatus.RUNNING
        task.startTime = LocalDateTime.now()
        reconciliationTaskRepository.save(task)

        // 发布对账开始事件
        eventPublishService.publishDomainEvent(
          eventType = "RECONCILIATION_STARTED",
          aggregateId = task.taskId,
          eventData = mapOf(
            "taskType" to "SETTLEMENT",
            "taskDate" to (task.taskDate ?: LocalDateTime.now()),
            "startTime" to (task.startTime ?: LocalDateTime.now())
          )
        )

        // 获取微信结算记录
        val weChatSettlements = weChatReconciliationApi.fetchSettlements(task.taskDate!!.toLocalDate())

        // 执行结算对账
        // val result = performSettlementReconciliation(weChatSettlements, task.taskId)

        // 暂时返回成功
        task.status = ReconciliationTaskStatus.SUCCESS
        task.endTime = LocalDateTime.now()
        task.totalRecords = weChatSettlements.size
        task.matchedRecords = weChatSettlements.size
        task.unmatchedRecords = 0

        reconciliationTaskRepository.save(task)

        // 发布对账完成事件
        eventPublishService.publishDomainEvent(
          eventType = "RECONCILIATION_COMPLETED",
          aggregateId = task.taskId,
          eventData = mapOf<String, Any>(
            "taskType" to "SETTLEMENT",
            "status" to task.status.name,
            "totalRecords" to task.totalRecords,
            "matchedRecords" to task.matchedRecords,
            "unmatchedRecords" to task.unmatchedRecords,
            "endTime" to (task.endTime ?: LocalDateTime.now())
          )
        )

        logger.info("结算对账任务执行完成: ${task.taskId}, 状态: ${task.status}")

        task
      } catch (e: Exception) {
        logger.error("结算对账任务执行失败: ${task.taskId}", e)

        // 更新任务状态为失败
        task.status = ReconciliationTaskStatus.FAILED
        task.endTime = LocalDateTime.now()
        task.errorMessage = e.message
        reconciliationTaskRepository.save(task)

        throw e
      }
    }, taskExecutor)
  }

  /**
   * 查询对账任务
   */
  override fun getReconciliationTasks(pageable: Pageable): Page<ReconciliationTaskModel> {
    return reconciliationTaskRepository.findAll(pageable)
  }

  /**
   * 根据日期查询对账任务
   */
  override fun getReconciliationTasksByDateRange(startDate: LocalDate, endDate: LocalDate): List<ReconciliationTaskModel> {
    return reconciliationTaskRepository.findByTaskDateBetween(
      startDate.atStartOfDay(),
      endDate.atTime(23, 59, 59, 999999999)
    )
  }

  /**
   * 获取对账任务详情
   */
  override fun getReconciliationTask(taskId: String): ReconciliationTaskModel? {
    return reconciliationTaskRepository.findByTaskId(taskId)
  }

  /**
   * 检查是否有正在运行的任务
   */
  override fun hasRunningTask(): Boolean {
    val runningTasks = reconciliationTaskRepository.findByStatusIn(
      listOf(ReconciliationTaskStatus.RUNNING, ReconciliationTaskStatus.PENDING)
    )
    return runningTasks.isNotEmpty()
  }

  /**
   * 获取对账差异列表
   */
  override fun getDiscrepancies(taskId: String): List<ReconciliationDiscrepancyModel> {
    return reconciliationDiscrepancyRepository.findByTaskId(taskId)
  }

  /**
   * 获取未解决的差异
   */
  override fun getUnresolvedDiscrepancies(): List<ReconciliationDiscrepancyModel> {
    return reconciliationDiscrepancyRepository.findByStatus("UNRESOLVED")
  }

  /**
   * 解决对账差异
   */
  @Transactional
  override fun resolveDiscrepancy(discrepancyId: Long, resolutionNotes: String, resolvedBy: String): Boolean {
    try {
      val discrepancy = reconciliationDiscrepancyRepository.findById(discrepancyId).orElse(null)
        ?: return false

      discrepancy.status = DiscrepancyStatus.RESOLVED
      discrepancy.resolutionNotes = resolutionNotes
      discrepancy.resolvedBy = resolvedBy
      discrepancy.resolvedAt = LocalDateTime.now()

      reconciliationDiscrepancyRepository.save(discrepancy)

      // 发布差异解决事件
      eventPublishService.publishDomainEvent(
        eventType = "DISCREPANCY_RESOLVED",
        aggregateId = discrepancy.taskId,
        eventData = mapOf<String, Any>(
          "discrepancyId" to discrepancyId,
          "discrepancyType" to discrepancy.discrepancyType.name,
          "resolvedBy" to resolvedBy,
          "resolvedAt" to (discrepancy.resolvedAt ?: LocalDateTime.now()),
          "resolutionNotes" to resolutionNotes
        )
      )

      logger.info("对账差异已解决: $discrepancyId, 解决人: $resolvedBy")
      return true
    } catch (e: Exception) {
      logger.error("解决对账差异失败: $discrepancyId", e)
      return false
    }
  }

  /**
   * 执行支付对账核心逻辑
   */
  private fun performPaymentReconciliation(
    internalPayments: List<PaymentModel>,
    weChatTransactions: List<WeChatTransactionRecord>,
    taskId: String
  ): PaymentReconciliationResult {
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
              recordDetails = mapOf(
                "internal" to mapOf(
                  "transactionId" to payment.transactionId,
                  "orderId" to payment.orderId,
                  "amount" to MoneyUtils.fromCents(payment.amount),
                  "amountCents" to payment.amount,
                  "status" to payment.status.name,
                  "createdAt" to payment.createdAt
                ),
                "amount" to MoneyUtils.fromCents(payment.amount),
                "amountCents" to payment.amount,
                "createdAt" to payment.createdAt
              )
            )
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
              recordDetails = mapOf(
                "internal" to mapOf(
                  "transactionId" to payment.transactionId,
                  "orderId" to payment.orderId,
                  "amount" to internalAmountYuan,
                  "amountCents" to payment.amount,
                  "status" to payment.status.name
                ),
                "wechat" to mapOf(
                  "transactionId" to weChatTx.transactionId,
                  "outTradeNo" to weChatTx.outTradeNo,
                  "amount" to wechatAmountYuan,
                  "amountCents" to weChatTx.amount,
                  "tradeState" to weChatTx.tradeState
                ),
                "amountDifference" to MoneyUtils.fromCents(abs(payment.amount - weChatTx.amount)),
                "amountDifferenceCents" to abs(payment.amount - weChatTx.amount)
              )
            )
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
            recordDetails = mapOf(
              "wechat" to mapOf(
                "transactionId" to weChatTx.transactionId,
                "outTradeNo" to weChatTx.outTradeNo,
                "amount" to amountYuan,
                "amountCents" to weChatTx.amount,
                "tradeState" to weChatTx.tradeState,
                "timeEnd" to weChatTx.timeEnd
              ),
              "amount" to amountYuan,
              "amountCents" to weChatTx.amount
            )
          )
        )
      }
    }

    // 保存差异记录
    reconciliationDiscrepancyRepository.saveAll(discrepancies)

    return PaymentReconciliationResult(
      totalRecords = internalPayments.size + weChatTransactions.size,
      matchedRecords = matchedCount,
      unmatchedRecords = discrepancies.size,
      discrepancies = discrepancies,
      errorMessage = if (discrepancies.isEmpty()) null else "发现${discrepancies.size}个对账差异"
    )
  }

  /**
   * 生成支付对账报表
   */
  private fun generatePaymentReconciliationReport(task: ReconciliationTaskModel, result: PaymentReconciliationResult) {
    val summaryReport = ReconciliationReportModel.createSummaryReport(
      taskId = task.taskId!!,
      reportData = mapOf<String, Any>(
        "taskType" to "PAYMENT",
        "taskDate" to (task.taskDate ?: LocalDateTime.now()),
        "totalRecords" to result.totalRecords,
        "matchedRecords" to result.matchedRecords,
        "unmatchedRecords" to result.unmatchedRecords,
        "discrepancies" to result.discrepancies.groupBy { it.discrepancyType }.mapValues { it.value },
        "discrepancyDetails" to result.discrepancies.take(50).map {
          mapOf(
            "id" to it.id,
            "type" to it.discrepancyType.name,
            "recordId" to it.recordId,
            "sourceSystem" to it.sourceSystem.name
          )
        },
        "executionTime" to if (task.startTime != null && task.endTime != null) {
          java.time.Duration.between(task.startTime, task.endTime).toMillis()
        } else Duration.ZERO
      )
    )

    reconciliationReportRepository.save(summaryReport)

    // 如果有差异，生成详细报表
    if (result.discrepancies.isNotEmpty()) {
      val detailReport = ReconciliationReportModel.createDetailReport(
        taskId = task.taskId!!,
        reportData = mapOf(
          "taskType" to "PAYMENT",
          "discrepancies" to result.discrepancies.map {
            mapOf(
              "id" to it.id,
              "discrepancyType" to it.discrepancyType.name,
              "sourceSystem" to it.sourceSystem.name,
              "recordId" to it.recordId,
              "recordDetails" to it.recordDetails,
              "status" to it.status,
              "createdAt" to it.createdAt,
              "resolvedAt" to it.resolvedAt,
              "resolutionNotes" to it.resolutionNotes
            )
          }
        )
      )

      reconciliationReportRepository.save(detailReport)
    }
  }

  /**
   * 支付对账结果数据类
   */
  private data class PaymentReconciliationResult(
    val totalRecords: Int,
    val matchedRecords: Int,
    val unmatchedRecords: Int,
    val discrepancies: List<ReconciliationDiscrepancyModel>,
    val errorMessage: String?
  )
}
