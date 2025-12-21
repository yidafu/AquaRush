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

import dev.yidafu.aqua.common.domain.model.enums.DiscrepancyStatus
import dev.yidafu.aqua.common.domain.model.enums.DiscrepancyType
import dev.yidafu.aqua.common.domain.model.enums.SourceSystem
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

/**
 * 对账差异实体
 */
import org.hibernate.annotations.SoftDelete

@Entity
@SoftDelete(columnName = "is_deleted")
@Table(name = "reconciliation_discrepancies")

class ReconciliationDiscrepancyModel : SoftDeletable {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: Long? = null

  @Column(name = "task_id", nullable = false)
  var taskId: String = ""

  @Enumerated(EnumType.STRING)
  @Column(name = "discrepancy_type", nullable = false)
  lateinit var discrepancyType: DiscrepancyType

  @Enumerated(EnumType.STRING)
  @Column(name = "source_system", nullable = false)
  lateinit var sourceSystem: SourceSystem

  @Column(name = "record_id", nullable = false)
  var recordId: String = ""

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "record_details", columnDefinition = "jsonb")
  var recordDetails: Map<String, Any>? = null

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  var status: DiscrepancyStatus = DiscrepancyStatus.UNRESOLVED // UNRESOLVED, RESOLVED

  @Column(name = "resolution_notes")
  var resolutionNotes: String? = null

  @Column(name = "resolved_by")
  var resolvedBy: String? = null

  @Column(name = "resolved_at")
  var resolvedAt: LocalDateTime? = null

  @Column(name = "created_at", nullable = false)
  var createdAt: LocalDateTime = LocalDateTime.now()

  @Column(name = "updated_at", nullable = false)
  var updatedAt: LocalDateTime = LocalDateTime.now()
@Column(name = "deleted_at")
  override var deletedAt: LocalDateTime? = null

  @Column(name = "deleted_by")
  override var deletedBy: Long? = null

  @PreUpdate
  fun preUpdate() {
    updatedAt = LocalDateTime.now()
  }

  companion object {
    fun createMissingRecord(
      taskId: String,
      sourceSystem: SourceSystem,
      recordId: String,
      recordDetails: Map<String, Any>,
    ): ReconciliationDiscrepancyModel {
      return ReconciliationDiscrepancyModel().apply {
        this.taskId = taskId
        this.discrepancyType = DiscrepancyType.MISSING
        this.sourceSystem = sourceSystem
        this.recordId = recordId
        this.recordDetails = recordDetails
        this.status = DiscrepancyStatus.UNRESOLVED
      }
    }

    fun createMismatchRecord(
      taskId: String,
      sourceSystem: SourceSystem,
      recordId: String,
      recordDetails: Map<String, Any>,
    ): ReconciliationDiscrepancyModel {
      return ReconciliationDiscrepancyModel().apply {
        this.taskId = taskId
        this.discrepancyType = DiscrepancyType.MISMATCH
        this.sourceSystem = sourceSystem
        this.recordId = recordId
        this.recordDetails = recordDetails
        this.status = DiscrepancyStatus.UNRESOLVED
      }
    }

    fun createExtraRecord(
      taskId: String,
      sourceSystem: SourceSystem,
      recordId: String,
      recordDetails: Map<String, Any>,
    ): ReconciliationDiscrepancyModel {
      return ReconciliationDiscrepancyModel().apply {
        this.taskId = taskId
        this.discrepancyType = DiscrepancyType.EXTRA
        this.sourceSystem = sourceSystem
        this.recordId = recordId
        this.recordDetails = recordDetails
        this.status = DiscrepancyStatus.UNRESOLVED
      }
    }
  }
}
