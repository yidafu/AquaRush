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
 * 对账差异DTO
 */
data class ReconciliationDiscrepancyDTO(
  val id: Long?,
  val taskId: String,
  val discrepancyType: DiscrepancyType,
  val sourceSystem: SourceSystem,
  val recordId: String,
  val recordDetails: Map<String, Any>?,
  val status: DiscrepancyStatus,
  val resolutionNotes: String?,
  val resolvedBy: String?,
  val resolvedAt: LocalDateTime?,
  val createdAt: LocalDateTime,
  val updatedAt: LocalDateTime,
)

/**
 * 对账差异类型枚举
 */
enum class DiscrepancyType {
  MISSING,
  MISMATCH,
  EXTRA,
}

/**
 * 源系统枚举
 */
enum class SourceSystem {
  WECHAT_PAY,
  INTERNAL,
  EXTERNAL,
}

/**
 * 差异状态枚举
 */
enum class DiscrepancyStatus {
  UNRESOLVED,
  RESOLVED,
}
