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

package dev.yidafu.aqua.analytics.statistics.service

import java.time.LocalDate

/**
 * 统计快照服务(第 3 层历史趋势指标中间表写入侧)
 * 夜间只做增量聚合(昨天/前天 + 缺口自愈),不重复全量;首启/人工补偿走 backfillHistory。
 */
interface StatisticsSnapshotService {
  /**
   * 执行夜间增量快照:
   * 1. 事实表全空(首次部署)→ 从最早源数据日期回填至昨天,一次性;
   * 2. 否则聚合 [昨天 - incrementalDays + 1, 昨天](默认昨天/前天)+ (上次数仓日期, 昨天] 缺口;
   * 3. 每日期 delete-then-insert,幂等可重跑;缺口超过 maxGapBackfillDays 时截断并由查询侧实时兜底。
   */
  fun executeIncrementalSnapshot()

  /**
   * 逐日重建 [startDate, endDate] 的快照(首次回填 / 人工补偿共用)。
   */
  fun backfillHistory(
    startDate: LocalDate,
    endDate: LocalDate,
  )
}
