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

package dev.yidafu.aqua.api.dto

import java.time.LocalDateTime

/**
 * 对账任务DTO
 */
data class ReconciliationTaskDTO(
  val id: Long?,
  val taskId: String,
  val taskType: TaskType,
  val status: TaskStatus,
  val taskDate: LocalDateTime?,
  val startTime: LocalDateTime?,
  val endTime: LocalDateTime?,
  val totalRecords: Int,
  val matchedRecords: Int,
  val unmatchedRecords: Int,
  val errorMessage: String?,
  val createdAt: LocalDateTime,
  val updatedAt: LocalDateTime,
)

/**
 * 任务类型枚举
 */
enum class TaskType {
  PAYMENT,
  REFUND,
  SETTLEMENT,
}

/**
 * 任务状态枚举
 */
enum class TaskStatus {
  PENDING,
  RUNNING,
  COMPLETED,
  FAILED,
  CANCELLED,
}
