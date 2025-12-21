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

package dev.yidafu.aqua.notice.service.impl

import dev.yidafu.aqua.api.service.MessageHistoryService
import dev.yidafu.aqua.common.domain.model.MessageHistoryModel
import dev.yidafu.aqua.common.domain.model.MessageStatus
import dev.yidafu.aqua.notice.domain.repository.MessageHistoryRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class MessageHistoryServiceImpl(
  private val messageHistoryRepository: MessageHistoryRepository,
) : MessageHistoryService {
  override fun save(messageHistory: MessageHistoryModel): MessageHistoryModel {
    return messageHistoryRepository.save(messageHistory)
  }

  override fun findByUserId(
    userId: Long,
    pageable: Pageable,
  ): Page<MessageHistoryModel> {
    return messageHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
  }

  override fun updateSuccess(
    messageId: Long,
    wxMessageId: String,
  ) {
    messageHistoryRepository.findById(messageId).ifPresent { message ->
      // Since MessageHistoryModel is immutable, we need to delete the old record and create a new one
      // or use a different approach. For now, let's handle this as a placeholder
      // In a real implementation, you might want to add update methods to the entity
      messageHistoryRepository.deleteById(messageId)

      val updatedMessage =
        MessageHistoryModel(
          id = message.id,
          userId = message.userId,
          messageType = message.messageType,
          templateId = message.templateId,
          content = message.content,
          status = MessageStatus.SENT,
          sentAt = LocalDateTime.now(),
          wxMessageId = wxMessageId,
          errorMessage = null,
          createdAt = message.createdAt,
          retryCount = message.retryCount,
          updatedAt = LocalDateTime.now(),
          isDeleted = false,
        )
      messageHistoryRepository.save(updatedMessage)
    }
  }

  override fun updateFailure(
    messageId: Long,
    errorMessage: String,
  ) {
    messageHistoryRepository.findById(messageId).ifPresent { message ->
      messageHistoryRepository.deleteById(messageId)

      val updatedMessage =
        MessageHistoryModel(
          id = message.id,
          userId = message.userId,
          messageType = message.messageType,
          templateId = message.templateId,
          content = message.content,
          status = MessageStatus.FAILED,
          sentAt = LocalDateTime.now(),
          wxMessageId = null,
          errorMessage = errorMessage,
          createdAt = message.createdAt,
          retryCount = message.retryCount,
          updatedAt = LocalDateTime.now(),
          isDeleted = false,
        )
      messageHistoryRepository.save(updatedMessage)
    }
  }

  override fun updateToRetrying(messageId: Long) {
    messageHistoryRepository.findById(messageId).ifPresent { message ->
      messageHistoryRepository.deleteById(messageId)

      val updatedMessage =
        MessageHistoryModel(
          id = message.id,
          userId = message.userId,
          messageType = message.messageType,
          templateId = message.templateId,
          content = message.content,
          status = MessageStatus.RETRYING,
          sentAt = message.sentAt,
          wxMessageId = message.wxMessageId,
          errorMessage = message.errorMessage,
          createdAt = message.createdAt,
          retryCount = message.retryCount,
          updatedAt = LocalDateTime.now(),
          isDeleted = false,
        )
      messageHistoryRepository.save(updatedMessage)
    }
  }

  override fun incrementRetryCount(messageId: Long) {
    messageHistoryRepository.findById(messageId).ifPresent { message ->
      messageHistoryRepository.deleteById(messageId)

      val updatedMessage =
        MessageHistoryModel(
          id = message.id,
          userId = message.userId,
          messageType = message.messageType,
          templateId = message.templateId,
          content = message.content,
          status = message.status,
          sentAt = message.sentAt,
          wxMessageId = message.wxMessageId,
          errorMessage = message.errorMessage,
          createdAt = message.createdAt,
          retryCount = message.retryCount + 1,
          updatedAt = LocalDateTime.now(),
          isDeleted = false,
        )
      messageHistoryRepository.save(updatedMessage)
    }
  }

  override fun findFailedMessagesForRetry(): List<MessageHistoryModel> {
    val cutoffTime = LocalDateTime.now().minusMinutes(5) // 5 minutes ago
    return messageHistoryRepository.findByStatusAndRetryCountLessThanAndCreatedAtBefore(
      MessageStatus.FAILED,
      3,
      cutoffTime,
    )
  }

  override fun getMessageStatistics(userId: Long): Map<String, Long> {
    val pendingCount = messageHistoryRepository.countByUserIdAndStatus(userId, MessageStatus.PENDING)
    val sentCount = messageHistoryRepository.countByUserIdAndStatus(userId, MessageStatus.SENT)
    val failedCount = messageHistoryRepository.countByUserIdAndStatus(userId, MessageStatus.FAILED)

    return mapOf(
      "pending" to pendingCount,
      "sent" to sentCount,
      "failed" to failedCount,
    )
  }

  override fun getStatistics(since: LocalDateTime): Map<String, Long> {
    val sentCount = messageHistoryRepository.countByMessageTypeSince("order", since)

    return mapOf(
      "total_sent" to sentCount,
    )
  }

  override fun findByWxMessageId(wxMessageId: String): MessageHistoryModel? {
    return messageHistoryRepository.findByWxMessageId(wxMessageId).orElse(null)
  }

  override fun getUnreadCount(userId: Long): Int {
    // Assuming all messages are considered "read" in this context
    // This would need to be implemented based on actual requirements
    return 0
  }

  override fun deleteOldMessages(olderThanDays: Int): Int {
    val cutoffDate = LocalDateTime.now().minusDays(olderThanDays.toLong())
    // This would require a custom query implementation
    // For now, return 0 as a placeholder
    return 0
  }
}
