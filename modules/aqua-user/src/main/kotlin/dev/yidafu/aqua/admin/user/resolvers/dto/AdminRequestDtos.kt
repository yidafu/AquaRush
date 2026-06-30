/**
 * AquaRush Admin User Resolver DTOs
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

package dev.yidafu.aqua.admin.user.resolvers.dto

import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import dev.yidafu.aqua.common.graphql.generated.CreateAdminInput
import dev.yidafu.aqua.common.graphql.generated.UpdateAdminInput
import dev.yidafu.aqua.user.mapper.AdminRoleMapper

/**
 * Request DTO for creating admin user
 */
data class CreateAdminRequest(
  val username: String,
  val password: String?,
  val realName: String?,
  val phone: String?,
  val role: AdminRoleModel,
) {
  companion object {
    fun fromInput(input: CreateAdminInput): CreateAdminRequest =
      CreateAdminRequest(
        username = input.username,
        password = input.password,
        realName = input.realName,
        phone = input.phone,
        role = AdminRoleMapper.map(input.role),
      )
  }
}

/**
 * Request DTO for updating admin user
 */
data class UpdateAdminRequest(
  val realName: String?,
  val phone: String?,
  val role: AdminRoleModel?,
) {
  companion object {
    fun fromInput(input: UpdateAdminInput): UpdateAdminRequest =
      UpdateAdminRequest(
        realName = input.realName,
        phone = input.phone,
        role = input.role?.let { AdminRoleMapper.map(it) },
      )
  }
}
