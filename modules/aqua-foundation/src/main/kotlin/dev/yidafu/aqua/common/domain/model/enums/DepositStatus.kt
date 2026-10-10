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

package dev.yidafu.aqua.common.domain.model.enums

/**
 * 桶押金记录状态(G06/R-MON-03:押金随单收取,退还闭环)
 */
enum class DepositStatus {
  COLLECTED, // 已收取(收款到账即收取)
  REFUNDED, // 已退还(订单取消自动退还或管理员退还)
  CANCELLED, // 已取消(未实际收取,记录作废)
  ;

  companion object {
    fun fromValue(value: String): DepositStatus =
      when (value) {
        "COLLECTED" -> COLLECTED
        "REFUNDED" -> REFUNDED
        "CANCELLED" -> CANCELLED
        else -> throw IllegalArgumentException("Unknown DepositStatus value: '" + value + "'")
      }
  }
}
