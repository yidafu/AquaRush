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

package dev.yidafu.aqua.reconciliation.scheduler

import dev.yidafu.aqua.reconciliation.service.DailyCollectionService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * 每日收银对账定时任务调度器
 */
@Component
class DailyCollectionScheduler(
  private val dailyCollectionService: DailyCollectionService,
) {
  private val logger = LoggerFactory.getLogger(DailyCollectionScheduler::class.java)

  /**
   * 每日凌晨3点执行收银对账
   * 对账昨日的收款和订单数据
   */
  @Scheduled(cron = "0 0 3 * * ?")
  fun executeDailyCollectionReconciliation() {
    logger.info("开始执行每日收银对账任务")

    try {
      val yesterday = LocalDate.now().minusDays(1)
      dailyCollectionService.executeDailyReconciliation(yesterday)
      logger.info("每日收银对账任务完成: {}", yesterday)
    } catch (e: Exception) {
      logger.error("每日收银对账调度失败", e)
    }
  }
}