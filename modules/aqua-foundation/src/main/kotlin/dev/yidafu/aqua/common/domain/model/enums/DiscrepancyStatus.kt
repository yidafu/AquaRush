/**
 * AquaRush
 *
 * Copyright (C) 2026 AquaRush Team
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
package dev.yidafu.aqua.common.domain.model.enums

/**
 * 对账差异状态(IGNORED:忽略——跨日边界等可解释差异,R-PAY-04/F-ORD-031;已处置状态不可再改)
 */
enum class DiscrepancyStatus {
  UNRESOLVED, // 未解决
  RESOLVED, // 已解决
  IGNORED, // 已忽略
  ;

  companion object {
    fun fromValue(value: String): DiscrepancyStatus =
      when (value) {
        "UNRESOLVED" -> UNRESOLVED
        "RESOLVED" -> RESOLVED
        "IGNORED" -> IGNORED
        else -> throw IllegalArgumentException("Unknown DiscrepancyStatus value: '" + value + "'")
      }
  }
}
