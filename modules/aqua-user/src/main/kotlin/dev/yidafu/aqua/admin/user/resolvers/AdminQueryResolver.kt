/*
 * AquaRush Admin Service Query Resolver
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

package dev.yidafu.aqua.admin.user.resolvers

import dev.yidafu.aqua.user.domain.model.Admin
import dev.yidafu.aqua.user.domain.repository.AdminRepository
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
class AdminQueryResolver(
  private val adminRepository: AdminRepository
) {

  /**
   * 获取所有管理员 - 管理员权限
   */
  @QueryMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun admins(): List<Admin> {
    return adminRepository.findAllAdmins()
  }

  /**
   * 根据ID获取管理员 - 管理员权限
   */
  @QueryMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun admin(
    @Argument id: Long,
  ): Admin? {
    return adminRepository.findAdminById(id)
  }
}
