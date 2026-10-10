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
 * 配送时段(AC-ORD-001-05 配送时间随单落库):
 * 用户端确认订单页可选配送时段,下单时快照落库(orders.delivery_time_slot),
 * 与 shared-config DeliveryTimeSlot 枚举一一对应;代客下单/后台不设置(列可空)。
 */
enum class DeliveryTimeSlot {
  IMMEDIATE, // 立即配送(30分钟内)
  MORNING, // 上午配送(9:00-12:00)
  AFTERNOON, // 下午配送(14:00-18:00)
  EVENING, // 晚上配送(18:00-21:00)
  ;

  companion object {
    fun fromValue(value: String): DeliveryTimeSlot =
      when (value) {
        "IMMEDIATE" -> IMMEDIATE
        "MORNING" -> MORNING
        "AFTERNOON" -> AFTERNOON
        "EVENING" -> EVENING
        else -> throw IllegalArgumentException("Unknown DeliveryTimeSlot value: '" + value + "'")
      }
  }
}
