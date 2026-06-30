/**
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
import dev.yidafu.aqua.common.domain.model.ApiLogModel
import dev.yidafu.aqua.common.domain.model.QApiLogModel
import dev.yidafu.aqua.common.domain.model.RequestType
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * QueryDSL implementation for ApiLogRepositoryCustom
 */
@Repository
class ApiLogRepositoryImpl : ApiLogRepositoryCustom {
  @PersistenceContext
  private lateinit var entityManager: EntityManager

  private val queryFactory: JPAQueryFactory by lazy {
    JPAQueryFactory(entityManager)
  }

  private val apiLog = QApiLogModel.apiLogModel

  override fun findByFilters(
    requestType: RequestType?,
    method: String?,
    responseStatus: Int?,
    userId: Long?,
    startTime: LocalDateTime?,
    endTime: LocalDateTime?,
    pageable: Pageable,
  ): Page<ApiLogModel> {
    val builder = BooleanBuilder()

    // Build dynamic conditions
    requestType?.let { builder.and(apiLog.requestType.eq(it)) }
    method?.let { builder.and(apiLog.method.eq(it)) }
    responseStatus?.let { builder.and(apiLog.responseStatus.eq(it)) }
    userId?.let { builder.and(apiLog.userId.eq(it)) }
    startTime?.let { builder.and(apiLog.createdAt.goe(it)) }
    endTime?.let { builder.and(apiLog.createdAt.loe(it)) }

    // Get total count
    val total =
      queryFactory
        .query()
        .from(apiLog)
        .where(builder)
        .fetchCount()

    // Get paginated results with sorting
    val results =
      queryFactory
        .selectFrom(apiLog)
        .where(builder)
        .offset(pageable.offset)
        .limit(pageable.pageSize.toLong())
        .orderBy(apiLog.createdAt.desc())
        .fetch()

    return PageImpl(results, pageable, total)
  }
}
