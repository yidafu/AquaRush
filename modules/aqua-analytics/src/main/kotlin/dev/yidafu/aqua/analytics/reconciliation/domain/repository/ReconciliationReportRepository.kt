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

import dev.yidafu.aqua.common.domain.model.ReconciliationReportModel
import java.time.LocalDateTime

/**
 * 对账报表仓储(JPA→Exposed 迁移 P3)。语义同 [ReconciliationTaskRepository]。
 */
interface ReconciliationReportRepository {
  fun findByTaskId(taskId: String): List<ReconciliationReportModel>

  fun findByTaskIdAndReportType(
    taskId: String,
    reportType: String,
  ): ReconciliationReportModel?

  fun findByReportType(reportType: String): List<ReconciliationReportModel>

  fun findFirstByOrderByGeneratedAtDesc(): ReconciliationReportModel?

  fun findByGeneratedAtBetween(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): List<ReconciliationReportModel>

  /** 清理 beforeDate 之前的报表:语义为软删(UPDATE is_deleted=true),返回受影响行数。 */
  fun deleteReportsBefore(beforeDate: LocalDateTime): Int

  fun countByTaskIdAndReportType(
    taskId: String,
    reportType: String,
  ): Long

  fun save(entity: ReconciliationReportModel): ReconciliationReportModel

  /** 语义对齐 @SoftDelete 下的 JpaRepository.deleteAll:全表软删(UPDATE is_deleted=true)。 */
  fun deleteAll(): Int
}
