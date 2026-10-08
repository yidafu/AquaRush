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

package dev.yidafu.aqua.analytics.statistics.domain.repository

import dev.yidafu.aqua.common.domain.model.DailyUserFactModel
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

/**
 * 用户日事实仓储(第 3 层历史趋势指标中间表)
 * 快照任务按日期 delete-then-insert;查询侧按日期范围读取(新增用户/登录用户)。
 */
@Repository
interface DailyUserFactRepository : JpaRepository<DailyUserFactModel, Long> {
  fun findByStatDateBetween(
    startDate: LocalDate,
    endDate: LocalDate,
  ): List<DailyUserFactModel>

  fun deleteByStatDateIn(dates: Collection<LocalDate>): Long

  /**
   * 最新快照日期(用于缺口自愈判定)
   */
  fun findFirstByOrderByStatDateDesc(): DailyUserFactModel?

  /**
   * 最早快照日期(查询侧快照覆盖范围判定:用户事实表按日连续写入,min/max 即覆盖区间)
   */
  fun findFirstByOrderByStatDateAsc(): DailyUserFactModel?
}
