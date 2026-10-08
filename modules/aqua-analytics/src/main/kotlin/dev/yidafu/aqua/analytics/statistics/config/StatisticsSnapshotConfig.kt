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

package dev.yidafu.aqua.analytics.statistics.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

/**
 * 统计快照(第 3 层历史趋势指标中间表)配置属性
 * - cron:夜间快照任务调度表达式(默认凌晨 4:15,避开对账 2:00–3:30 与收银对账 3:00)
 * - incrementalDays:增量窗口天数(默认 2 = 昨天/前天;前天重算以吸收「昨天才完成」的状态迁移)
 * - maxGapBackfillDays:缺口自愈回补的最大天数(默认 30,超出部分由查询侧实时兜底,保证正确性不降级)
 */
@Component
@ConfigurationProperties(prefix = "statistics.snapshot")
data class StatisticsSnapshotConfig(
  val cron: String = "0 15 4 * * ?",
  val incrementalDays: Int = 2,
  val maxGapBackfillDays: Int = 30,
)
