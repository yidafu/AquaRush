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

import dev.yidafu.aqua.reconciliation.domain.model.ReconciliationReport
import java.time.LocalDateTime

/**
 * Custom repository interface for ReconciliationReport entity with QueryDSL implementations
 */
interface ReconciliationReportRepositoryCustom {
  /**
   * Find reports by generation date range
   * @param startDate the start date
   * @param endDate the end date
   * @return list of reports ordered by generation date descending
   */
  fun findByGeneratedAtBetween(startDate: LocalDateTime, endDate: LocalDateTime): List<ReconciliationReport>

  /**
   * Delete reports before a specific date
   * @param beforeDate the date before which to delete reports
   * @return number of deleted records
   */
  fun deleteReportsBefore(beforeDate: LocalDateTime): Int

  /**
   * Count reports by task ID and report type
   * @param taskId the task ID
   * @param reportType the report type
   * @return number of reports
   */
  fun countByTaskIdAndReportType(taskId: String, reportType: String): Long
}