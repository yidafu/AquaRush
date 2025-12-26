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
import dev.yidafu.aqua.common.domain.model.QAddressModel.addressModel
import dev.yidafu.aqua.common.domain.model.QUserModel.userModel
import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.graphql.generated.UserStatus
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class UserRepositoryImpl : UserRepositoryCustom {

  @PersistenceContext
  private lateinit var entityManager: EntityManager

  private val queryFactory: JPAQueryFactory by lazy {
    JPAQueryFactory(entityManager)
  }

  override fun getUserTotalSpent(userId: Long): Double {
    val result = queryFactory.select(userModel.totalSpentCents)
      .from(userModel)
      .where(userModel.id.eq(userId))
      .fetchOne()

    return dev.yidafu.aqua.common.utils.MoneyUtils.fromCents(result ?: 0L).toDouble()
  }

  override fun getUserBalance(userId: Long): Double {
    val result = queryFactory.select(userModel.balanceCents)
      .from(userModel)
      .where(userModel.id.eq(userId))
      .fetchOne()

    return dev.yidafu.aqua.common.utils.MoneyUtils.fromCents(result ?: 0L).toDouble()
  }

  override fun getUserAddressCount(userId: Long): Int {
    return queryFactory.query()
      .from(addressModel)
      .where(addressModel.userId.eq(userId))
      .fetchCount()
      .toInt()
  }

  override fun canUserReview(userId: Long, orderId: Long): Boolean {
    // Simplified check - verify user exists
    // Business logic should be moved to service layer
    return queryFactory.selectFrom(userModel)
      .where(userModel.id.eq(userId))
      .fetchFirst() != null
  }

  override fun findByNicknameContainingIgnoreCaseAndStatusOrPhoneContainingIgnoreCaseAndStatus(
    keyword: String,
    status: UserStatus,
    pageable: Pageable
  ): Page<UserModel> {
    val lowerKeyword = keyword.lowercase()

    // Count query
    val totalCount = queryFactory.query()
      .from(userModel)
      .where(
        userModel.status.eq(status).and(
          userModel.nickname.lower().like("%$lowerKeyword%")
            .or(userModel.phone.lower().like("%$lowerKeyword%"))
        )
      )
      .fetchCount()

    // Main query with pagination
    val results = queryFactory.selectFrom(userModel)
      .where(
        userModel.status.eq(status).and(
          userModel.nickname.lower().like("%$lowerKeyword%")
            .or(userModel.phone.lower().like("%$lowerKeyword%"))
        )
      )
      .orderBy(userModel.id.desc())
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
    status: UserStatus,
    pageable: Pageable
  ): Page<UserModel>
}
