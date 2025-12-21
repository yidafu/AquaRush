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
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

@Repository
interface AddressRepository : JpaRepository<AddressModel, Long>, JpaSpecificationExecutor<AddressModel>, AddressRepositoryCustom {

  /**
   * Find all addresses for a user
   * @param userId the user ID
   * @return list of addresses
   */
  fun findByUserId(userId: Long): List<AddressModel>

  /**
   * Find all addresses for a user ordered by default status and creation time
   * @param userId the user ID
   * @return list of addresses ordered by default status and creation time
   */
  fun findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId: Long): List<AddressModel>

  /**
   * Find all addresses for a user ordered by ID descending
   * @param userId the user ID
   * @return list of addresses ordered by ID descending
   */
  fun findByUserIdOrderByIdDesc(userId: Long): List<AddressModel>

  /**
   * Find address by user ID and default status
   * @param userId the user ID
   * @param isDefault the default status
   * @return address that matches the criteria, or null if not found
   */
  fun findByUserIdAndIsDefault(
    userId: Long,
    isDefault: Boolean,
  ): AddressModel?

  /**
   * Find addresses by user ID excluding a specific address ID
   * @param userId the user ID
   * @param addressId the address ID to exclude
   * @return list of addresses
   */
  fun findByUserIdAndIdNot(userId: Long, addressId: Long): List<AddressModel>

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

  fun findByUserIdAndIsDefaultTrue(userId: Long): AddressModel?

  fun countByUserId(userId: Long): Int
}
