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

import dev.yidafu.aqua.user.domain.model.Address
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface AddressRepository : JpaRepository<Address, Long>, JpaSpecificationExecutor<Address> {

  fun findByUserId(userId: Long): List<Address>

  fun findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId: Long): List<Address>

  fun findByUserIdAndIsDefault(
    userId: Long,
    isDefault: Boolean,
  ): Address?

  fun findByUserIdAndIdNot(userId: Long, addressId: Long): List<Address>

  @Modifying
  @Query("UPDATE Address a SET a.isDefault = false WHERE a.userId = :userId")
  fun clearDefaultAddresses(@Param("userId") userId: Long)

  @Query(
    value = """
      SELECT * FROM addresses
      WHERE longitude IS NOT NULL AND latitude IS NOT NULL
      AND (
        6371 * acos(
          cos(radians(:latitude)) * cos(radians(latitude)) *
          cos(radians(longitude) - radians(:longitude)) +
          sin(radians(:latitude)) * sin(radians(latitude))
        )
      ) <= :radiusKm
      ORDER BY (
        6371 * acos(
          cos(radians(:latitude)) * cos(radians(latitude)) *
          cos(radians(longitude) - radians(:longitude)) +
          sin(radians(:latitude)) * sin(radians(latitude))
        )
      )
      LIMIT 20
    """,
    nativeQuery = true
  )
  fun findNearby(
    @Param("longitude") longitude: Double,
    @Param("latitude") latitude: Double,
    @Param("radiusKm") radiusKm: Double
  ): List<Address>

  fun deleteByIdAndUserId(
    id: Long,
    userId: Long,
  ): Int {
    val address = findById(id).orElse(null)
    return if (address != null && address.userId == userId) {
      delete(address)
      1
    } else {
      0
    }
  }

  fun clearDefaultByUserId(userId: Long) {
    val addresses = findByUserId(userId)
    addresses.forEach { address ->
      address.isDefault = false
      save(address)
    }
  }

  fun findByUserIdAndIsDefaultTrue(userId: Long): Address?

  @Query(
    value = """
      SELECT a FROM Address a
      WHERE a.userId = :userId
      AND (
        LOWER(a.province) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
        LOWER(a.city) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
        LOWER(a.district) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
        LOWER(a.detailAddress) LIKE LOWER(CONCAT('%', :keyword, '%'))
      )
    """
  )
  fun searchByUserIdAndKeyword(
    @Param("userId") userId: Long,
    @Param("keyword") keyword: String
  ): List<Address>

  fun countByUserId(userId: Long): Int
}