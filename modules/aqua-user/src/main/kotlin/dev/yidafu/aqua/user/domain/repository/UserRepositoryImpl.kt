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

package dev.yidafu.aqua.user.domain.repository

import com.querydsl.jpa.impl.JPAQueryFactory
import dev.yidafu.aqua.user.domain.model.QUserModel
import dev.yidafu.aqua.user.domain.model.UserModel
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class UserRepositoryImpl : UserRepositoryCustom {

  @PersistenceContext
  private lateinit var entityManager: EntityManager

  private val queryFactory: JPAQueryFactory by lazy {
    JPAQueryFactory(entityManager)
  }

  override fun getUserTotalSpent(userId: Long): Double {
    val query = entityManager.createQuery(
      "SELECT COALESCE(u.totalSpent, 0) FROM User u WHERE u.id = :userId",
      java.lang.Double::class.java
    )
    query.setParameter("userId", userId)
    return (query.singleResult as Number).toDouble()
  }

  override fun getUserBalance(userId: Long): Double {
    val query = entityManager.createQuery(
      "SELECT COALESCE(u.balance, 0) FROM User u WHERE u.id = :userId",
      java.lang.Double::class.java
    )
    query.setParameter("userId", userId)
    return (query.singleResult as Number).toDouble()
  }

  override fun getUserAddressCount(userId: Long): Int {
    val query = entityManager.createQuery(
      "SELECT COUNT(a) FROM Address a WHERE a.userId = :userId",
      java.lang.Long::class.java
    )
    query.setParameter("userId", userId)
    return query.singleResult.toInt()
  }

  override fun canUserReview(userId: Long, orderId: Long): Boolean {
    // Check if user has a completed order and hasn't already reviewed it
    val query = entityManager.createQuery(
      """
        SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END
        FROM Order o
        LEFT JOIN Review r ON r.orderId = o.id AND r.userId = :userId
        WHERE o.id = :orderId
        AND o.userId = :userId
        AND o.status = 'COMPLETED'
        AND r.id IS NULL
      """.trimIndent(),
      java.lang.Boolean::class.java
    )
    query.setParameter("userId", userId)
    query.setParameter("orderId", orderId)
    return (query.singleResult as Boolean?) ?: false
  }

  override fun findByNicknameContainingIgnoreCaseAndStatusOrPhoneContainingIgnoreCaseAndStatus(
    keyword: String,
    status: dev.yidafu.aqua.api.dto.UserStatus,
    pageable: Pageable
  ): Page<UserModel> {
    val user = QUserModel.userModel

    // Build the query
    val query = queryFactory
      .selectFrom(user)
      .where(
        user.nickname.containsIgnoreCase(keyword)
          .or(user.phone.contains(keyword))
          .and(user.status.eq(status))
      )
      .orderBy(user.id.desc())

    // Get total count
    val totalCount = queryFactory
      .select(user.count())
      .where(
        user.nickname.containsIgnoreCase(keyword)
          .or(user.phone.contains(keyword))
          .and(user.status.eq(status))
      )
      .fetchOne() ?: 0L

    // Apply pagination
    val results = query
      .offset(pageable.offset)
      .limit(pageable.pageSize.toLong())
      .fetch()

    return PageImpl(results, pageable, totalCount)
  }
}

interface UserRepositoryCustom {
  fun getUserTotalSpent(userId: Long): Double
  fun getUserBalance(userId: Long): Double
  fun getUserAddressCount(userId: Long): Int
  fun canUserReview(userId: Long, orderId: Long): Boolean
  fun findByNicknameContainingIgnoreCaseAndStatusOrPhoneContainingIgnoreCaseAndStatus(
    keyword: String,
    status: dev.yidafu.aqua.api.dto.UserStatus,
    pageable: Pageable
  ): Page<UserModel>
}