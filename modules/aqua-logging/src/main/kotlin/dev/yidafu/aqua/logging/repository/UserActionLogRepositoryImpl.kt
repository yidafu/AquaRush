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

package dev.yidafu.aqua.logging.repository

import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import dev.yidafu.aqua.common.domain.model.QUserActionLogModel
import dev.yidafu.aqua.common.domain.model.UserActionLogModel
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * QueryDSL implementation for UserActionLogRepositoryCustom
 */
@Repository
class UserActionLogRepositoryImpl : UserActionLogRepositoryCustom {
  @PersistenceContext
  private lateinit var entityManager: EntityManager

  private val queryFactory: JPAQueryFactory by lazy {
    JPAQueryFactory(entityManager)
  }

  private val userActionLog = QUserActionLogModel.userActionLogModel

  override fun findByFilters(
    userId: String?,
    actionType: String?,
    username: String?,
    startTime: LocalDateTime?,
    endTime: LocalDateTime?,
    pageable: Pageable,
  ): Page<UserActionLogModel> {
    val builder = BooleanBuilder()

    // Build dynamic conditions
    userId?.let { builder.and(userActionLog.userId.eq(it)) }
    actionType?.let { builder.and(userActionLog.actionType.eq(it)) }
    username?.let { builder.and(userActionLog.username.like("%$it%")) }
    startTime?.let { builder.and(userActionLog.createdAt.goe(it)) }
    endTime?.let { builder.and(userActionLog.createdAt.loe(it)) }

    // Get total count
    val total =
      queryFactory
        .query()
        .from(userActionLog)
        .where(builder)
        .fetchCount()

    // Get paginated results with sorting
    val results =
      queryFactory
        .selectFrom(userActionLog)
        .where(builder)
        .offset(pageable.offset)
        .limit(pageable.pageSize.toLong())
        .orderBy(userActionLog.createdAt.desc())
        .fetch()

    return PageImpl(results, pageable, total)
  }
}
