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

import dev.yidafu.aqua.reconciliation.domain.model.ReconciliationTask
import dev.yidafu.aqua.reconciliation.domain.model.enums.TaskStatus
import dev.yidafu.aqua.reconciliation.domain.model.enums.TaskType
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.Predicate
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * Custom repository implementation for ReconciliationTask entity using type-safe queries
 */
@Repository
class ReconciliationTaskRepositoryImpl(
  @PersistenceContext private val entityManager: EntityManager
) : ReconciliationTaskRepositoryCustom {

  override fun findByTaskDateBetween(startDate: LocalDateTime, endDate: LocalDateTime): List<ReconciliationTask> {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(ReconciliationTask::class.java)
    val root = query.from(ReconciliationTask::class.java)

    // Create predicate for taskDate BETWEEN startDate AND endDate
    query.where(cb.between(root.get<LocalDateTime>("taskDate"), startDate, endDate))

    // Order by taskDate DESC
    query.orderBy(cb.desc(root.get<LocalDateTime>("taskDate")))

    // Execute query and return results
    return entityManager.createQuery(query).resultList
  }

  override fun findByTaskTypeAndTaskDateBetween(
    taskType: TaskType,
    startDate: LocalDateTime,
    endDate: LocalDateTime
  ): List<ReconciliationTask> {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(ReconciliationTask::class.java)
    val root = query.from(ReconciliationTask::class.java)

    // Create predicates for taskType AND taskDate BETWEEN startDate AND endDate
    val predicates = mutableListOf<Predicate>()

    // taskType = :taskType predicate
    predicates.add(cb.equal(root.get<TaskType>("taskType"), taskType))

    // taskDate BETWEEN startDate AND endDate predicate
    predicates.add(cb.between(root.get<LocalDateTime>("taskDate"), startDate, endDate))

    // Apply where clause with AND condition
    query.where(*predicates.toTypedArray())

    // Order by taskDate DESC
    query.orderBy(cb.desc(root.get<LocalDateTime>("taskDate")))

    // Execute query and return results
    return entityManager.createQuery(query).resultList
  }

  override fun countByTaskTypeAndStatus(taskType: TaskType, status: TaskStatus): Long {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(Long::class.java)
    val root = query.from(ReconciliationTask::class.java)

    // Create count query
    query.select(cb.count(root))

    // Create predicates for taskType and status
    val predicates = mutableListOf<Predicate>()

    // taskType = :taskType predicate
    predicates.add(cb.equal(root.get<TaskType>("taskType"), taskType))

    // status = :status predicate
    predicates.add(cb.equal(root.get<TaskStatus>("status"), status))

    // Apply where clause with AND condition
    query.where(*predicates.toTypedArray())

    // Execute count query and return result
    return entityManager.createQuery(query).singleResult
  }
}