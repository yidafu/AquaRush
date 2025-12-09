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

package dev.yidafu.aqua.storage.repository

import dev.yidafu.aqua.storage.domain.entity.FileMetadata
import dev.yidafu.aqua.storage.domain.enums.FileType
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import jakarta.persistence.criteria.Subquery
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * Custom repository implementation for FileMetadata entity using JPA Criteria API
 */
@Repository
class FileMetadataRepositoryImpl(
  @PersistenceContext private val entityManager: EntityManager
) : FileMetadataRepositoryCustom {

  override fun findByCreatedAtBetween(startTime: LocalDateTime, endTime: LocalDateTime): List<FileMetadata> {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(FileMetadata::class.java)
    val root = query.from(FileMetadata::class.java)

    // Create predicate for createdAt BETWEEN startTime AND endTime
    query.where(cb.between(root.get<LocalDateTime>("createdAt"), startTime, endTime))

    // Order by createdAt DESC
    query.orderBy(cb.desc(root.get<LocalDateTime>("createdAt")))

    return entityManager.createQuery(query).resultList
  }

  override fun countByFileType(): Array<Array<Any>> {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(Object::class.java)
    val root = query.from(FileMetadata::class.java)

    // Create multiselect for GROUP BY query: fileType and count
    query.multiselect(
      root.get<FileType>("fileType"),
      cb.count(root)
    )

    // Group by fileType
    query.groupBy(root.get<FileType>("fileType"))

    // Execute query and convert results to Array<Array<Any>>
    val results = entityManager.createQuery(query).resultList
    return results.map { result ->
      when (result) {
        is Array<*> -> result.map { it ?: 0 }.toTypedArray()
        else -> arrayOf(result, 0)
      }
    }.toTypedArray()
  }

  override fun getTotalFileSizeByOwner(ownerId: Long?): Long {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(Long::class.java)
    val root = query.from(FileMetadata::class.java)

    // Create COALESCE(SUM(fileSize), 0) expression
    val sumExpression = cb.sum(root.get<Long>("fileSize"))
    query.select(cb.coalesce(sumExpression, cb.literal(0L)))

    // Create predicate for ownerId = :ownerId (handling null case)
    val predicate = if (ownerId == null) {
      cb.isNull(root.get<Long>("ownerId"))
    } else {
      cb.equal(root.get<Long>("ownerId"), ownerId)
    }
    query.where(predicate)

    return entityManager.createQuery(query).singleResult ?: 0L
  }

  override fun findDuplicateFiles(): List<FileMetadata> {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(FileMetadata::class.java)
    val root = query.from(FileMetadata::class.java)

    // Create subquery to find checksums that appear more than once
    val subquery: Subquery<String> = query.subquery(String::class.java)
    val subRoot = subquery.from(FileMetadata::class.java)

    subquery.select(subRoot.get<String>("checksum"))
    subquery.groupBy(subRoot.get<String>("checksum"))
    subquery.having(cb.greaterThan(cb.count(subRoot), 1L))

    // Main query: find files where checksum is in the subquery result
    query.where(root.get<String>("checksum").`in`(subquery))

    // Order by checksum to group duplicates together
    query.orderBy(cb.asc(root.get<String>("checksum")))

    return entityManager.createQuery(query).resultList
  }

  override fun findByMultipleConditions(
    fileType: FileType?,
    ownerId: Long?,
    isPublic: Boolean?
  ): List<FileMetadata> {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(FileMetadata::class.java)
    val root = query.from(FileMetadata::class.java)

    // Build dynamic predicates list
    val predicates = mutableListOf<Predicate>()

    fileType?.let {
      predicates.add(cb.equal(root.get<FileType>("fileType"), it))
    }

    ownerId?.let {
      predicates.add(cb.equal(root.get<Long>("ownerId"), it))
    }

    isPublic?.let {
      predicates.add(cb.equal(root.get<Boolean>("isPublic"), it))
    }

    // Apply where clause if predicates exist
    if (predicates.isNotEmpty()) {
      query.where(*predicates.toTypedArray())
    }

    // Order by createdAt DESC
    query.orderBy(cb.desc(root.get<LocalDateTime>("createdAt")))

    return entityManager.createQuery(query).resultList
  }

  override fun findByMultipleConditions(
    fileType: FileType?,
    ownerId: Long?,
    isPublic: Boolean?,
    pageable: Pageable
  ): Page<FileMetadata> {
    val cb = entityManager.criteriaBuilder
    val countQuery = cb.createQuery(Long::class.java)
    val rootCount = countQuery.from(FileMetadata::class.java)

    // Build dynamic predicates list for count query
    val countPredicates = mutableListOf<Predicate>()

    fileType?.let {
      countPredicates.add(cb.equal(rootCount.get<FileType>("fileType"), it))
    }

    ownerId?.let {
      countPredicates.add(cb.equal(rootCount.get<Long>("ownerId"), it))
    }

    isPublic?.let {
      countPredicates.add(cb.equal(rootCount.get<Boolean>("isPublic"), it))
    }

    // Apply where clause if predicates exist for count query
    if (countPredicates.isNotEmpty()) {
      countQuery.where(*countPredicates.toTypedArray())
    }

    // Execute count query
    countQuery.select(cb.count(rootCount))
    val totalCount = entityManager.createQuery(countQuery).singleResult

    // Create main query for results
    val query = cb.createQuery(FileMetadata::class.java)
    val root = query.from(FileMetadata::class.java)

    // Rebuild predicates for main query
    val predicates = mutableListOf<Predicate>()

    fileType?.let {
      predicates.add(cb.equal(root.get<FileType>("fileType"), it))
    }

    ownerId?.let {
      predicates.add(cb.equal(root.get<Long>("ownerId"), it))
    }

    isPublic?.let {
      predicates.add(cb.equal(root.get<Boolean>("isPublic"), it))
    }

    // Apply where clause if predicates exist for main query
    if (predicates.isNotEmpty()) {
      query.where(*predicates.toTypedArray())
    }

    // Apply sorting
    val orders = mutableListOf<jakarta.persistence.criteria.Order>()

    if (pageable.sort.isSorted) {
      pageable.sort.forEach { sort ->
        if (sort.isAscending) {
          orders.add(cb.asc(root.get<Any>(sort.property)))
        } else {
          orders.add(cb.desc(root.get<Any>(sort.property)))
        }
      }
    }

    if (orders.isNotEmpty()) {
      query.orderBy(*orders.toTypedArray())
    } else {
      // Default order by createdAt DESC
      query.orderBy(cb.desc(root.get<LocalDateTime>("createdAt")))
    }

    // Apply pagination
    val typedQuery = entityManager.createQuery(query)
    typedQuery.firstResult = pageable.offset.toInt()
    typedQuery.maxResults = pageable.pageSize

    // Execute query and get results
    val results = typedQuery.resultList

    // Return Page object
    return PageImpl(results, pageable, totalCount)
  }
}