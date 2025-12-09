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

import dev.yidafu.aqua.user.domain.model.AddressModel

/**
 * Custom repository interface for Address entity with QueryDSL implementations
 */
interface AddressRepositoryCustom {
  /**
   * Clear default addresses for a user
   * @param userId the user ID
   * @return number of updated records
   */
  fun clearDefaultAddresses(userId: Long): Int

  /**
   * Find nearby addresses within a given radius
   * @param longitude the longitude coordinate
   * @param latitude the latitude coordinate
   * @param radiusKm the search radius in kilometers
   * @return list of nearby addresses ordered by distance
   */
  fun findNearby(
    longitude: Double,
    latitude: Double,
    radiusKm: Double
  ): List<AddressModel>

  /**
   * Search addresses by user ID and keyword
   * @param userId the user ID
   * @param keyword the search keyword
   * @return list of matching addresses
   */
  fun searchByUserIdAndKeyword(
    userId: Long,
    keyword: String
  ): List<AddressModel>
}