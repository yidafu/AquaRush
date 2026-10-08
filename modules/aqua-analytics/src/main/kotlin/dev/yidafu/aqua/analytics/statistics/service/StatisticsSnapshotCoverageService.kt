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

import dev.yidafu.aqua.analytics.statistics.domain.repository.DailyUserFactRepository
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * 统计快照覆盖判定(查询侧两段合并共用):
 * 用户事实表按日连续写入(无数据的日期也落全 0 行),故 min/max stat_date 即快照覆盖区间;
 * 覆盖区间内的日期以中间表为准,区间外(首启前/今日)回退实时 SQL。
 */
@Component
class StatisticsSnapshotCoverageService(
  private val dailyUserFactRepository: DailyUserFactRepository,
) {
  /**
   * [startDate, endDate] 中已被快照覆盖的日期集合(与用户事实表覆盖区间取交集)。
   */
  fun coveredDates(
    startDate: LocalDate,
    endDate: LocalDate,
  ): Set<LocalDate> {
    if (startDate.isAfter(endDate)) return emptySet()
    val first = dailyUserFactRepository.findFirstByOrderByStatDateAsc()?.statDate ?: return emptySet()
    val last = dailyUserFactRepository.findFirstByOrderByStatDateDesc()?.statDate ?: return emptySet()
    if (first.isAfter(endDate) || last.isBefore(startDate)) return emptySet()
    return generateSequence(maxOf(startDate, first)) { it.plusDays(1) }
      .takeWhile { !it.isAfter(minOf(endDate, last)) }
      .toSet()
  }

  /**
   * [startDate, endDate] 中未被快照覆盖的连续区间(实时 SQL 兜底段;含今日——快照最晚到昨天)。
   */
  fun uncoveredRanges(
    startDate: LocalDate,
    endDate: LocalDate,
    factDates: Set<LocalDate>,
  ): List<Pair<LocalDate, LocalDate>> {
    if (startDate.isAfter(endDate)) return emptyList()
    if (factDates.isEmpty()) return listOf(startDate to endDate)
    val covered = factDates.sorted()
    val ranges = mutableListOf<Pair<LocalDate, LocalDate>>()
    var cursor = startDate
    for (date in covered) {
      if (date.isAfter(endDate)) break
      if (date > cursor) ranges.add(cursor to date.minusDays(1))
      cursor = maxOf(cursor, date.plusDays(1))
    }
    if (cursor <= endDate) ranges.add(cursor to endDate)
    return ranges
  }
}
