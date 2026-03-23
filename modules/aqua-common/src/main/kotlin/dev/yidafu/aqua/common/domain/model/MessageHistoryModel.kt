/*
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

import dev.yidafu.aqua.common.id.DefaultIdGenerator
import jakarta.persistence.*
import org.hibernate.annotations.SoftDelete
import java.time.LocalDateTime

@Entity
@SoftDelete(columnName = "is_deleted")
@Table(name = "message_history")
open class MessageHistoryModel(
  @Id
  @SnowflakeIdGenerator
  @Column(name = "id", nullable = false, updatable = false)
  var id: Long? = null,
  @Column(name = "user_id", nullable = false)
  val userId: Long,
  @Column(name = "message_type", nullable = false, length = 50)
  val messageType: String,
  @Column(name = "template_id", nullable = false, length = 100)
  val templateId: String,
  @Column(name = "content", columnDefinition = "TEXT")
  val content: String,
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  val status: MessageStatus,
  @Column(name = "sent_at")
  val sentAt: LocalDateTime?,
  @Column(name = "wx_message_id", length = 100)
  val wxMessageId: String?,
  @Column(name = "error_message", columnDefinition = "TEXT")
  val errorMessage: String?,
  @Column(name = "created_at", nullable = false, updatable = false)
  val createdAt: LocalDateTime = LocalDateTime.now(),
  @Column(name = "retry_count", nullable = false)
  val retryCount: Int = 0,
  @Column(name = "updated_at", nullable = false)
  var updatedAt: LocalDateTime = LocalDateTime.now(),
  @Column(name = "deleted_at")
  override var deletedAt: LocalDateTime? = null,
  @Column(name = "deleted_by")
  override var deletedBy: Long? = null,
) : SoftDeletable {
  @PrePersist
  fun onPrePersist() {
    id = DefaultIdGenerator().generate()
  }

  companion object {
    fun createSuccess(
      userId: Long,
      messageType: String,
      templateId: String,
      content: String,
      wxMessageId: String,
    ): MessageHistoryModel =
      MessageHistoryModel(
        userId = userId,
        messageType = messageType,
        templateId = templateId,
        content = content,
        status = MessageStatus.SENT,
        sentAt = LocalDateTime.now(),
        wxMessageId = wxMessageId,
        errorMessage = null,
      )

    fun createFailure(
      userId: Long,
      messageType: String,
      templateId: String,
      content: String,
      errorMessage: String,
      retryCount: Int = 0,
    ): MessageHistoryModel =
      MessageHistoryModel(
        userId = userId,
        messageType = messageType,
        templateId = templateId,
        content = content,
        status = MessageStatus.FAILED,
        sentAt = LocalDateTime.now(),
        wxMessageId = null,
        errorMessage = errorMessage,
        retryCount = retryCount,
      )

    fun createPending(
      userId: Long,
      messageType: String,
      templateId: String,
      content: String,
    ): MessageHistoryModel =
      MessageHistoryModel(
        userId = userId,
        messageType = messageType,
        templateId = templateId,
        content = content,
        status = MessageStatus.PENDING,
        sentAt = LocalDateTime.now(),
        wxMessageId = null,
        errorMessage = null,
      )
  }
}

enum class MessageStatus {
  PENDING,
  SENT,
  FAILED,
  RETRYING,
}
