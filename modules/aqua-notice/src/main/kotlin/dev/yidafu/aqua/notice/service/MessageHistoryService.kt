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

package dev.yidafu.aqua.notice.service

import dev.yidafu.aqua.notice.domain.model.MessageHistoryModel
import dev.yidafu.aqua.notice.domain.model.MessageStatus
import dev.yidafu.aqua.notice.domain.repository.MessageHistoryRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@Transactional
class MessageHistoryService(
  private val messageHistoryRepository: MessageHistoryRepository,
  private val redisTemplate: RedisTemplate<String, Any>,
) {
  private val logger = LoggerFactory.getLogger(MessageHistoryService::class.java)
  private val statisticsKeyPrefix = "message_stats:"

  fun save(messageHistory: MessageHistoryModel): MessageHistoryModel {
    logger.debug("Saving message history for user ${messageHistory.userId}")
    return messageHistoryRepository.save(messageHistory)
  }

  fun findByUserId(
    userId: Long,
    pageable: Pageable,
  ): Page<MessageHistoryModel> {
    logger.debug("Finding message history for user $userId")
    return messageHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
  }

  fun updateSuccess(
    messageId: Long,
    wxMessageId: String,
  ) {
    logger.debug("Updating message $messageId as sent with wx_message_id: $wxMessageId")

    val message =
      messageHistoryRepository.findById(messageId)
        .orElseThrow { IllegalArgumentException("Message not found: $messageId") }

    val updatedMessage =
      message.copy(
        status = MessageStatus.SENT,
        sentAt = LocalDateTime.now(),
        wxMessageId = wxMessageId,
      )

    messageHistoryRepository.save(updatedMessage)
    updateStatistics(message.messageType, "success")
  }

  fun updateFailure(
    messageId: Long,
    errorMessage: String,
  ) {
    logger.debug("Updating message $messageId as failed with error: $errorMessage")

    val message =
      messageHistoryRepository.findById(messageId)
        .orElseThrow { IllegalArgumentException("Message not found: $messageId") }

    val updatedMessage =
      message.copy(
        status = MessageStatus.FAILED,
        errorMessage = errorMessage,
      )

    messageHistoryRepository.save(updatedMessage)
    updateStatistics(message.messageType, "failure")
  }

  fun updateToRetrying(messageId: Long) {
    logger.debug("Updating message $messageId to retrying status")

    val message =
      messageHistoryRepository.findById(messageId)
        .orElseThrow { IllegalArgumentException("Message not found: $messageId") }

    val updatedMessage =
      message.copy(
        status = MessageStatus.RETRYING,
      )

    messageHistoryRepository.save(updatedMessage)
  }

  fun incrementRetryCount(messageId: Long) {
    logger.debug("Incrementing retry count for message $messageId")

    val message =
      messageHistoryRepository.findById(messageId)
        .orElseThrow { IllegalArgumentException("Message not found: $messageId") }

    val updatedMessage =
      message.copy(
        retryCount = message.retryCount + 1,
        status = MessageStatus.FAILED,
      )

    messageHistoryRepository.save(updatedMessage)
  }

  fun findFailedMessagesForRetry(): List<MessageHistoryModel> {
    val oneHourAgo = LocalDateTime.now().minusHours(1)
    return messageHistoryRepository.findByStatusAndRetryCountLessThanAndCreatedAtBefore(
      MessageStatus.FAILED,
      3,
      oneHourAgo,
    )
  }

  fun getMessageStatistics(userId: Long): Map<String, Long> {
    val stats = mutableMapOf<String, Long>()

    stats["total"] = messageHistoryRepository.countByUserIdAndStatus(userId, MessageStatus.SENT)
    stats["pending"] = messageHistoryRepository.countByUserIdAndStatus(userId, MessageStatus.PENDING)
    stats["failed"] = messageHistoryRepository.countByUserIdAndStatus(userId, MessageStatus.FAILED)
    stats["retrying"] = messageHistoryRepository.countByUserIdAndStatus(userId, MessageStatus.RETRYING)

    return stats
  }

  fun getStatistics(since: LocalDateTime): Map<String, Long> {
    val cacheKey = "${statisticsKeyPrefix}${since.toLocalDate()}"

    return try {
      val cached = redisTemplate.opsForValue().get(cacheKey) as? Map<String, Long>
      if (cached != null) {
        logger.debug("Returning cached statistics for ${since.toLocalDate()}")
        cached
      } else {
        val stats = computeStatistics(since)
        redisTemplate.opsForValue().set(cacheKey, stats, Duration.ofHours(1))
        logger.debug("Computed and cached statistics for ${since.toLocalDate()}")
        stats
      }
    } catch (e: Exception) {
      logger.warn("Failed to get cached statistics, computing directly", e)
      computeStatistics(since)
    }
  }

  private fun computeStatistics(since: LocalDateTime): Map<String, Long> {
    val stats = mutableMapOf<String, Long>()

    dev.yidafu.aqua.notice.domain.model.MessageType.values().forEach { messageType ->
      val count = messageHistoryRepository.countByMessageTypeSince(messageType.templateId, since)
      stats[messageType.templateId] = count
    }

    return stats
  }

  private fun updateStatistics(
    messageType: String,
    status: String,
  ) {
    try {
      val todayKey = "${statisticsKeyPrefix}${LocalDate.now()}"
      redisTemplate.opsForHash<String, Any>().increment(todayKey, "${messageType}_$status", 1)
    } catch (e: Exception) {
      logger.warn("Failed to update statistics cache", e)
    }
  }

  fun findByWxMessageId(wxMessageId: String): MessageHistoryModel? {
    return messageHistoryRepository.findByWxMessageId(wxMessageId).orElse(null)
  }

  fun getUnreadCount(userId: Long): Int {
    return messageHistoryRepository.countByUserIdAndStatus(userId, MessageStatus.SENT).toInt()
  }

  fun deleteOldMessages(olderThanDays: Int): Int {
    val cutoffDate = LocalDateTime.now().minusDays(olderThanDays.toLong())
    val oldMessages =
      messageHistoryRepository.findAll().filter {
        it.createdAt.isBefore(cutoffDate)
      }

    messageHistoryRepository.deleteAll(oldMessages)
    logger.info("Deleted ${oldMessages.size} old messages older than $olderThanDays days")
    return oldMessages.size
  }
}
