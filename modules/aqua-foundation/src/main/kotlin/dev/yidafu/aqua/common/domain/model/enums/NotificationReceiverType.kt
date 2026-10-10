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
 * 通知接收人类型(PRD 13-notification.md,站内消息实体)
 */
enum class NotificationReceiverType {
  USER, // 用户端(用户小程序)
  WORKER, // 送水工端(RN,worker_id 为送水工 admin 账号 id)
  ;

  companion object {
    fun fromValue(value: String): NotificationReceiverType =
      when (value) {
        "USER" -> USER
        "WORKER" -> WORKER
        else -> throw IllegalArgumentException("Unknown NotificationReceiverType value: '" + value + "'")
      }
  }
}
