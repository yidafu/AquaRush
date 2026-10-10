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
 * 退押申请状态机(R-PRO-04/F-PRO-010):
 * PENDING(待处理)→ RECOVERED(已回收)→ REFUNDED(已退还)/ REJECTED(已驳回);
 * 退还以押金记录状态幂等(R-MON-03),申请与回收并发以申请状态为准。
 */
enum class DepositRefundApplicationStatus {
  PENDING, // 待处理(用户已提交)
  RECOVERED, // 已回收(空桶回收确认)
  REFUNDED, // 已退还(押金已退)
  REJECTED, // 已驳回(留原因)
  ;

  companion object {
    fun fromValue(value: String): DepositRefundApplicationStatus =
      when (value) {
        "PENDING" -> PENDING
        "RECOVERED" -> RECOVERED
        "REFUNDED" -> REFUNDED
        "REJECTED" -> REJECTED
        else -> throw IllegalArgumentException("Unknown DepositRefundApplicationStatus value: '" + value + "'")
      }
  }
}
