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
// import dev.yidafu.aqua.user.domain.model.QUserModel // TODO: Regenerate QueryDSL Q-classes
import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.graphql.generated.UserStatus
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
      "SELECT COALESCE(u.totalSpent, 0) FROM UserModel u WHERE u.id = :userId",
      java.lang.Double::class.java
    )
    query.setParameter("userId", userId)
    return (query.singleResult as Number).toDouble()
  }

  override fun getUserBalance(userId: Long): Double {
    val query = entityManager.createQuery(
      "SELECT COALESCE(u.balance, 0) FROM UserModel u WHERE u.id = :userId",
      java.lang.Double::class.java
    )
    query.setParameter("userId", userId)
    return (query.singleResult as Number).toDouble()
  }

  override fun getUserAddressCount(userId: Long): Int {
    val query = entityManager.createQuery(
      "SELECT COUNT(a) FROM AddressModel a WHERE a.userId = :userId",
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
        FROM OrderModel o
        LEFT JOIN ReviewModel r ON r.orderId = o.id AND r.userId = :userId
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
    status: UserStatus,
    pageable: Pageable
  ): Page<UserModel> {
    // TODO: Replace with QueryDSL implementation once Q-classes are regenerated
    // For now, use a simple JPA query with pagination filtering in memory
    val query = entityManager.createQuery(
      "SELECT u FROM UserModel u WHERE " +
      "(LOWER(u.nickname) LIKE LOWER(:keyword) OR LOWER(u.phone) LIKE LOWER(:keyword)) " +
      "AND u.status = :status " +
      "ORDER BY u.id DESC",
      UserModel::class.java
    )
    query.setParameter("keyword", "%$keyword%")
    query.setParameter("status", status)

    // Get all results (less efficient but works for now)
    val allResults = query.resultList

    // Apply pagination manually
    val total = allResults.size.toLong()
    val start = pageable.offset.toInt()
    val end = (start + pageable.pageSize).coerceAtMost(allResults.size)
    val pageResults = if (start < allResults.size) allResults.subList(start, end) else emptyList()

    return PageImpl(pageResults, pageable, total)
  }
}

interface UserRepositoryCustom {
  fun getUserTotalSpent(userId: Long): Double
  fun getUserBalance(userId: Long): Double
  fun getUserAddressCount(userId: Long): Int
  fun canUserReview(userId: Long, orderId: Long): Boolean
  fun findByNicknameContainingIgnoreCaseAndStatusOrPhoneContainingIgnoreCaseAndStatus(
    keyword: String,
    status: UserStatus,
    pageable: Pageable
  ): Page<UserModel>
}
