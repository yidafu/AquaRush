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
import dev.yidafu.aqua.api.service.user.UserService
import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.common.domain.model.DeliverWorkerModelStatus
import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.domain.model.enums.AdminAccountStatus
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.exception.UserNotFoundException
import dev.yidafu.aqua.common.id.SnowflakeIdGenerator
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.user.domain.repository.AdminRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for handling admin operations
 *
 * 送水员档案并入 admins 单表(PRD/07-user.md §F-USR-021 目标架构):role=DELIVERY_WORKER
 * 的账号行即送水员身份,配送字段直接挂账号行,不再联动 delivery_workers 档案表。
 */
@Service
@Transactional
class AdminServiceImpl(
  private val adminRepository: AdminRepository,
  private val userService: UserService,
  private val passwordEncoder: PasswordEncoder,
  private val snowflakeIdGenerator: SnowflakeIdGenerator,
  private val bizLogger: BizLogger,
) : AdminService {
  private val logger = LoggerFactory.getLogger(AdminServiceImpl::class.java)

  companion object {
    // 初始/重置缺省密码(PRD/07-user.md §F-USR-010/021:缺省 123456)
    private const val DEFAULT_PASSWORD = "123456"
    private const val DELIVERY_WORKER_USERNAME_PREFIX = "delivery_"
    private val DELIVERY_CAPABLE_ROLES = listOf(AdminRoleModel.DELIVERY_WORKER, AdminRoleModel.ADMIN)

    /**
     * 内部装配/统计场景的 SQL 侧上限(配送排班候选、送水员统计等)。
     * 这些调用方需要「尽可能全」的配送能力账号,但不是对外列表接口 —— 用有界 limit
     * 代替无界全表扫描(AGENTS.md 规则 10);业务上配送账号量级远低于该上限。
     */
    private const val INTERNAL_DELIVERY_LIST_LIMIT = 500
  }

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
   * 统计指定角色账号数(删除保护:至少保留一个超级管理员)
   */
  override fun countByRole(role: AdminRoleModel): Long = adminRepository.countByRole(role)

  /**
   * 重置账号密码(G11 落地,PRD/07-user.md §F-USR-020/021)
   *
   * newPassword 为空时重置为默认密码 123456;重置后原 token 仍有效至过期(签名不变),
   * 停用/删除才即时失效。
   */
  override fun resetPassword(
    id: Long,
    newPassword: String?,
  ): AdminModel {
    val admin =
      adminRepository.findAdminById(id)
        ?: throw BadRequestException("管理员不存在: $id")

    val passwordToEncode = newPassword ?: DEFAULT_PASSWORD
    if (passwordToEncode.length < 6) {
      throw BadRequestException("新密码长度不能少于6位")
    }
    val passwordHash =
      requireNotNull(passwordEncoder.encode(passwordToEncode)) { "Password encoding returned null" }
    admin.passwordHash = passwordHash

    val savedAdmin = adminRepository.save(admin)

    logger.info("Successfully reset password for admin: ${savedAdmin.id} - ${savedAdmin.username}")

    // 业务日志(F-LOG-003 USER 域):重置密码
    val (operatorId, operatorName) = bizLogger.resolveOperator()
    bizLogger.logUserOperation(
      targetUserId = savedAdmin.id!!,
      operation = "ADMIN_PASSWORD_RESET",
      result = "SUCCESS",
      operatorId = operatorId,
      operatorName = operatorName,
      additionalData = mapOf("username" to savedAdmin.username),
    )
    return savedAdmin
  }

  /**
   * 具备配送能力的账号(role ∈ {DELIVERY_WORKER, ADMIN},兼容旧 deliveryWorkers 查询语义)
   */
  override fun findAllDeliveryCapable(): List<AdminModel> =
    adminRepository.findAllByRoleIn(DELIVERY_CAPABLE_ROLES, INTERNAL_DELIVERY_LIST_LIMIT)

  override fun findDeliveryWorkers(
    keyword: String?,
    role: AdminRoleModel?,
    pageable: Pageable,
  ): Page<AdminModel> {
    val roles = if (role == null) DELIVERY_CAPABLE_ROLES else listOf(role)
    return adminRepository.findAllByRoleIn(roles, keyword, pageable)
  }

  /**
   * 具备配送能力的账号数(role ∈ {DELIVERY_WORKER, ADMIN},条件 count,避免全表加载)
   */
  override fun countDeliveryCapable(): Long = adminRepository.countByRoleIn(DELIVERY_CAPABLE_ROLES)

  /**
   * 按在线状态查询配送能力账号
   */
  override fun findByOnlineStatus(status: DeliverWorkerModelStatus): List<AdminModel> =
    adminRepository
      .findByOnlineStatus(status, INTERNAL_DELIVERY_LIST_LIMIT)
      .filter { it.role in DELIVERY_CAPABLE_ROLES }

  /**
   * Check if admin exists by phone
   */
  override fun existsByPhone(phone: String): Boolean = adminRepository.existsByPhone(phone)

  /**
   * Create a new admin user
   *
   * role=DELIVERY_WORKER 且未传 username 时,用户名自动生成 `delivery_{账号ID}`(单表落库,见 F-USR-021)。
   */
  override fun createAdmin(
    username: String?,
    password: String?,
    realName: String?,
    phone: String?,
    role: AdminRoleModel,
    avatarUrl: String?,
    onlineStatus: DeliverWorkerModelStatus,
    isAvailable: Boolean,
    coordinates: String?,
    currentLocation: String?,
    rating: Double?,
    earningCents: Long?,
    accountStatus: AdminAccountStatus,
  ): AdminModel {
    val resolvedUsername: String
    var autoGeneratedUsername = false
    if (username.isNullOrBlank()) {
      if (role != AdminRoleModel.DELIVERY_WORKER) {
        throw BadRequestException("用户名不能为空")
      }
      // 送水员账号:先以唯一占位用户名落库(Hibernate 生成账号 id),再改为 delivery_{账号ID}
      // (预分配 id 会让 save() 走 merge,触发 StaleObjectState,故由 Hibernate 生成)
      autoGeneratedUsername = true
      resolvedUsername = "$DELIVERY_WORKER_USERNAME_PREFIX${snowflakeIdGenerator.nextId()}"
    } else {
      resolvedUsername = username
    }

    // Check if username already exists
    if (adminRepository.existsByUsername(resolvedUsername)) {
      throw BadRequestException("用户名已存在: $resolvedUsername")
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
    // id 置空交由 Hibernate @SnowflakeIdGenerator 在 INSERT 时生成;
    // 手动预分配 id 会让 save() 走 merge,重复运行/并发时抛 StaleObjectState
    val admin =
      AdminModel(
        username = resolvedUsername,
        passwordHash = passwordHash,
        realName = realName,
        phone = phone,
        role = role,
        avatarUrl = avatarUrl,
        onlineStatus = onlineStatus,
        isAvailable = isAvailable,
        coordinates = coordinates,
        currentLocation = currentLocation,
        rating = rating,
        earningCents = earningCents,
        accountStatus = accountStatus,
      )

    val savedAdmin = adminRepository.save(admin)

    // 送水员账号:落库后按 Hibernate 生成的账号 id 修正用户名 delivery_{账号ID}(同事务)
    if (autoGeneratedUsername) {
      savedAdmin.username = "$DELIVERY_WORKER_USERNAME_PREFIX${savedAdmin.id}"
      adminRepository.save(savedAdmin)
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
    avatarUrl: String?,
    onlineStatus: DeliverWorkerModelStatus?,
    isAvailable: Boolean?,
    coordinates: String?,
    currentLocation: String?,
    rating: Double?,
    earningCents: Long?,
    accountStatus: AdminAccountStatus?,
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

    // 账号状态(停用/启用,F-USR-020/021)
    accountStatus?.let { existingAdmin.accountStatus = it }

    // 配送字段直接更新账号行(送水员档案并入 admins 单表,无档案联动)
    avatarUrl?.let { existingAdmin.avatarUrl = it }
    onlineStatus?.let { existingAdmin.onlineStatus = it }
    isAvailable?.let { existingAdmin.isAvailable = it }
    coordinates?.let { existingAdmin.coordinates = it }
    currentLocation?.let { existingAdmin.currentLocation = it }
    rating?.let { existingAdmin.rating = it }
    earningCents?.let { existingAdmin.earningCents = it }

    val savedAdmin = adminRepository.save(existingAdmin)

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

  override fun getUserById(id: Long): UserModel {
    val admin = findById(id) ?: throw UserNotFoundException("管理员未关联微信账号")
    val userId = admin.userId ?: throw UserNotFoundException("管理员未关联微信账号")
    return userService.findById(userId) ?: throw UserNotFoundException("管理员未关联微信账号")
  }
}
