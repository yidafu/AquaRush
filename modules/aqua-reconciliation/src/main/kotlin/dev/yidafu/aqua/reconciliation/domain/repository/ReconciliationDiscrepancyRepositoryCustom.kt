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

import dev.yidafu.aqua.common.domain.model.ReconciliationDiscrepancyModel
import java.time.LocalDateTime

/**
 * Custom repository interface for ReconciliationDiscrepancy entity with QueryDSL implementations
 */
interface ReconciliationDiscrepancyRepositoryCustom {
  /**
   * Count unresolved discrepancies by task ID
   * @param taskId the task ID
   * @return number of unresolved discrepancies
   */
  fun countUnresolvedByTaskId(taskId: String): Long

  /**
   * Count discrepancies by discrepancy type grouped by type
   * @param taskId the task ID
   * @return list of arrays containing discrepancy type and count
   */
  fun countByDiscrepancyTypeGroup(taskId: String): List<Array<Any>>

  /**
   * Find discrepancies by creation date range
   * @param startDate the start date
   * @param endDate the end date
   * @return list of discrepancies ordered by creation date descending
   */
  fun findByCreatedAtBetween(startDate: LocalDateTime, endDate: LocalDateTime): List<ReconciliationDiscrepancyModel>

  /**
   * Delete resolved discrepancies before a specific date
   * @param beforeDate the date before which to delete resolved discrepancies
   * @return number of deleted records
   */
  fun deleteResolvedBefore(beforeDate: LocalDateTime): Int
}
