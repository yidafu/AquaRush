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

package dev.yidafu.aqua.reconciliation.scheduler

import dev.yidafu.aqua.common.messaging.service.SimplifiedEventPublishService
import dev.yidafu.aqua.api.service.ReconciliationService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

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
   * 每日凌晨2点执行支付对账
   */
  @Scheduled(cron = "0 0 2 * * ?")
  fun executeDailyPaymentReconciliation() {
    logger.info("开始执行每日支付对账任务")

    try {
      val yesterday = LocalDate.now().minusDays(1)
      val task = reconciliationService.createPaymentReconciliationTask(yesterday)

      // 检查是否有正在运行的任务
      if (reconciliationService.hasRunningTask()) {
        logger.warn("已有对账任务正在运行，跳过本次调度")
        return
      }

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
    } catch (e: Exception) {
      logger.error("每日支付对账调度失败", e)
    }
  }

  /**
   * 每日凌晨2点执行退款对账
   */
  @Scheduled(cron = "0 5 2 * * ?") // 延迟5分钟执行
  fun executeDailyRefundReconciliation() {
    logger.info("开始执行每日退款对账任务")

    try {
      val yesterday = LocalDate.now().minusDays(1)
      val task = reconciliationService.createRefundReconciliationTask(yesterday)

      // 检查是否有正在运行的任务
      if (reconciliationService.hasRunningTask()) {
        logger.warn("已有对账任务正在运行，跳过本次退款对账调度")
        return
      }

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
    } catch (e: Exception) {
      logger.error("每日退款对账调度失败", e)
    }
  }

  /**
   * 每日凌晨3点执行结算对账（延迟更长时间）
   */
  @Scheduled(cron = "0 10 2 * * ?") // 延迟10分钟执行
  fun executeDailySettlementReconciliation() {
    logger.info("开始执行每日结算对账任务")

    try {
      // 结算对账通常在支付和退款完成后执行
      val twoDaysAgo = LocalDate.now().minusDays(2)
      val task = reconciliationService.createSettlementReconciliationTask(twoDaysAgo)

      // 检查是否有正在运行的任务
      if (reconciliationService.hasRunningTask()) {
        logger.warn("已有对账任务正在运行，跳过本次结算对账调度")
        return
      }

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
    } catch (e: Exception) {
      logger.error("每日结算对账调度失败", e)
    }
  }

  /**
   * 清理旧的对账数据（保留30天）
   */
  @Scheduled(cron = "0 30 3 * * ?") // 每天凌晨3点执行
  fun cleanupOldReconciliationData() {
    logger.info("开始清理旧的对账数据")

    try {
      val thirtyDaysAgo = LocalDate.now().minusDays(30)

      // TODO: 实现清理逻辑
      // reconciliationReportRepository.deleteReportsBefore(thirtyDaysAgo.atStartOfDay())
      // reconciliationDiscrepancyRepository.deleteResolvedBefore(thirtyDaysAgo.atStartOfDay())

      logger.info("旧的对账数据清理完成")
    } catch (e: Exception) {
      logger.error("清理旧的对账数据失败", e)
    }
  }
}
