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

import com.querydsl.jpa.impl.JPAQueryFactory
import dev.yidafu.aqua.common.domain.model.MessageHistoryModel
import dev.yidafu.aqua.common.domain.model.MessageStatus
import dev.yidafu.aqua.common.domain.model.QMessageHistoryModel.messageHistoryModel
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

/**
 * Custom repository implementation for MessageHistory entity using QueryDSL
 */
@Repository
class MessageHistoryRepositoryImpl : MessageHistoryRepositoryCustom {

  @PersistenceContext
  private lateinit var entityManager: EntityManager

  private val queryFactory: JPAQueryFactory by lazy {
    JPAQueryFactory(entityManager)
  }

  override fun countByUserIdAndStatus(
    userId: Long,
    status: MessageStatus,
  ): Long {
    return queryFactory.query()
      .from(messageHistoryModel)
      .where(
        messageHistoryModel.userId.eq(userId)
          .and(messageHistoryModel.status.eq(status))
      )
      .fetchCount()
  }

  override fun countByMessageTypeSince(
    messageType: String,
    since: LocalDateTime,
  ): Long {
    return queryFactory.query()
      .from(messageHistoryModel)
      .where(
        messageHistoryModel.messageType.eq(messageType)
          .and(messageHistoryModel.createdAt.goe(since))
      )
      .fetchCount()
  }

  override fun findByStatusAndRetryCountLessThanAndCreatedAtBefore(
    status: MessageStatus,
    retryCount: Int,
    before: LocalDateTime,
  ): List<MessageHistoryModel> {
    return queryFactory.selectFrom(messageHistoryModel)
      .where(
        messageHistoryModel.status.eq(status)
          .and(messageHistoryModel.retryCount.lt(retryCount))
          .and(messageHistoryModel.createdAt.lt(before))
      )
      .orderBy(messageHistoryModel.createdAt.asc())
      .fetch()
  }

  override fun findByWxMessageId(wxMessageId: String): Optional<MessageHistoryModel> {
    val result = queryFactory.selectFrom(messageHistoryModel)
      .where(messageHistoryModel.wxMessageId.eq(wxMessageId))
      .fetchFirst()

    return Optional.ofNullable(result)
  }
}
