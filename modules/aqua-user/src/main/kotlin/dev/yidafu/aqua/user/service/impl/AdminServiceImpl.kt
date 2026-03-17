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
import dev.yidafu.aqua.api.service.UserService
import dev.yidafu.aqua.api.service.delivery.DeliveryWorkerMutationService
import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.common.domain.model.AdminRoleModel
import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.exception.UserNotFoundException
import dev.yidafu.aqua.common.graphql.generated.DeliveryWorker
import dev.yidafu.aqua.common.id.SnowflakeIdGenerator
import dev.yidafu.aqua.user.domain.repository.AdminRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for handling admin operations
 */
@Service
@Transactional
class AdminServiceImpl(
  private val adminRepository: AdminRepository,
  private val deliveryWorkerMutationService: DeliveryWorkerMutationService,
  private val userService: UserService,
) : AdminService {
  private val logger = LoggerFactory.getLogger(AdminServiceImpl::class.java)

  companion object {
    private const val DEFAULT_PASSWORD = "12345678"
  }

  /**
   * Find all admin users
   */
  override fun findAll(): List<AdminModel> = adminRepository.findAll()

  /**
   * Find admin by id
   */
  override fun findById(id: Long): AdminModel? = adminRepository.findById(id).orElse(null)

  override fun findByUserId(userId: Long): AdminModel? = adminRepository.findByUserId(userId)

  /**
   * Find admin by username
   */
  override fun findByUsername(username: String): AdminModel? = adminRepository.findByUsername(username)

  /**
   * Create or update admin
   */
  override fun save(admin: AdminModel): AdminModel = adminRepository.save(admin)

  /**
   * Delete admin by id
   */
  override fun deleteById(id: Long): Boolean =
    if (adminRepository.existsById(id)) {
      adminRepository.deleteById(id)
      true
    } else {
      false
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
  override fun findByRole(role: AdminRoleModel): List<AdminModel> = adminRepository.findByRole(role)

  /**
   * Check if admin exists by phone
   */
  override fun existsByPhone(phone: String): Boolean = adminRepository.existsByPhone(phone)

  /**
   * Create a new admin user
   */
  override fun createAdmin(
    username: String,
    password: String?,
    realName: String?,
    phone: String?,
    role: AdminRoleModel,
  ): AdminModel {
    // Check if username already exists
    if (adminRepository.existsByUsername(username)) {
      throw BadRequestException("用户名已存在: $username")
    }

    // Check if phone already exists
    if (!phone.isNullOrBlank() && adminRepository.existsByPhone(phone)) {
      throw BadRequestException("手机号已存在: $phone")
    }

    // Encode password
    val passwordToEncode = password ?: DEFAULT_PASSWORD
    val passwordHash = "" // passwordEncoder.encode(passwordToEncode) ?: ""

    // Create admin user
    val admin =
      AdminModel(
        id = SnowflakeIdGenerator().generate(),
        username = username,
        passwordHash = passwordHash,
        realName = realName,
        phone = phone,
        role = role,
      )

    val savedAdmin = adminRepository.save(admin)

    // Create delivery worker record for ADMIN and DELIVERY_WORKER roles
    if (role == AdminRoleModel.ADMIN || role == AdminRoleModel.DELIVERY_WORKER) {
      deliveryWorkerMutationService.createDeliveryWorker(
        adminId = savedAdmin.id,
        name = realName ?: username,
        phone = phone ?: "",
        wechatOpenId = "",
      )
      logger.info("Created delivery worker record for admin: ${savedAdmin.id}")
    }

    logger.info("Successfully created admin user: ${savedAdmin.id} - ${savedAdmin.username} with role: ${role.name}")
    return savedAdmin
  }

  /**
   * Update an existing admin user
   */
  override fun updateAdmin(
    id: Long,
    realName: String?,
    phone: String?,
    role: AdminRoleModel?,
  ): AdminModel {
    val existingAdmin =
      adminRepository.findAdminById(id)
        ?: throw BadRequestException("管理员不存在: $id")

    // Update realName
    realName?.let { existingAdmin.realName = it }

    // Update phone
    phone?.let { newPhone ->
      // Check if phone is used by other users (excluding self)
      val existingWithPhone = adminRepository.findByPhone(newPhone)
      if (existingWithPhone != null && existingWithPhone.id != id) {
        throw BadRequestException("手机号已被其他用户使用: $newPhone")
      }
      existingAdmin.phone = newPhone
    }

    // Update role
    role?.let { existingAdmin.role = it }

    val savedAdmin = adminRepository.save(existingAdmin)
    logger.info("Successfully updated admin user: ${savedAdmin.id}")
    return savedAdmin
  }

  /**
   * Delete an admin user
   */
  override fun deleteAdmin(id: Long): Boolean {
    val existingAdmin =
      adminRepository.findAdminById(id)
        ?: throw BadRequestException("管理员不存在: $id")

    // Cannot delete super admin
    if (existingAdmin.role == AdminRoleModel.SUPER_ADMIN) {
      throw BadRequestException("不能删除超级管理员")
    }

    adminRepository.delete(existingAdmin)
    logger.info("Successfully deleted admin user: $id")
    return true
  }

  override fun getDeliveryWorkerById(id: Long): DeliveryWorker? = null

  override fun getUserById(id: Long): UserModel {
    val admin = findById(id) ?: throw UserNotFoundException("管理员未关联微信账号")
    val userId = admin.userId ?: throw UserNotFoundException("管理员未关联微信账号")
    return userService.findById(userId) ?: throw UserNotFoundException("管理员未关联微信账号")
  }
}
