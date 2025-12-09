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

import dev.yidafu.aqua.reconciliation.domain.model.ReconciliationDiscrepancy
import dev.yidafu.aqua.reconciliation.domain.model.enums.DiscrepancyStatus
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.CriteriaUpdate
import jakarta.persistence.criteria.Predicate
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * Custom repository implementation for ReconciliationDiscrepancy entity using type-safe queries
 */
@Repository
class ReconciliationDiscrepancyRepositoryImpl(
  @PersistenceContext private val entityManager: EntityManager
) : ReconciliationDiscrepancyRepositoryCustom {

  override fun countUnresolvedByTaskId(taskId: String): Long {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(Long::class.java)
    val root = query.from(ReconciliationDiscrepancy::class.java)

    // Create count query
    query.select(cb.count(root))

    // Create predicates for taskId and status = 'UNRESOLVED'
    val predicates = mutableListOf<Predicate>()

    // taskId = :taskId predicate
    predicates.add(cb.equal(root.get<String>("taskId"), taskId))

    // status = 'UNRESOLVED' predicate
    predicates.add(cb.equal(root.get<DiscrepancyStatus>("status"), DiscrepancyStatus.UNRESOLVED))

    // Apply where clause with AND condition
    query.where(*predicates.toTypedArray())

    // Execute count query and return result
    return entityManager.createQuery(query).singleResult
  }

  override fun countByDiscrepancyTypeGroup(taskId: String): List<Array<Any>> {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery()
    val root = query.from(ReconciliationDiscrepancy::class.java)

    // Create multiselect query for discrepancyType and count
    query.multiselect(root.get<String>("discrepancyType"), cb.count(root))

    // Create predicate for taskId
    query.where(cb.equal(root.get<String>("taskId"), taskId))

    // Group by discrepancyType
    query.groupBy(root.get<String>("discrepancyType"))

    // Execute query and convert results to Array<Any>
    val results = entityManager.createQuery(query).resultList
    return results.map { row ->
      when (row) {
        is Array<*> -> row.map { it ?: "" }.toTypedArray()
        else -> arrayOf(row.toString())
      }
    }
  }

  override fun findByCreatedAtBetween(startDate: LocalDateTime, endDate: LocalDateTime): List<ReconciliationDiscrepancy> {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(ReconciliationDiscrepancy::class.java)
    val root = query.from(ReconciliationDiscrepancy::class.java)

    // Create predicate for createdAt BETWEEN startDate AND endDate
    query.where(cb.between(root.get<LocalDateTime>("createdAt"), startDate, endDate))

    // Order by createdAt DESC
    query.orderBy(cb.desc(root.get<LocalDateTime>("createdAt")))

    // Execute query and return results
    return entityManager.createQuery(query).resultList
  }

  override fun deleteResolvedBefore(beforeDate: LocalDateTime): Int {
    val cb = entityManager.criteriaBuilder
    val update: CriteriaUpdate<ReconciliationDiscrepancy> = cb.createCriteriaUpdate(ReconciliationDiscrepancy::class.java)
    val root = update.from(ReconciliationDiscrepancy::class.java)

    // Create predicates for status = 'RESOLVED' AND resolvedAt < :beforeDate
    val predicates = mutableListOf<Predicate>()

    // status = 'RESOLVED' predicate
    predicates.add(cb.equal(root.get<DiscrepancyStatus>("status"), DiscrepancyStatus.RESOLVED))

    // resolvedAt < :beforeDate predicate
    predicates.add(cb.lessThan(root.get<LocalDateTime>("resolvedAt"), beforeDate))

    // Apply where clause with AND condition
    update.where(*predicates.toTypedArray())

    // For delete operations, we need to use a different approach with EntityManager
    // Create a delete query using JPQL through EntityManager.createNativeQuery
    val deleteQuery = entityManager.createNativeQuery(
      "DELETE FROM reconciliation_discrepancies WHERE status = 'RESOLVED' AND resolved_at < :beforeDate"
    )
    deleteQuery.setParameter("beforeDate", beforeDate)

    return deleteQuery.executeUpdate()
  }
}