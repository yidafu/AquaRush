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

package dev.yidafu.aqua.delivery.domain.repository

import dev.yidafu.aqua.common.domain.model.DailyReconciliationModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDate
import java.util.Optional

/**
 * 每日对账记录仓储(JPA→Exposed 迁移 P4):剥离 JpaRepository,仅保留实际被调用的方法。
 * 表为裸 Table 但带 is_deleted 列(类级 @SoftDelete)——每个查询显式过滤
 * `is_deleted = false`,删除为 `UPDATE is_deleted = true`;
 * report_data 为 text 列,JSON 结构化转换由仓储层负责(Jackson)。
 */
interface DailyReconciliationRepository {
  /** 分页(过滤软删;原 JPA 无序,id 升序破平防跨页重复)。 */
  fun findAll(pageable: Pageable): Page<DailyReconciliationModel>

  fun findById(id: Long): Optional<DailyReconciliationModel>

  /** upsert 语义:无 id 或 id 不存在时插入(雪花 id),否则按 id 更新业务列。 */
  fun save(entity: DailyReconciliationModel): DailyReconciliationModel

  /**
   * 清空全部活跃记录。语义对齐 JPA @SoftDelete 的 deleteAll:
   * `UPDATE ... SET is_deleted = true`(物理行保留,满足软删留痕)。
   */
  fun deleteAll()

  /** 按对账日期查询唯一记录(过滤软删)。 */
  fun findByReconciliationDate(reconciliationDate: LocalDate): DailyReconciliationModel?

  /** 日期区间查询,口径同 JPA Between:[startDate, endDate] 闭区间(过滤软删)。 */
  fun findByReconciliationDateBetween(
    startDate: LocalDate,
    endDate: LocalDate,
  ): List<DailyReconciliationModel>
}
