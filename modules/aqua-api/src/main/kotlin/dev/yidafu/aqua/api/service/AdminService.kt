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
 * along with this program.  If not, see &lt;https://www.gnu.org/licenses/&gt;.
 */

package dev.yidafu.aqua.api.service

import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.common.domain.model.AdminRoleModel

/**
 * 管理员服务接口
 */
interface AdminService {
  /**
   * Find all admin users
   */
  fun findAll(): List<AdminModel>

  /**
   * Find admin by id
   */
  fun findById(id: Long): AdminModel?

  /**
   * Find admin by username
   */
  fun findByUsername(username: String): AdminModel?

  /**
   * Create or update admin
   */
  fun save(admin: AdminModel): AdminModel

  /**
   * Delete admin by id
   */
  fun deleteById(id: Long): Boolean

  /**
   * Check if admin exists by username
   */
  fun existsByUsername(username: String): Boolean

  /**
   * Check if admin exists by id
   */
  fun existsById(id: Long): Boolean

  /**
   * Find admins by role
   */
  fun findByRole(role: AdminRoleModel): List<AdminModel>
}
