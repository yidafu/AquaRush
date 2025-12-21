/*
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

package dev.yidafu.aqua.reconciliation.domain.repository

import dev.yidafu.aqua.common.domain.model.ReconciliationTaskModel
import dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskStatus
import dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskType
import java.time.LocalDateTime

/**
 * Custom repository interface for ReconciliationTask entity with QueryDSL implementations
 */
interface ReconciliationTaskRepositoryCustom {
  /**
   * Find tasks by task date range
   * @param startDate the start date
   * @param endDate the end date
   * @return list of tasks ordered by task date descending
   */
  fun findByTaskDateBetween(startDate: LocalDateTime, endDate: LocalDateTime): List<ReconciliationTaskModel>

  /**
   * Find tasks by task type and date range
   * @param taskType the task type
   * @param startDate the start date
   * @param endDate the end date
   * @return list of tasks ordered by task date descending
   */
  fun findByTaskTypeAndTaskDateBetween(
    taskType: ReconciliationTaskType,
    startDate: LocalDateTime,
    endDate: LocalDateTime
  ): List<ReconciliationTaskModel>

  /**
   * Count tasks by task type and status
   * @param taskType the task type
   * @param status the task status
   * @return number of tasks
   */
  fun countByTaskTypeAndStatus(taskType: ReconciliationTaskType, status: ReconciliationTaskStatus): Long
}
