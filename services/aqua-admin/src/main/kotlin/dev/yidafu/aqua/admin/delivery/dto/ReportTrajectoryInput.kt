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

package dev.yidafu.aqua.admin.delivery.dto

import java.time.LocalDateTime

/**
 * 轨迹上报输入类型(F-ADR-012,R-ADR-03;与 admin-inputs.graphqls ReportTrajectoryInput 一致)
 */
data class ReportTrajectoryInput(
  val orderId: Long,
  val longitude: Float,
  val latitude: Float,
  val accuracy: Float? = null,
  val reportedAt: LocalDateTime? = null,
)
