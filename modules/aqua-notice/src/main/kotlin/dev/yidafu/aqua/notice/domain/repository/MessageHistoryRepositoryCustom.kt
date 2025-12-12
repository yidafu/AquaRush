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

package dev.yidafu.aqua.notice.domain.repository

import dev.yidafu.aqua.notice.domain.model.MessageHistoryModel
import dev.yidafu.aqua.notice.domain.model.MessageStatus
import java.time.LocalDateTime
import java.util.*

/**
 * Custom repository interface for MessageHistory entity with QueryDSL implementations
 */
interface MessageHistoryRepositoryCustom {
  /**
   * Count messages by user ID and status
   * @param userId the user ID
   * @param status the message status
   * @return count of matching messages
   */
  fun countByUserIdAndStatus(
    userId: Long,
    status: MessageStatus
  ): Long

  /**
   * Count messages by message type since a specific time
   * @param messageType the message type
   * @param since the start time
   * @return count of matching messages
   */
  fun countByMessageTypeSince(
    messageType: String,
    since: LocalDateTime
  ): Long

  /**
   * Find messages that need to be retried based on status, retry count, and creation time
   * @param status the message status
   * @param retryCount the maximum retry count
   * @param before the creation time cutoff
   * @return list of messages that should be retried
   */
  fun findByStatusAndRetryCountLessThanAndCreatedAtBefore(
    status: MessageStatus,
    retryCount: Int,
    before: LocalDateTime
  ): List<MessageHistoryModel>

  /**
   * Find message by WeChat message ID
   * @param wxMessageId the WeChat message ID
   * @return optional containing the message if found
   */
  fun findByWxMessageId(wxMessageId: String): Optional<MessageHistoryModel>
}