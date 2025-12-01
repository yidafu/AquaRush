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

package dev.yidafu.aqua.reconciliation.domain.model

import dev.yidafu.aqua.common.id.SnowflakeIdGenerator
import dev.yidafu.aqua.reconciliation.domain.model.enums.TaskStatus
import dev.yidafu.aqua.reconciliation.domain.model.enums.TaskType
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 对账任务实体
 */
@Entity
@Table(name = "reconciliation_tasks")
class ReconciliationTask {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: Long? = null

  @Column(name = "task_id", nullable = false, unique = true)
  var taskId: String = ""

  @Enumerated(EnumType.STRING)
  @Column(name = "task_type", nullable = false)
  lateinit var taskType: TaskType

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  var status: TaskStatus = TaskStatus.PENDING

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

  @PreUpdate
  fun preUpdate() {
    updatedAt = LocalDateTime.now()
  }

  companion object {
    fun createPaymentTask(date: LocalDateTime): ReconciliationTask {
      return ReconciliationTask().apply {
        taskId = SnowflakeIdGenerator().generate().toString()
        taskType = TaskType.PAYMENT
        taskDate = date
        status = TaskStatus.PENDING
      }
    }

    fun createRefundTask(date: LocalDateTime): ReconciliationTask {
      return ReconciliationTask().apply {
        taskId = SnowflakeIdGenerator().generate().toString()
        taskType = TaskType.REFUND
        taskDate = date
        status = TaskStatus.PENDING
      }
    }

    fun createSettlementTask(date: LocalDateTime): ReconciliationTask {
      return ReconciliationTask().apply {
        taskId = SnowflakeIdGenerator().generate().toString()
        taskType = TaskType.SETTLEMENT
        taskDate = date
        status = TaskStatus.PENDING
      }
    }
  }
}
