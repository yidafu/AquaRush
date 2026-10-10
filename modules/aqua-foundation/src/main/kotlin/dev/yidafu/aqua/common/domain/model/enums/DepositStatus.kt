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
 * 桶押金记录状态(R-MON-03 押金桶状态机,B2 回退三态):
 * PENDING(待收取,下单建账)→ HELD(已收取,支付成功实收)→ REFUNDED(已退还);
 * 取消/超时未实收的 PENDING 直接删除,不再保留作废态。
 */
enum class DepositStatus {
  PENDING, // 待收取(随单创建,未支付)
  HELD, // 已收取(在租,计入押金合计)
  REFUNDED, // 已退还(退押审核通过或随单退款)
  ;

  companion object {
    fun fromValue(value: String): DepositStatus =
      when (value) {
        "PENDING" -> PENDING
        "HELD" -> HELD
        "REFUNDED" -> REFUNDED
        else -> throw IllegalArgumentException("Unknown DepositStatus value: '" + value + "'")
      }
  }
}
