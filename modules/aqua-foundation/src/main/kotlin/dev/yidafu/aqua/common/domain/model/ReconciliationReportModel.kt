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

package dev.yidafu.aqua.common.domain.model

/**
 * 对账报表实体
 */
import java.time.LocalDateTime

class ReconciliationReportModel : SoftDeletable {
  var id: Long? = null

  var taskId: String = ""

  var reportType: String = "" // SUMMARY, DETAIL

  var reportData: Map<String, Any> = emptyMap()

  var filePath: String? = null

  var fileName: String? = null

  var fileSize: Long? = null

  var generatedAt: LocalDateTime = LocalDateTime.now()

  var createdAt: LocalDateTime = LocalDateTime.now()

  var updatedAt: LocalDateTime = LocalDateTime.now()

  override var deletedAt: LocalDateTime? = null

  override var deletedBy: Long? = null

  companion object {
    fun createSummaryReport(
      taskId: String,
      reportData: Map<String, Any>,
    ): ReconciliationReportModel =
      ReconciliationReportModel().apply {
        this.taskId = taskId
        this.reportType = "SUMMARY"
        this.reportData = reportData
      }

    fun createDetailReport(
      taskId: String,
      reportData: Map<String, Any>,
    ): ReconciliationReportModel =
      ReconciliationReportModel().apply {
        this.taskId = taskId
        this.reportType = "DETAIL"
        this.reportData = reportData
      }

    fun createExcelReport(
      taskId: String,
      reportData: Map<String, Any>,
      fileName: String,
      filePath: String,
      fileSize: Long,
    ): ReconciliationReportModel =
      ReconciliationReportModel().apply {
        this.taskId = taskId
        this.reportType = "EXCEL_EXPORT"
        this.reportData = reportData
        this.fileName = fileName
        this.filePath = filePath
        this.fileSize = fileSize
      }
  }
}
