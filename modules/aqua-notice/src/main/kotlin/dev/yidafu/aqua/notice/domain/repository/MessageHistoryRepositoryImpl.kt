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
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.Predicate
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

/**
 * Custom repository implementation for MessageHistory entity using type-safe queries
 */
@Repository
class MessageHistoryRepositoryImpl(
  @PersistenceContext private val entityManager: EntityManager
) : MessageHistoryRepositoryCustom {

  override fun countByUserIdAndStatus(
    userId: Long,
    status: MessageStatus
  ): Long {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(Long::class.java)
    val root = query.from(MessageHistoryModel::class.java)

    // Create count query
    query.select(cb.count(root))

    // Create predicates for userId and status
    val predicates = mutableListOf<Predicate>()

    // userId = :userId predicate
    predicates.add(cb.equal(root.get<Long>("userId"), userId))

    // status = :status predicate
    predicates.add(cb.equal(root.get<MessageStatus>("status"), status))

    // Apply where clause with AND condition
    query.where(*predicates.toTypedArray())

    // Execute count query and return result
    return entityManager.createQuery(query).singleResult
  }

  override fun countByMessageTypeSince(
    messageType: String,
    since: LocalDateTime
  ): Long {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(Long::class.java)
    val root = query.from(MessageHistoryModel::class.java)

    // Create count query
    query.select(cb.count(root))

    // Create predicates for messageType and createdAt >= since
    val predicates = mutableListOf<Predicate>()

    // messageType = :messageType predicate
    predicates.add(cb.equal(root.get<String>("messageType"), messageType))

    // createdAt >= :since predicate
    predicates.add(cb.greaterThanOrEqualTo(root.get<LocalDateTime>("createdAt"), since))

    // Apply where clause with AND condition
    query.where(*predicates.toTypedArray())

    // Execute count query and return result
    return entityManager.createQuery(query).singleResult
  }

  override fun findByStatusAndRetryCountLessThanAndCreatedAtBefore(
    status: MessageStatus,
    retryCount: Int,
    before: LocalDateTime
  ): List<MessageHistoryModel> {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(MessageHistoryModel::class.java)
    val root = query.from(MessageHistoryModel::class.java)

    // Create predicates for status, retryCount, and createdAt
    val predicates = mutableListOf<Predicate>()

    // status = :status predicate
    predicates.add(cb.equal(root.get<MessageStatus>("status"), status))

    // retryCount < :retryCount predicate
    predicates.add(cb.lessThan(root.get<Int>("retryCount"), retryCount))

    // createdAt < :before predicate
    predicates.add(cb.lessThan(root.get<LocalDateTime>("createdAt"), before))

    // Apply where clause with AND condition
    query.where(*predicates.toTypedArray())

    // Order by creation time (oldest first)
    query.orderBy(cb.asc(root.get<LocalDateTime>("createdAt")))

    return entityManager.createQuery(query).resultList
  }

  override fun findByWxMessageId(wxMessageId: String): Optional<MessageHistoryModel> {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(MessageHistoryModel::class.java)
    val root = query.from(MessageHistoryModel::class.java)

    // Create predicate for wxMessageId
    query.where(cb.equal(root.get<String>("wxMessageId"), wxMessageId))

    // Execute query and return optional result
    val results = entityManager.createQuery(query).resultList
    return if (results.isEmpty()) {
      Optional.empty()
    } else {
      Optional.of(results.first())
    }
  }
}