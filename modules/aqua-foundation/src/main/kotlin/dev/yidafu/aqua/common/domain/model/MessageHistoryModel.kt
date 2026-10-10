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

import java.time.LocalDateTime

open class MessageHistoryModel(
  var id: Long? = null,
  val userId: Long,
  val messageType: String,
  val templateId: String,
  val content: String,
  val status: MessageStatus,
  val sentAt: LocalDateTime?,
  val wxMessageId: String?,
  val errorMessage: String?,
  val createdAt: LocalDateTime = LocalDateTime.now(),
  val retryCount: Int = 0,
  var updatedAt: LocalDateTime = LocalDateTime.now(),
  override var deletedAt: LocalDateTime? = null,
  override var deletedBy: Long? = null,
) : SoftDeletable {
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
