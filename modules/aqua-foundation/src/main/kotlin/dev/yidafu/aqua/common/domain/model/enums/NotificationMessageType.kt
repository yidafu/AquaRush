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
 * 站内消息分类(PRD 13-notification.md F-NTF-002/004:消息中心 Tab 维度)
 */
enum class NotificationMessageType {
  ORDER, // 订单动态
  SYSTEM, // 系统消息
  ANNOUNCEMENT, // 公告(聚合开放问题,枚举预留)
  FEEDBACK, // 意见反馈回执(聚合开放问题,枚举预留)
  ;

  companion object {
    fun fromValue(value: String): NotificationMessageType =
      when (value) {
        "ORDER" -> ORDER
        "SYSTEM" -> SYSTEM
        "ANNOUNCEMENT" -> ANNOUNCEMENT
        "FEEDBACK" -> FEEDBACK
        else -> throw IllegalArgumentException("Unknown NotificationMessageType value: '" + value + "'")
      }
  }
}
