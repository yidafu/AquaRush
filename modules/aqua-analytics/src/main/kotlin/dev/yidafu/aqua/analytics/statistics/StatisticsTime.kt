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

package dev.yidafu.aqua.analytics.statistics

import java.time.ZoneId

/**
 * 统计域共享常量
 */
object StatisticsTime {
  /**
   * PRD 08-statistics.md §域级验收清单:统计按日/周/月聚合时区统一 Asia/Shanghai。
   * 快照任务日界与查询侧「今天/昨天」边界均使用该时区,避免依赖 JVM 默认时区(合规报告 P2-8)。
   */
  val STATS_ZONE: ZoneId = ZoneId.of("Asia/Shanghai")
}
