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
import dev.yidafu.aqua.common.domain.model.QAddressModel.Companion.addressModel
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

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
  override fun clearDefaultAddresses(userId: Long): Int =
    queryFactory
      .update(addressModel)
      .set(addressModel.isDefault, false)
      .where(addressModel.userId.eq(userId))
      .execute()
      .toInt()

  override fun findNearby(
    longitude: Double,
    latitude: Double,
    radiusKm: Double,
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

    val query =
      entityManager.createNativeQuery(
        """
        SELECT * FROM addresses a
        WHERE a.longitude IS NOT NULL
          AND a.latitude IS NOT NULL
          AND $haversineFormula <= :radiusKm
        ORDER BY $haversineFormula
        LIMIT 20
      """,
        AddressModel::class.java,
      )

    query.setParameter("longitude", longitude)
    query.setParameter("latitude", latitude)
    query.setParameter("radiusKm", radiusKm)

    @Suppress("UNCHECKED_CAST")
    return query.resultList as List<AddressModel>
  }

  override fun searchByUserIdAndKeyword(
    userId: Long,
    keyword: String,
  ): List<AddressModel> {
    val lowerKeyword = keyword.lowercase()

    return queryFactory
      .selectFrom(addressModel)
      .where(
        addressModel.userId.eq(userId).and(
          addressModel.province
            .lower()
            .like("%$lowerKeyword%")
            .or(addressModel.city.lower().like("%$lowerKeyword%"))
            .or(addressModel.district.lower().like("%$lowerKeyword%"))
            .or(addressModel.detailAddress.lower().like("%$lowerKeyword%")),
        ),
      ).fetch()
  }

  override fun searchByUserIdAndKeyword(
    userId: Long,
    keyword: String,
    pageable: Pageable,
  ): Page<AddressModel> {
    val lowerKeyword = keyword.lowercase()

    // Get total count
    val total =
      queryFactory
        .query()
        .from(addressModel)
        .where(
          addressModel.userId.eq(userId).and(
            addressModel.detailAddress
              .like("%$lowerKeyword%")
              .or(addressModel.phone.like("%$lowerKeyword%"))
              .or(addressModel.receiverName.like("%$lowerKeyword%")),
          ),
        ).fetchCount()

    // Get paginated results
    val results =
      queryFactory
        .selectFrom(addressModel)
        .where(
          addressModel.userId.eq(userId).and(
            addressModel.detailAddress
              .like("%$lowerKeyword%")
              .or(addressModel.phone.like("%$lowerKeyword%"))
              .or(addressModel.receiverName.like("%$lowerKeyword%")),
          ),
        ).offset(pageable.offset)
        .limit(pageable.pageSize.toLong())
        .fetch()

    return PageImpl(results, pageable, total)
  }

  override fun searchAllAddresses(
    keyword: String?,
    pageable: Pageable,
  ): Page<AddressModel> {
    val lowerKeyword = keyword?.lowercase()

    // Build the where condition
    val whereCondition =
      if (lowerKeyword.isNullOrBlank()) {
        null
      } else {
        addressModel.detailAddress
          .like("%$lowerKeyword%")
          .or(addressModel.phone.like("%$lowerKeyword%"))
          .or(addressModel.receiverName.like("%$lowerKeyword%"))
      }

    // Get total count
    val total =
      if (whereCondition == null) {
        queryFactory.query().from(addressModel).fetchCount()
      } else {
        queryFactory
          .query()
          .from(addressModel)
          .where(whereCondition)
          .fetchCount()
      }

    // Get paginated results
    val results =
      if (whereCondition == null) {
        queryFactory
          .selectFrom(addressModel)
          .offset(pageable.offset)
          .limit(pageable.pageSize.toLong())
          .fetch()
      } else {
        queryFactory
          .selectFrom(addressModel)
          .where(whereCondition)
          .offset(pageable.offset)
          .limit(pageable.pageSize.toLong())
          .fetch()
      }

    return PageImpl(results, pageable, total)
  }
}
