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
 * 周期订购计划状态(R-ORD-13,F-ORD-026):
 * 生效中可暂停/恢复/终止;已暂停可恢复/终止;已终止不可恢复(需新建计划)。
 */
enum class SubscriptionStatus {
  ACTIVE, // 生效中
  PAUSED, // 已暂停(期间不生成订单)
  TERMINATED, // 已终止(不可恢复)
  ;

  companion object {
    fun fromValue(value: String): SubscriptionStatus =
      when (value) {
        "ACTIVE" -> ACTIVE
        "PAUSED" -> PAUSED
        "TERMINATED" -> TERMINATED
        else -> throw IllegalArgumentException("Unknown SubscriptionStatus value: '" + value + "'")
      }
  }
}
