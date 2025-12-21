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

import dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskStatus
import dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskType
import dev.yidafu.aqua.common.id.SnowflakeIdGenerator
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 对账任务实体
 */
import org.hibernate.annotations.SoftDelete

@Entity
@SoftDelete(columnName = "is_deleted")
@Table(name = "reconciliation_tasks")

class ReconciliationTaskModel : SoftDeletable {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: Long? = null

  @Column(name = "task_id", nullable = false, unique = true)
  var taskId: String = ""

  @Enumerated(EnumType.STRING)
  @Column(name = "task_type", nullable = false)
  lateinit var taskType: ReconciliationTaskType

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  var status: ReconciliationTaskStatus = ReconciliationTaskStatus.PENDING

  @Column(name = "task_date", nullable = false)
  var taskDate: LocalDateTime? = null

  @Column(name = "start_time")
  var startTime: LocalDateTime? = null

  @Column(name = "end_time")
  var endTime: LocalDateTime? = null

  @Column(name = "total_records")
  var totalRecords: Int = 0

  @Column(name = "matched_records")
  var matchedRecords: Int = 0

  @Column(name = "unmatched_records")
  var unmatchedRecords: Int = 0

  @Column(name = "error_message")
  var errorMessage: String? = null

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
    fun createPaymentTask(date: LocalDateTime): ReconciliationTaskModel {
      return ReconciliationTaskModel().apply {
        taskId = SnowflakeIdGenerator().generate().toString()
        taskType = ReconciliationTaskType.PAYMENT
        taskDate = date
        status = ReconciliationTaskStatus.PENDING
      }
    }

    fun createRefundTask(date: LocalDateTime): ReconciliationTaskModel {
      return ReconciliationTaskModel().apply {
        taskId = SnowflakeIdGenerator().generate().toString()
        taskType = ReconciliationTaskType.REFUND
        taskDate = date
        status = ReconciliationTaskStatus.PENDING
      }
    }

    fun createSettlementTask(date: LocalDateTime): ReconciliationTaskModel {
      return ReconciliationTaskModel().apply {
        taskId = SnowflakeIdGenerator().generate().toString()
        taskType = ReconciliationTaskType.SETTLEMENT
        taskDate = date
        status = ReconciliationTaskStatus.PENDING
      }
    }
  }
}
