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

package dev.yidafu.aqua.analytics.reconciliation.domain.repository

import dev.yidafu.aqua.common.domain.model.ReconciliationDiscrepancyModel
import dev.yidafu.aqua.common.domain.model.enums.DiscrepancyStatus
import dev.yidafu.aqua.common.domain.model.enums.DiscrepancyType
import dev.yidafu.aqua.common.domain.model.enums.SourceSystem
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime

/**
 * 对账差异仓储(JPA→Exposed 迁移 P3)。语义同 [ReconciliationTaskRepository]。
 */
interface ReconciliationDiscrepancyRepository {
  fun findByTaskId(taskId: String): List<ReconciliationDiscrepancyModel>

  fun findByTaskIdAndStatus(
    taskId: String,
    status: String,
  ): List<ReconciliationDiscrepancyModel>

  fun findByDiscrepancyType(discrepancyType: DiscrepancyType): List<ReconciliationDiscrepancyModel>

  fun findBySourceSystem(sourceSystem: SourceSystem): List<ReconciliationDiscrepancyModel>

  fun findByTaskIdAndDiscrepancyType(
    taskId: String,
    discrepancyType: DiscrepancyType,
  ): List<ReconciliationDiscrepancyModel>

  fun findByStatus(status: String = "UNRESOLVED"): List<ReconciliationDiscrepancyModel>

  /** 语义对齐原 JpaRepository.findById(返回 Optional)。 */
  fun findById(id: Long): java.util.Optional<ReconciliationDiscrepancyModel>

  fun save(entity: ReconciliationDiscrepancyModel): ReconciliationDiscrepancyModel

  fun saveAll(entities: List<ReconciliationDiscrepancyModel>): List<ReconciliationDiscrepancyModel>

  /** 语义对齐 @SoftDelete 下的 JpaRepository.deleteAll:全表软删(UPDATE is_deleted=true)。 */
  fun deleteAll(): Int

  fun countUnresolvedByTaskId(taskId: String): Long

  /** 按差异类型分组计数:返回 (type, count) 对。 */
  fun countByDiscrepancyTypeGroup(taskId: String): List<Pair<String, Long>>

  fun findByCreatedAtBetween(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): List<ReconciliationDiscrepancyModel>

  /** 清理 beforeDate 之前已解决的差异:语义为软删(UPDATE is_deleted=true),返回受影响行数。 */
  fun deleteResolvedBefore(beforeDate: LocalDateTime): Int

  /** 对账差异分页查询(后台微信资金对账页):任务ID/解决状态均可选,按创建时间倒序。 */
  fun findDiscrepanciesPaged(
    taskId: String?,
    status: DiscrepancyStatus?,
    pageable: Pageable,
  ): Page<ReconciliationDiscrepancyModel>
}
