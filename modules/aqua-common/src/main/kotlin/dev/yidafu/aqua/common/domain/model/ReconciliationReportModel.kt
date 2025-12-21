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

package dev.yidafu.aqua.common.domain.model

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

/**
 * 对账报表实体
 */
import org.hibernate.annotations.SoftDelete

@Entity
@SoftDelete(columnName = "is_deleted")
@Table(name = "reconciliation_reports")

class ReconciliationReportModel : SoftDeletable {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: Long? = null

  @Column(name = "task_id", nullable = false)
  var taskId: String = ""

  @Column(name = "report_type", nullable = false)
  var reportType: String = "" // SUMMARY, DETAIL

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "report_data", columnDefinition = "jsonb", nullable = false)
  var reportData: Map<String, Any> = emptyMap()

  @Column(name = "file_path")
  var filePath: String? = null

  @Column(name = "file_name")
  var fileName: String? = null

  @Column(name = "file_size")
  var fileSize: Long? = null

  @Column(name = "generated_at", nullable = false)
  var generatedAt: LocalDateTime = LocalDateTime.now()
@Column(name = "deleted_at")
  override var deletedAt: LocalDateTime? = null

  @Column(name = "deleted_by")
  override var deletedBy: Long? = null

  companion object {
    fun createSummaryReport(
      taskId: String,
      reportData: Map<String, Any>,
    ): ReconciliationReportModel {
      return ReconciliationReportModel().apply {
        this.taskId = taskId
        this.reportType = "SUMMARY"
        this.reportData = reportData
      }
    }

    fun createDetailReport(
      taskId: String,
      reportData: Map<String, Any>,
    ): ReconciliationReportModel {
      return ReconciliationReportModel().apply {
        this.taskId = taskId
        this.reportType = "DETAIL"
        this.reportData = reportData
      }
    }

    fun createExcelReport(
      taskId: String,
      reportData: Map<String, Any>,
      fileName: String,
      filePath: String,
      fileSize: Long,
    ): ReconciliationReportModel {
      return ReconciliationReportModel().apply {
        this.taskId = taskId
        this.reportType = "EXCEL_EXPORT"
        this.reportData = reportData
        this.fileName = fileName
        this.filePath = filePath
        this.fileSize = fileSize
      }
    }
  }
}
