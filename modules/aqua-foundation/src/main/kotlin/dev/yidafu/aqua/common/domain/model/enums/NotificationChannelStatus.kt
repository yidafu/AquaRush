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
 * 通知渠道投递状态(微信订阅消息通道结果,PRD 13-notification.md 站内消息实体)
 * 站内消息本身只要落库即对消息中心可见;channel_status 记录订阅消息发送结果。
 */
enum class NotificationChannelStatus {
  PENDING, // 待发送/无微信通道
  SENT, // 订阅消息发送成功
  FAILED, // 订阅消息发送失败
}
