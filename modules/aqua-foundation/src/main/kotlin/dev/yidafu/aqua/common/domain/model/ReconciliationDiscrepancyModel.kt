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
 * 对账差异实体
 */
import dev.yidafu.aqua.common.domain.model.enums.DiscrepancyStatus
import dev.yidafu.aqua.common.domain.model.enums.DiscrepancyType
import dev.yidafu.aqua.common.domain.model.enums.SourceSystem
import java.time.LocalDateTime

class ReconciliationDiscrepancyModel : SoftDeletable {
  var id: Long? = null

  var taskId: String = ""

  lateinit var discrepancyType: DiscrepancyType

  lateinit var sourceSystem: SourceSystem

  var recordId: String = ""

  var recordDetails: Map<String, Any>? = null

  var status: DiscrepancyStatus = DiscrepancyStatus.UNRESOLVED // UNRESOLVED, RESOLVED

  var resolutionNotes: String? = null

  var resolvedBy: String? = null

  var resolvedAt: LocalDateTime? = null

  var createdAt: LocalDateTime = LocalDateTime.now()

  var updatedAt: LocalDateTime = LocalDateTime.now()

  override var deletedAt: LocalDateTime? = null

  override var deletedBy: Long? = null

  companion object {
    fun createMissingRecord(
      taskId: String,
      sourceSystem: SourceSystem,
      recordId: String,
      recordDetails: Map<String, Any>,
    ): ReconciliationDiscrepancyModel =
      ReconciliationDiscrepancyModel().apply {
        this.taskId = taskId
        this.discrepancyType = DiscrepancyType.MISSING
        this.sourceSystem = sourceSystem
        this.recordId = recordId
        this.recordDetails = recordDetails
        this.status = DiscrepancyStatus.UNRESOLVED
      }

    fun createMismatchRecord(
      taskId: String,
      sourceSystem: SourceSystem,
      recordId: String,
      recordDetails: Map<String, Any>,
    ): ReconciliationDiscrepancyModel =
      ReconciliationDiscrepancyModel().apply {
        this.taskId = taskId
        this.discrepancyType = DiscrepancyType.MISMATCH
        this.sourceSystem = sourceSystem
        this.recordId = recordId
        this.recordDetails = recordDetails
        this.status = DiscrepancyStatus.UNRESOLVED
      }

    fun createExtraRecord(
      taskId: String,
      sourceSystem: SourceSystem,
      recordId: String,
      recordDetails: Map<String, Any>,
    ): ReconciliationDiscrepancyModel =
      ReconciliationDiscrepancyModel().apply {
        this.taskId = taskId
        this.discrepancyType = DiscrepancyType.EXTRA
        this.sourceSystem = sourceSystem
        this.recordId = recordId
        this.recordDetails = recordDetails
        this.status = DiscrepancyStatus.UNRESOLVED
      }
  }
}
