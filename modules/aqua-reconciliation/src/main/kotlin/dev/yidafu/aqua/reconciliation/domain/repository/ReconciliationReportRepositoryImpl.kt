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

import com.querydsl.jpa.impl.JPAQueryFactory
import dev.yidafu.aqua.common.domain.model.QReconciliationReportModel.reconciliationReportModel
import dev.yidafu.aqua.common.domain.model.ReconciliationReportModel
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * Custom repository implementation for ReconciliationReport entity using QueryDSL
 */
@Repository
class ReconciliationReportRepositoryImpl : ReconciliationReportRepositoryCustom {

  @PersistenceContext
  private lateinit var entityManager: EntityManager

  private val queryFactory: JPAQueryFactory by lazy {
    JPAQueryFactory(entityManager)
  }

  override fun findByGeneratedAtBetween(startDate: LocalDateTime, endDate: LocalDateTime): List<ReconciliationReportModel> {
    return queryFactory.selectFrom(reconciliationReportModel)
      .where(reconciliationReportModel.generatedAt.between(startDate, endDate))
      .orderBy(reconciliationReportModel.generatedAt.desc())
      .fetch()
  }

  @Transactional
  override fun deleteReportsBefore(beforeDate: LocalDateTime): Int {
    // Use bulk delete for better performance
    return queryFactory.delete(reconciliationReportModel)
      .where(reconciliationReportModel.generatedAt.lt(beforeDate))
      .execute()
      .toInt()
  }

  override fun countByTaskIdAndReportType(taskId: String, reportType: String): Long {
    return queryFactory.query()
      .from(reconciliationReportModel)
      .where(
        reconciliationReportModel.taskId.eq(taskId)
          .and(reconciliationReportModel.reportType.eq(reportType))
      )
      .fetchCount()
  }
}
