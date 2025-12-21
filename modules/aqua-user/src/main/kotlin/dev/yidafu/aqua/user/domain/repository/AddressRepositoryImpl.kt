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

import dev.yidafu.aqua.common.domain.model.AddressModel
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository
import kotlin.math.*

/**
 * Custom repository implementation for Address entity using type-safe queries
 */
@Repository
class AddressRepositoryImpl(
  @PersistenceContext private val entityManager: EntityManager
) : AddressRepositoryCustom {

  override fun clearDefaultAddresses(userId: Long): Int {
    val cb = entityManager.criteriaBuilder
    val update = cb.createCriteriaUpdate(AddressModel::class.java)
    val root = update.from(AddressModel::class.java)

    // Set isDefault to false
    update.set("isDefault", false)

    // Where userId matches
    update.where(cb.equal(root.get<Long>("userId"), userId))

    return entityManager.createQuery(update).executeUpdate()
  }

  override fun findNearby(
    longitude: Double,
    latitude: Double,
    radiusKm: Double
  ): List<AddressModel> {
    // Haversine formula for calculating distance
    val haversineFormula = """
      (6371 * acos(
        cos(radians(:latitude)) * cos(radians(a.latitude)) *
        cos(radians(a.longitude) - radians(:longitude)) +
        sin(radians(:latitude)) * sin(radians(a.latitude))
      ))
    """

    val query = entityManager.createNativeQuery(
      """
        SELECT * FROM addresses a
        WHERE a.longitude IS NOT NULL
          AND a.latitude IS NOT NULL
          AND $haversineFormula <= :radiusKm
        ORDER BY $haversineFormula
        LIMIT 20
      """,
      AddressModel::class.java
    )

    query.setParameter("longitude", longitude)
    query.setParameter("latitude", latitude)
    query.setParameter("radiusKm", radiusKm)

    @Suppress("UNCHECKED_CAST")
    return query.resultList as List<AddressModel>
  }

  override fun searchByUserIdAndKeyword(
    userId: Long,
    keyword: String
  ): List<AddressModel> {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(AddressModel::class.java)
    val root = query.from(AddressModel::class.java)

    // Create predicates for search
    val predicates = mutableListOf(
      cb.equal(root.get<Long>("userId"), userId),
      cb.like(
        cb.lower(root.get("province")),
        cb.lower(cb.literal("%$keyword%"))
      ),
      cb.like(
        cb.lower(root.get("city")),
        cb.lower(cb.literal("%$keyword%"))
      ),
      cb.like(
        cb.lower(root.get("district")),
        cb.lower(cb.literal("%$keyword%"))
      ),
      cb.like(
        cb.lower(root.get("detailAddress")),
        cb.lower(cb.literal("%$keyword%"))
      )
    )

    // Combine keyword search with OR
    val keywordPredicate = cb.or(
      predicates[1], predicates[2], predicates[3], predicates[4]
    )

    // Final where clause: userId AND (province OR city OR district OR detailAddress contains keyword)
    query.where(
      predicates[0], // userId = :userId
      keywordPredicate
    )

    return entityManager.createQuery(query).resultList
  }
}
