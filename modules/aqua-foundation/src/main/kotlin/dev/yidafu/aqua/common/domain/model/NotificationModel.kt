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

import dev.yidafu.aqua.common.annotation.SnowflakeIdGenerator
import dev.yidafu.aqua.common.domain.model.enums.NotificationChannelStatus
import dev.yidafu.aqua.common.domain.model.enums.NotificationMessageType
import dev.yidafu.aqua.common.domain.model.enums.NotificationReceiverType
import jakarta.persistence.*
import org.hibernate.annotations.SoftDelete
import java.time.LocalDateTime

/**
 * 站内消息统一实体(PRD 13-notification.md F-NTF-002/004/006)
 *
 * 消息来源聚合(订单事件/系统消息)由消息网关统一落库;订阅消息发送记录亦落此表。
 * 幂等键 = 关联单号 + 场景 + 事件时间(R-NTF-02),数据库唯一约束保证去重基准。
 */
@Entity
@SoftDelete(columnName = "is_deleted")
@Table(name = "notifications")
open class NotificationModel(
  @Id
  @SnowflakeIdGenerator
  @Column(name = "id", nullable = false, updatable = false)
  var id: Long? = null,
  @Enumerated(EnumType.STRING)
  @Column(name = "receiver_type", nullable = false, length = 20)
  val receiverType: NotificationReceiverType,
  @Column(name = "user_id")
  val userId: Long? = null,
  @Column(name = "worker_id")
  val workerId: Long? = null,
  @Enumerated(EnumType.STRING)
  @Column(name = "message_type", nullable = false, length = 20)
  val messageType: NotificationMessageType,
  @Column(name = "scene", nullable = false, length = 50)
  val scene: String,
  @Column(name = "title", nullable = false, length = 200)
  val title: String,
  @Column(name = "summary", length = 500)
  val summary: String? = null,
  @Column(name = "related_no", length = 64)
  val relatedNo: String? = null,
  @Column(name = "idempotency_key", nullable = false, unique = true, length = 128)
  val idempotencyKey: String,
  @Enumerated(EnumType.STRING)
  @Column(name = "channel_status", nullable = false, length = 20)
  var channelStatus: NotificationChannelStatus = NotificationChannelStatus.PENDING,
  @Column(name = "wx_message_id", length = 100)
  var wxMessageId: String? = null,
  @Column(name = "error_message", columnDefinition = "TEXT")
  var errorMessage: String? = null,
  @Column(name = "sent_at")
  var sentAt: LocalDateTime? = null,
  @Column(name = "read_at")
  var readAt: LocalDateTime? = null,
  @Column(name = "created_at", nullable = false, updatable = false)
  val createdAt: LocalDateTime = LocalDateTime.now(),
  @Column(name = "updated_at", nullable = false)
  var updatedAt: LocalDateTime = LocalDateTime.now(),
  @Column(name = "deleted_at")
  override var deletedAt: LocalDateTime? = null,
  @Column(name = "deleted_by")
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
