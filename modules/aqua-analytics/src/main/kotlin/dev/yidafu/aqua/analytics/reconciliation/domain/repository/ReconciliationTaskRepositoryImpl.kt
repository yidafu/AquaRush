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

package dev.yidafu.aqua.analytics.reconciliation.domain.repository

import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import dev.yidafu.aqua.common.domain.model.QReconciliationTaskModel.Companion.reconciliationTaskModel
import dev.yidafu.aqua.common.domain.model.ReconciliationTaskModel
import dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskStatus
import dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskType
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * Custom repository implementation for ReconciliationTask entity using QueryDSL
 */
@Repository
class ReconciliationTaskRepositoryImpl : ReconciliationTaskRepositoryCustom {
  @PersistenceContext
  private lateinit var entityManager: EntityManager

  private val queryFactory: JPAQueryFactory by lazy {
    JPAQueryFactory(entityManager)
  }

  override fun findByTaskDateBetween(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): List<ReconciliationTaskModel> =
    queryFactory
      .selectFrom(reconciliationTaskModel)
      .where(reconciliationTaskModel.taskDate.between(startDate, endDate))
      .orderBy(reconciliationTaskModel.taskDate.desc())
      .fetch()

  override fun findByTaskTypeAndTaskDateBetween(
    taskType: ReconciliationTaskType,
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): List<ReconciliationTaskModel> =
    queryFactory
      .selectFrom(reconciliationTaskModel)
      .where(
        reconciliationTaskModel.taskType
          .eq(taskType)
          .and(reconciliationTaskModel.taskDate.between(startDate, endDate)),
      ).orderBy(reconciliationTaskModel.taskDate.desc())
      .fetch()

  override fun countByTaskTypeAndStatus(
    taskType: ReconciliationTaskType,
    status: ReconciliationTaskStatus,
  ): Long =
    queryFactory
      .query()
      .from(reconciliationTaskModel)
      .where(
        reconciliationTaskModel.taskType
          .eq(taskType)
          .and(reconciliationTaskModel.status.eq(status)),
      ).fetchCount()

  override fun findTasksPaged(
    taskType: ReconciliationTaskType?,
    status: ReconciliationTaskStatus?,
    startDate: LocalDateTime?,
    endDate: LocalDateTime?,
    pageable: Pageable,
  ): Page<ReconciliationTaskModel> {
    val builder = BooleanBuilder()
    taskType?.let { builder.and(reconciliationTaskModel.taskType.eq(it)) }
    status?.let { builder.and(reconciliationTaskModel.status.eq(it)) }
    startDate?.let { builder.and(reconciliationTaskModel.taskDate.goe(it)) }
    endDate?.let { builder.and(reconciliationTaskModel.taskDate.loe(it)) }

    val content =
      queryFactory
        .selectFrom(reconciliationTaskModel)
        .where(builder)
        .orderBy(reconciliationTaskModel.taskDate.desc())
        .offset(pageable.offset)
        .limit(pageable.pageSize.toLong())
        .fetch()

    val total =
      queryFactory
        .select(reconciliationTaskModel.count())
        .from(reconciliationTaskModel)
        .where(builder)
        .fetchOne() ?: 0L

    return PageImpl(content, pageable, total)
  }
}
