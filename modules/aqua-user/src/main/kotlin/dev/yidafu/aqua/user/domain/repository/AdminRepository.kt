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

import dev.yidafu.aqua.user.domain.model.Admin
import dev.yidafu.aqua.user.domain.model.AdminRole
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface AdminRepository : JpaRepository<Admin, Long>, JpaSpecificationExecutor<Admin> {
  fun findByUsername(username: String): Optional<Admin>

  fun findByUsernameAndRole(
    username: String,
    role: AdminRole,
  ): Optional<Admin>

  fun findByPhone(phone: String): Optional<Admin>

  fun findByRole(role: AdminRole): List<Admin>

  fun countByRole(
    role: AdminRole,
  ): Long {
    val specification = AdminSpecifications.byRole(role)
    return count(specification)
  }

  fun existsByUsername(username: String): Boolean {
    val specification = AdminSpecifications.byUsername(username)
    return count(specification) > 0
  }

  fun existsByPhone(phone: String): Boolean {
    val specification = AdminSpecifications.byPhone(phone)
    return count(specification) > 0
  }
}
