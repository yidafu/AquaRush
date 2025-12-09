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
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

@Repository
interface AddressRepository : JpaRepository<AddressModel, Long>, JpaSpecificationExecutor<AddressModel>, AddressRepositoryCustom {

  fun findByUserId(userId: Long): List<AddressModel>

  fun findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId: Long): List<AddressModel>

  fun findByUserIdAndIsDefault(
    userId: Long,
    isDefault: Boolean,
  ): AddressModel?

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
