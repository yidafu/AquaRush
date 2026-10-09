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

package dev.yidafu.aqua.common.domain.model

import dev.yidafu.aqua.common.domain.model.enums.NotificationChannelStatus
import dev.yidafu.aqua.common.domain.model.enums.NotificationMessageType
import dev.yidafu.aqua.common.domain.model.enums.NotificationReceiverType
import java.time.LocalDateTime

/**
 * 站内消息统一实体(PRD 13-notification.md F-NTF-002/004/006)
 *
 * 消息来源聚合(订单事件/系统消息)由消息网关统一落库;订阅消息发送记录亦落此表。
 * 幂等键 = 关联单号 + 场景 + 事件时间(R-NTF-02),数据库唯一约束保证去重基准。
 */
open class NotificationModel(
  var id: Long? = null,
  val receiverType: NotificationReceiverType,
  val userId: Long? = null,
  val workerId: Long? = null,
  val messageType: NotificationMessageType,
  val scene: String,
  val title: String,
  val summary: String? = null,
  val relatedNo: String? = null,
  val idempotencyKey: String,
  // ===== 微信订阅消息模板上下文(支撑失败消息手动重发,PRD 13-notification.md F-NTF-006) =====
  var templateScene: String? = null,
  var templatePage: String? = null,
  /** 模板字段值 JSON(模板参数名 → 值) */
  var templateFields: String? = null,
  var channelStatus: NotificationChannelStatus = NotificationChannelStatus.PENDING,
  var wxMessageId: String? = null,
  var errorMessage: String? = null,
  var sentAt: LocalDateTime? = null,
  var readAt: LocalDateTime? = null,
  val createdAt: LocalDateTime = LocalDateTime.now(),
  var updatedAt: LocalDateTime = LocalDateTime.now(),
  override var deletedAt: LocalDateTime? = null,
  override var deletedBy: Long? = null,
) : SoftDeletable {
  /** 标记已读(幂等:已读再标记不报错) */
  fun markRead() {
    if (readAt == null) {
      readAt = LocalDateTime.now()
      updatedAt = LocalDateTime.now()
    }
  }

  /** 记录订阅消息发送结果 */
  fun recordChannelResult(
    status: NotificationChannelStatus,
    wxMessageId: String? = null,
    errorMessage: String? = null,
  ) {
    this.channelStatus = status
    this.wxMessageId = wxMessageId
    this.errorMessage = errorMessage
    this.sentAt = LocalDateTime.now()
    this.updatedAt = LocalDateTime.now()
  }
}
