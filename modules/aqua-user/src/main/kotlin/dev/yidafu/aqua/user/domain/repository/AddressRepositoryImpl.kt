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

import com.querydsl.jpa.impl.JPAQueryFactory
import dev.yidafu.aqua.common.domain.model.AddressModel
import dev.yidafu.aqua.common.domain.model.QAddressModel.addressModel
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import kotlin.math.*

/**
 * Custom repository implementation for Address entity using QueryDSL
 */
@Repository
class AddressRepositoryImpl : AddressRepositoryCustom {

  @PersistenceContext
  private lateinit var entityManager: EntityManager

  private val queryFactory: JPAQueryFactory by lazy {
    JPAQueryFactory(entityManager)
  }

  @Transactional
  override fun clearDefaultAddresses(userId: Long): Int {
    return queryFactory.update(addressModel)
      .set(addressModel.isDefault, false)
      .where(addressModel.userId.eq(userId))
      .execute()
      .toInt()
  }

  override fun findNearby(
    longitude: Double,
    latitude: Double,
    radiusKm: Double
  ): List<AddressModel> {
    // Haversine formula for calculating distance
    // Keep native query for geospatial calculations (more efficient)
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
    val lowerKeyword = keyword.lowercase()

    return queryFactory.selectFrom(addressModel)
      .where(
        addressModel.userId.eq(userId).and(
          addressModel.province.lower().like("%$lowerKeyword%")
            .or(addressModel.city.lower().like("%$lowerKeyword%"))
            .or(addressModel.district.lower().like("%$lowerKeyword%"))
            .or(addressModel.detailAddress.lower().like("%$lowerKeyword%"))
        )
      )
      .fetch()
  }
}
