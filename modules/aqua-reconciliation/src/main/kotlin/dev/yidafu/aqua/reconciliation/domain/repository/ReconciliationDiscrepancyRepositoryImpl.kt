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

package dev.yidafu.aqua.reconciliation.domain.repository

import com.querydsl.core.Tuple
import com.querydsl.jpa.impl.JPAQueryFactory
import dev.yidafu.aqua.common.domain.model.QReconciliationDiscrepancyModel.reconciliationDiscrepancyModel
import dev.yidafu.aqua.common.domain.model.ReconciliationDiscrepancyModel
import dev.yidafu.aqua.common.domain.model.enums.DiscrepancyStatus
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * Custom repository implementation for ReconciliationDiscrepancy entity using QueryDSL
 */
@Repository
class ReconciliationDiscrepancyRepositoryImpl : ReconciliationDiscrepancyRepositoryCustom {

  @PersistenceContext
  private lateinit var entityManager: EntityManager

  private val queryFactory: JPAQueryFactory by lazy {
    JPAQueryFactory(entityManager)
  }

  override fun countUnresolvedByTaskId(taskId: String): Long {
    return queryFactory.query()
      .from(reconciliationDiscrepancyModel)
      .where(
        reconciliationDiscrepancyModel.taskId.eq(taskId)
          .and(reconciliationDiscrepancyModel.status.eq(DiscrepancyStatus.UNRESOLVED))
      )
      .fetchCount()
  }

  override fun countByDiscrepancyTypeGroup(taskId: String): List<Array<Any>> {
    val results: List<Tuple> = queryFactory
      .select(reconciliationDiscrepancyModel.discrepancyType, reconciliationDiscrepancyModel.count())
      .from(reconciliationDiscrepancyModel)
      .where(reconciliationDiscrepancyModel.taskId.eq(taskId))
      .groupBy(reconciliationDiscrepancyModel.discrepancyType)
      .fetch()

    return results.map { tuple ->
      arrayOf<Any>(
        tuple.get(reconciliationDiscrepancyModel.discrepancyType) ?: "",
        tuple.get(reconciliationDiscrepancyModel.count()) ?: 0L
      )
    }
  }

  override fun findByCreatedAtBetween(startDate: LocalDateTime, endDate: LocalDateTime): List<ReconciliationDiscrepancyModel> {
    return queryFactory.selectFrom(reconciliationDiscrepancyModel)
      .where(reconciliationDiscrepancyModel.createdAt.between(startDate, endDate))
      .orderBy(reconciliationDiscrepancyModel.createdAt.desc())
      .fetch()
  }

  @Transactional
  override fun deleteResolvedBefore(beforeDate: LocalDateTime): Int {
    return queryFactory
      .delete(reconciliationDiscrepancyModel)
      .where(
        reconciliationDiscrepancyModel.status.eq(DiscrepancyStatus.RESOLVED)
          .and(reconciliationDiscrepancyModel.resolvedAt.lt(beforeDate))
      )
      .execute()
      .toInt()
  }
}
