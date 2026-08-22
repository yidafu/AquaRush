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

package dev.yidafu.aqua.analytics.statistics.scheduler

import dev.yidafu.aqua.analytics.statistics.config.StatisticsSnapshotConfig
import dev.yidafu.aqua.analytics.statistics.service.StatisticsSnapshotService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 统计快照定时任务调度器(第 3 层历史趋势指标中间表写入)
 * 每日凌晨 4:15 执行增量聚合(昨天/前天 + 缺口自愈),避开对账(2:00–3:30)与收银对账(3:00)。
 * 结构镜像 ReconciliationScheduler / DailyCollectionScheduler:try/catch 吞异常 + 运行态防重叠。
 */
@Component
class StatisticsSnapshotScheduler(
  private val statisticsSnapshotService: StatisticsSnapshotService,
  private val config: StatisticsSnapshotConfig,
) {
  private val logger = LoggerFactory.getLogger(StatisticsSnapshotScheduler::class.java)

  private val running = AtomicBoolean(false)

  @Scheduled(cron = "\${statistics.snapshot.cron:0 15 4 * * ?}")
  fun executeDailyStatisticsSnapshot() {
    if (!running.compareAndSet(false, true)) {
      logger.warn("统计快照任务正在运行,跳过本次调度")
      return
    }
    try {
      logger.info("开始执行统计快照任务(cron={})", config.cron)
      statisticsSnapshotService.executeIncrementalSnapshot()
      logger.info("统计快照任务执行完成")
    } catch (e: Exception) {
      logger.error("统计快照任务执行失败", e)
    } finally {
      running.set(false)
    }
  }
}
