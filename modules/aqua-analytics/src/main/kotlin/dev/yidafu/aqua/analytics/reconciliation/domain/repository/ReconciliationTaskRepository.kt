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

import dev.yidafu.aqua.common.domain.model.ReconciliationTaskModel
import dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskStatus
import dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime

/**
 * 对账任务仓储(JPA→Exposed 迁移 P3)。
 * 原 JpaRepository 派生方法收进接口,实现为 reconciliation_tasks 上的 Exposed DSL;
 * 表带 @SoftDelete —— 查询过滤 is_deleted=false,删除翻 is_deleted=true。
 */
interface ReconciliationTaskRepository {
  fun findByTaskId(taskId: String): ReconciliationTaskModel?

  fun findByStatus(status: ReconciliationTaskStatus): List<ReconciliationTaskModel>

  fun findByTaskTypeAndStatus(
    taskType: ReconciliationTaskType,
    status: ReconciliationTaskStatus,
  ): List<ReconciliationTaskModel>

  fun findByStatusIn(statuses: List<ReconciliationTaskStatus>): List<ReconciliationTaskModel>

  fun findFirstByOrderByTaskDateDesc(): ReconciliationTaskModel?

  fun existsByStatus(status: ReconciliationTaskStatus): Boolean

  /** 语义对齐 @SoftDelete 下的 JpaRepository.deleteAll:全表软删(UPDATE is_deleted=true)。 */
  fun deleteAll(): Int

  fun findAll(pageable: Pageable): Page<ReconciliationTaskModel>

  fun save(entity: ReconciliationTaskModel): ReconciliationTaskModel

  fun findByTaskDateBetween(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): List<ReconciliationTaskModel>

  fun findByTaskTypeAndTaskDateBetween(
    taskType: ReconciliationTaskType,
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): List<ReconciliationTaskModel>

  fun countByTaskTypeAndStatus(
    taskType: ReconciliationTaskType,
    status: ReconciliationTaskStatus,
  ): Long

  /** 对账任务分页查询(后台微信资金对账页):任务类型/状态/对账日期范围均可选,按对账日期倒序。 */
  fun findTasksPaged(
    taskType: ReconciliationTaskType?,
    status: ReconciliationTaskStatus?,
    startDate: LocalDateTime?,
    endDate: LocalDateTime?,
    pageable: Pageable,
  ): Page<ReconciliationTaskModel>
}
