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

package dev.yidafu.aqua.user.service

import dev.yidafu.aqua.user.domain.model.Admin
import dev.yidafu.aqua.user.domain.repository.AdminRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for handling admin operations
 */
@Service
@Transactional
class AdminService(
  private val adminRepository: AdminRepository,
) {

  /**
   * Find all admin users
   */
  fun findAll(): List<Admin> = adminRepository.findAll()

  /**
   * Find admin by id
   */
  fun findById(id: Long): Admin? = adminRepository.findById(id).orElse(null)

  /**
   * Find admin by username
   */
  fun findByUsername(username: String): Admin? = adminRepository.findByUsername(username).orElse(null)

  /**
   * Create or update admin
   */
  fun save(admin: Admin): Admin = adminRepository.save(admin)

  /**
   * Delete admin by id
   */
  fun deleteById(id: Long): Boolean {
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
  fun existsByUsername(username: String): Boolean = adminRepository.existsByUsername(username)

  /**
   * Check if admin exists by id
   */
  fun existsById(id: Long): Boolean = adminRepository.existsById(id)

  /**
   * Find admins by role
   */
  fun findByRole(role: dev.yidafu.aqua.user.domain.model.AdminRole): List<Admin> =
    adminRepository.findByRole(role)
}