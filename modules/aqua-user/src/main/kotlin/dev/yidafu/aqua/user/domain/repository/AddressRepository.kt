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
import org.springframework.stereotype.Repository

@Repository
interface AddressRepository : JpaRepository<Address, Long>, JpaSpecificationExecutor<Address> {
  fun findByUserId(userId: Long): List<Address>

  fun findByUserIdAndIsDefault(
    userId: Long,
    isDefault: Boolean,
  ): Address?

  fun clearDefaultByUserId(userId: Long) {
    val addresses = findByUserId(userId)
    addresses.forEach { address ->
      address.isDefault = false
      save(address)
    }
  }

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
}
