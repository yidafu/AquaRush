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

package dev.yidafu.aqua.analytics.reconciliation.scheduler

import dev.yidafu.aqua.api.service.reconciliation.ReconciliationService
import dev.yidafu.aqua.common.domain.model.ReconciliationTaskModel
import dev.yidafu.aqua.common.messaging.service.SimplifiedEventPublishService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.concurrent.CompletableFuture

/**
 * 对账定时任务调度器
 */
@Component
class ReconciliationScheduler(
  private val reconciliationService: ReconciliationService,
  private val eventPublishService: SimplifiedEventPublishService,
) {
  private val logger = LoggerFactory.getLogger(ReconciliationScheduler::class.java)

  /**
   * 每日上午10:30执行支付对账(T-1)。
   * 官方约束:账单 T+1 09:00 开始生成、建议 10:00 后获取 —— 凌晨调度必然拿到
   * STATEMENT_CREATING/NO_STATEMENT_EXIST,故安排在 10:30(下载账单-常见问题)。
   */
  @Scheduled(cron = "0 30 10 * * ?")
  fun executeDailyPaymentReconciliation() {
    logger.info("开始执行每日支付对账任务")

    try {
      // 检查是否有正在运行的任务(先检查再创建,避免自身新任务阻塞调度)
      if (reconciliationService.hasRunningTask()) {
        logger.warn("已有对账任务正在运行，跳过本次调度")
        return
      }

      val yesterday = LocalDate.now().minusDays(1)
      val task = reconciliationService.createPaymentReconciliationTask(yesterday)

      logger.info("每日支付对账任务已创建: ${task.taskId}")

      // 发布调度事件
      eventPublishService.publishDomainEvent(
        eventType = "DAILY_RECONCILIATION_STARTED",
        aggregateId = task.taskId,
        eventData =
          mapOf(
            "taskType" to "PAYMENT",
            "taskDate" to yesterday,
            "schedule" to "daily",
          ),
      )

      // 派发异步执行
      dispatchExecution(task, "PAYMENT") {
        reconciliationService.executePaymentReconciliation(task)
      }
    } catch (e: Exception) {
      logger.error("每日支付对账调度失败", e)
    }
  }

  /**
   * 每日上午10:40执行退款对账(T-1;账单生成约束同支付对账,错峰10分钟避免并发任务互斥)
   */
  @Scheduled(cron = "0 40 10 * * ?")
  fun executeDailyRefundReconciliation() {
    logger.info("开始执行每日退款对账任务")

    try {
      // 检查是否有正在运行的任务(先检查再创建)
      if (reconciliationService.hasRunningTask()) {
        logger.warn("已有对账任务正在运行，跳过本次退款对账调度")
        return
      }

      val yesterday = LocalDate.now().minusDays(1)
      val task = reconciliationService.createRefundReconciliationTask(yesterday)

      logger.info("每日退款对账任务已创建: ${task.taskId}")

      // 发布调度事件
      eventPublishService.publishDomainEvent(
        eventType = "DAILY_RECONCILIATION_STARTED",
        aggregateId = task.taskId,
        eventData =
          mapOf(
            "taskType" to "REFUND",
            "taskDate" to yesterday,
            "schedule" to "daily",
          ),
      )

      // 派发异步执行
      dispatchExecution(task, "REFUND") {
        reconciliationService.executeRefundReconciliation(task)
      }
    } catch (e: Exception) {
      logger.error("每日退款对账调度失败", e)
    }
  }

  /**
   * 每日上午10:50执行资金入账对账(T-2;资金账单同为 T+1 生成,T-2 留足记账缓冲)
   */
  @Scheduled(cron = "0 50 10 * * ?")
  fun executeDailySettlementReconciliation() {
    logger.info("开始执行每日结算对账任务")

    try {
      // 检查是否有正在运行的任务(先检查再创建)
      if (reconciliationService.hasRunningTask()) {
        logger.warn("已有对账任务正在运行，跳过本次结算对账调度")
        return
      }

      // 结算对账通常在支付和退款完成后执行
      val twoDaysAgo = LocalDate.now().minusDays(2)
      val task = reconciliationService.createSettlementReconciliationTask(twoDaysAgo)

      logger.info("每日结算对账任务已创建: ${task.taskId}")

      // 发布调度事件
      eventPublishService.publishDomainEvent(
        eventType = "DAILY_RECONCILIATION_STARTED",
        aggregateId = task.taskId,
        eventData =
          mapOf(
            "taskType" to "SETTLEMENT",
            "taskDate" to twoDaysAgo,
            "schedule" to "daily",
          ),
      )

      // 派发异步执行
      dispatchExecution(task, "SETTLEMENT") {
        reconciliationService.executeSettlementReconciliation(task)
      }
    } catch (e: Exception) {
      logger.error("每日结算对账调度失败", e)
    }
  }

  /**
   * 派发对账任务异步执行(不阻塞调度线程;完成/异常仅记日志)
   */
  private fun <T> dispatchExecution(
    task: ReconciliationTaskModel,
    taskType: String,
    execution: () -> CompletableFuture<T>,
  ) {
    execution().whenComplete { _, error ->
      if (error != null) {
        logger.error("对账任务($taskType:${task.taskId})执行异常", error)
      } else {
        logger.info("对账任务($taskType:${task.taskId})执行完成")
      }
    }
  }

  /**
   * 清理过期对账数据(每日 03:30,R-PAY-04/设计 07 §3.4):
   * 报表快照保留 30 天;已处置(已解决/已忽略)差异保留 180 天;未处置差异永不清理。
   * 软删(UPDATE is_deleted=true),单条件批量删除非全表扫描。
   */
  @Scheduled(cron = "0 30 3 * * ?")
  fun cleanupOldReconciliationData() {
    logger.info("开始清理旧的对账数据")

    try {
      reconciliationService.cleanupExpiredData()
    } catch (e: Exception) {
      logger.error("清理旧的对账数据失败", e)
    }
  }
}
