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
import java.time.LocalDate

/**
 * DailyUserFactModel 仓储(JPA→Exposed 迁移 P3)。
 * 原 Spring Data 派生方法收进接口,实现为 DailyUserFactsTable 上的 Exposed DSL(ExposedUserFactRepositoryImpl);
 * 快照任务按日期 delete-then-insert,查询侧按日期范围读取后组合口径。
 */
interface DailyUserFactRepository {
  fun count(): Long

  fun findByStatDateBetween(
    startDate: LocalDate,
    endDate: LocalDate,
  ): List<DailyUserFactModel>

  fun deleteByStatDateIn(dates: Collection<LocalDate>): Long

  fun saveAll(entities: List<DailyUserFactModel>): List<DailyUserFactModel>

  fun save(entity: DailyUserFactModel): DailyUserFactModel

  /** 最新快照日期(缺口自愈/回补起点判定)。 */
  fun findFirstByOrderByStatDateDesc(): DailyUserFactModel?

  /** 最早快照日期(一次性回补起点判定)。 */
  fun findFirstByOrderByStatDateAsc(): DailyUserFactModel?
}
