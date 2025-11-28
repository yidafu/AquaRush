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

import dev.yidafu.aqua.notice.domain.model.MessageHistory
import dev.yidafu.aqua.notice.domain.model.MessageStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

@Repository
interface MessageHistoryRepository : JpaRepository<MessageHistory, Long> {
    fun findByUserIdOrderByCreatedAtDesc(userId: Long, pageable: Pageable): Page<MessageHistory>
    fun findByUserIdAndStatus(userId: Long, status: MessageStatus): List<MessageHistory>
    fun findByMessageTypeAndStatus(messageType: String, status: MessageStatus): List<MessageHistory>

    @Query("SELECT COUNT(m) FROM MessageHistory m WHERE m.userId = :userId AND m.status = :status")
    fun countByUserIdAndStatus(@Param("userId") userId: Long, @Param("status") status: MessageStatus): Long

    @Query("SELECT COUNT(m) FROM MessageHistory m WHERE m.messageType = :messageType AND m.createdAt >= :since")
    fun countByMessageTypeSince(@Param("messageType") messageType: String, @Param("since") since: LocalDateTime): Long

    fun findByStatusAndRetryCountLessThanAndCreatedAtBefore(
        status: MessageStatus,
        retryCount: Int,
        before: LocalDateTime
    ): List<MessageHistory>

    fun findByWxMessageId(wxMessageId: String): Optional<MessageHistory>
}
