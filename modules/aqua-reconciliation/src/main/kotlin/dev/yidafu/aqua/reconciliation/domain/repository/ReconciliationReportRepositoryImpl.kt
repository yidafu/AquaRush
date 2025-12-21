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

import dev.yidafu.aqua.common.domain.model.ReconciliationReportModel
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.Predicate
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * Custom repository implementation for ReconciliationReport entity using type-safe queries
 */
@Repository
class ReconciliationReportRepositoryImpl(
  @PersistenceContext private val entityManager: EntityManager
) : ReconciliationReportRepositoryCustom {

  override fun findByGeneratedAtBetween(startDate: LocalDateTime, endDate: LocalDateTime): List<ReconciliationReportModel> {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(ReconciliationReportModel::class.java)
    val root = query.from(ReconciliationReportModel::class.java)

    // Create predicate for generatedAt BETWEEN startDate AND endDate
    query.where(cb.between(root.get<LocalDateTime>("generatedAt"), startDate, endDate))

    // Order by generatedAt DESC
    query.orderBy(cb.desc(root.get<LocalDateTime>("generatedAt")))

    // Execute query and return results
    return entityManager.createQuery(query).resultList
  }

  override fun deleteReportsBefore(beforeDate: LocalDateTime): Int {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(ReconciliationReportModel::class.java)
    val root = query.from(ReconciliationReportModel::class.java)

    // Create predicate for generatedAt < :beforeDate
    query.where(cb.lessThan(root.get<LocalDateTime>("generatedAt"), beforeDate))

    // Find all reports before the date
    val reportsToDelete = entityManager.createQuery(query).resultList

    // Delete each report individually
    var deletedCount = 0
    reportsToDelete.forEach { report ->
      entityManager.remove(report)
      deletedCount++
    }

    return deletedCount
  }

  override fun countByTaskIdAndReportType(taskId: String, reportType: String): Long {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(Long::class.java)
    val root = query.from(ReconciliationReportModel::class.java)

    // Create count query
    query.select(cb.count(root))

    // Create predicates for taskId and reportType
    val predicates = mutableListOf<Predicate>()

    // taskId = :taskId predicate
    predicates.add(cb.equal(root.get<String>("taskId"), taskId))

    // reportType = :reportType predicate
    predicates.add(cb.equal(root.get<String>("reportType"), reportType))

    // Apply where clause with AND condition
    query.where(*predicates.toTypedArray())

    // Execute count query and return result
    return entityManager.createQuery(query).singleResult
  }
}
