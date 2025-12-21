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

import dev.yidafu.aqua.common.domain.model.RegionModel
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.Predicate
import org.springframework.stereotype.Repository

/**
 * Custom repository implementation for Region entity using type-safe queries
 */
@Repository
class RegionRepositoryImpl(
  @PersistenceContext private val entityManager: EntityManager
) : RegionRepositoryCustom {

  override fun findRootRegions(level: Int): List<RegionModel> {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(RegionModel::class.java)
    val root = query.from(RegionModel::class.java)

    // Create predicates for parentCode = '0' AND level = :level
    val predicates = mutableListOf<Predicate>()

    // parentCode = '0' predicate
    predicates.add(cb.equal(root.get<String>("parentCode"), "0"))

    // level = :level predicate
    predicates.add(cb.equal(root.get<Int>("level"), level))

    // Apply where clause with AND condition
    query.where(*predicates.toTypedArray())

    return entityManager.createQuery(query).resultList
  }

  override fun existsByNameAndLevelAndParentCode(
    name: String,
    level: Int,
    parentCode: String
  ): Boolean {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(Long::class.java)
    val root = query.from(RegionModel::class.java)

    // Create count query
    query.select(cb.count(root))

    // Create predicates for name, level, and parentCode
    val predicates = mutableListOf<Predicate>()

    // name = :name predicate
    predicates.add(cb.equal(root.get<String>("name"), name))

    // level = :level predicate
    predicates.add(cb.equal(root.get<Int>("level"), level))

    // parentCode = :parentCode predicate
    predicates.add(cb.equal(root.get<String>("parentCode"), parentCode))

    // Apply where clause with AND condition
    query.where(*predicates.toTypedArray())

    // Execute count query and return if count > 0
    val count = entityManager.createQuery(query).singleResult
    return count > 0
  }
}