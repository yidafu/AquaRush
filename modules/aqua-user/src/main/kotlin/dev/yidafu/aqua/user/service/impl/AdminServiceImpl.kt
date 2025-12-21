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

package dev.yidafu.aqua.user.service.impl

import dev.yidafu.aqua.api.service.AdminService
import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.common.domain.model.AdminRoleModel
import dev.yidafu.aqua.user.domain.repository.AdminRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for handling admin operations
 */
@Service
@Transactional
class AdminServiceImpl(
  private val adminRepository: AdminRepository,
) : AdminService {

  /**
   * Find all admin users
   */
  override fun findAll(): List<AdminModel> = adminRepository.findAll()

  /**
   * Find admin by id
   */
  override fun findById(id: Long): AdminModel? = adminRepository.findById(id).orElse(null)

  /**
   * Find admin by username
   */
  override fun findByUsername(username: String): AdminModel? = adminRepository.findByUsername(username).orElse(null)

  /**
   * Create or update admin
   */
  override fun save(admin: AdminModel): AdminModel = adminRepository.save(admin)

  /**
   * Delete admin by id
   */
  override fun deleteById(id: Long): Boolean {
    return if (adminRepository.existsById(id)) {
      adminRepository.deleteById(id)
      true
    } else {
      false
    }
  }

  /**
   * Check if admin exists by username
   */
  override fun existsByUsername(username: String): Boolean = adminRepository.existsByUsername(username)

  /**
   * Check if admin exists by id
   */
  override fun existsById(id: Long): Boolean = adminRepository.existsById(id)

  /**
   * Find admins by role
   */
  override fun findByRole(role: AdminRoleModel): List<AdminModel> {
    return adminRepository.findByRole(role)
  }
}
