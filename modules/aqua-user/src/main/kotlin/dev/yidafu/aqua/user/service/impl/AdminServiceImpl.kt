/**
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

import dev.yidafu.aqua.api.service.admin.AdminService
import dev.yidafu.aqua.api.service.delivery.DeliveryWorkerMutationService
import dev.yidafu.aqua.api.service.delivery.DeliveryWorkerQueryService
import dev.yidafu.aqua.api.service.user.UserService
import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.exception.UserNotFoundException
import dev.yidafu.aqua.common.graphql.generated.DeliveryWorker
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.user.domain.repository.AdminRepository
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
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
  private val deliveryWorkerQueryService: DeliveryWorkerQueryService,
  private val userService: UserService,
  private val passwordEncoder: PasswordEncoder,
  private val bizLogger: BizLogger,
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
  override fun deleteById(id: Long): Boolean {
    val existed = adminRepository.existsById(id)
    if (!existed) {
      return false
    }
    adminRepository.deleteById(id)

    // 业务日志(F-LOG-003 USER 域):管理员删除
    val (operatorId, operatorName) = bizLogger.resolveOperator()
    bizLogger.logUserOperation(
      targetUserId = id,
      operation = "ADMIN_DELETED",
      result = "SUCCESS",
      operatorId = operatorId,
      operatorName = operatorName,
    )
    return true
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

    // Encode password（passwordToEncode 非空，Security 7 下 encode 返回 @Nullable，此处必非空）
    val passwordToEncode = password ?: DEFAULT_PASSWORD
    val passwordHash =
      requireNotNull(passwordEncoder.encode(passwordToEncode)) { "Password encoding returned null" }

    // Create admin user
    // id 置空交由 Hibernate @SnowflakeIdGenerator 在 INSERT 时生成；
    // 手动预分配 id 会让 save() 走 merge，重复运行/并发时抛 StaleObjectState
    val admin =
      AdminModel(
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
        adminId = savedAdmin.id!!,
        name = realName ?: username,
        phone = phone ?: "",
        wechatOpenId = "",
      )
      logger.info("Created delivery worker record for admin: ${savedAdmin.id}")
    }

    logger.info("Successfully created admin user: ${savedAdmin.id} - ${savedAdmin.username} with role: ${role.name}")

    // 业务日志(F-LOG-003 USER 域):管理员创建
    val (operatorId, operatorName) = bizLogger.resolveOperator()
    bizLogger.logUserOperation(
      targetUserId = savedAdmin.id!!,
      operation = "ADMIN_CREATED",
      result = "SUCCESS",
      operatorId = operatorId,
      operatorName = operatorName,
      additionalData = mapOf("username" to savedAdmin.username, "role" to savedAdmin.role.name),
    )
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

    // 角色切到 ADMIN/DELIVERY_WORKER 时确保存在关联的 delivery_worker 记录(与 createAdmin 一致)
    val newRole = savedAdmin.role
    if (newRole == AdminRoleModel.ADMIN || newRole == AdminRoleModel.DELIVERY_WORKER) {
      val workerExists =
        try {
          deliveryWorkerQueryService.getWorkerByAdminId(savedAdmin.id!!)
          true
        } catch (e: Exception) {
          false
        }
      if (!workerExists) {
        deliveryWorkerMutationService.createDeliveryWorker(
          adminId = savedAdmin.id!!,
          name = savedAdmin.realName ?: savedAdmin.username,
          phone = savedAdmin.phone ?: "",
          wechatOpenId = "",
        )
        logger.info("Created delivery worker record for admin: ${savedAdmin.id}")
      }
    }

    logger.info("Successfully updated admin user: ${savedAdmin.id}")

    // 业务日志(F-LOG-003 USER 域):管理员更新
    val (operatorId, operatorName) = bizLogger.resolveOperator()
    bizLogger.logUserOperation(
      targetUserId = savedAdmin.id!!,
      operation = "ADMIN_UPDATED",
      result = "SUCCESS",
      operatorId = operatorId,
      operatorName = operatorName,
      additionalData = mapOf("username" to savedAdmin.username, "role" to savedAdmin.role.name),
    )
    return savedAdmin
  }

  override fun getDeliveryWorkerById(id: Long): DeliveryWorker? = null

  override fun getUserById(id: Long): UserModel {
    val admin = findById(id) ?: throw UserNotFoundException("管理员未关联微信账号")
    val userId = admin.userId ?: throw UserNotFoundException("管理员未关联微信账号")
    return userService.findById(userId) ?: throw UserNotFoundException("管理员未关联微信账号")
  }
}
