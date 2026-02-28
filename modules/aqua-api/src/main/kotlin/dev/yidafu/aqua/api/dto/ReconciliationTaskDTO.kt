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

import dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskStatus
import dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskType
import java.time.LocalDateTime

/**
 * 对账任务DTO
 */
data class ReconciliationTaskDTO(
  val id: Long?,
  val taskId: String,
  val taskType: ReconciliationTaskType,
  val status: ReconciliationTaskStatus,
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
