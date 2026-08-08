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
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * 对账报表仓库接口
 */
@Repository
interface ReconciliationReportRepository :
  JpaRepository<ReconciliationReportModel, Long>,
  ReconciliationReportRepositoryCustom {
  /**
   * 根据任务ID查找报表
   */
  fun findByTaskId(taskId: String): List<ReconciliationReportModel>

  /**
   * 根据任务ID和报表类型查找报表
   */
  fun findByTaskIdAndReportType(
    taskId: String,
    reportType: String,
  ): ReconciliationReportModel?

  /**
   * 根据报表类型查找报表
   */
  fun findByReportType(reportType: String): List<ReconciliationReportModel>

  /**
   * 查找最新的报表
   */
  fun findFirstByOrderByGeneratedAtDesc(): ReconciliationReportModel?
}
