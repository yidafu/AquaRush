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

import dev.yidafu.aqua.api.service.notification.NotificationMutationApiService
import dev.yidafu.aqua.api.service.user.UserService
import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.graphql.generated.UserRole
import dev.yidafu.aqua.common.graphql.generated.UserStatus
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.user.domain.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class UserServiceImpl(
  private val userRepository: UserRepository,
  private val notificationMutationApiService: NotificationMutationApiService,
  private val bizLogger: BizLogger,
) : UserService {
  override fun findById(id: Long): UserModel? = userRepository.findById(id).orElse(null)

  override fun findByIds(ids: List<Long>): List<UserModel> = userRepository.findAllById(ids)

  override fun findByWechatOpenId(wechatOpenId: String): UserModel? = userRepository.findByWechatOpenId(wechatOpenId)

  @Transactional
  override fun createUser(
    wechatOpenId: String,
    nickname: String?,
    avatarUrl: String?,
  ): UserModel {
    val user =
      UserModel(
        wechatOpenId = wechatOpenId,
        nickname = nickname,
        avatarUrl = avatarUrl,
        phone = null,
        email = "",
        status = UserStatus.ACTIVE,
        role = UserRole.USER,
//        balance = 0L,
//        totalSpent = BigDecimal.ZERO,
        lastLoginAt = LocalDateTime.now(),
      )

    val savedUser = userRepository.save(user)

    // 用户创建落默认通知开关(user_notification_settings,AC-NTF-005-04;缺陷 #7 双表合一后的唯一写入口)
    notificationMutationApiService.createDefaultUserSettings(savedUser.id!!)

    // 业务日志(F-LOG-003 USER 域):用户创建
    val (operatorId, operatorName) = bizLogger.resolveOperator()
    bizLogger.logUserOperation(
      targetUserId = savedUser.id!!,
      operation = "USER_CREATED",
      result = "SUCCESS",
      operatorId = operatorId,
      operatorName = operatorName,
    )
    return savedUser
  }

  @Transactional
  override fun updateUserInfo(
    userId: Long,
    nickname: String?,
    phone: String?,
    avatarUrl: String?,
  ): UserModel {
    val user =
      userRepository
        .findById(userId)
        .orElseThrow { IllegalArgumentException("User not found: $userId") }

    nickname?.let { user.nickname = it }
    phone?.let { user.phone = it }
    avatarUrl?.let { user.avatarUrl = it }
    val saved = userRepository.save(user)

    // 业务日志(F-LOG-003 USER 域):用户资料更新
    val (operatorId, operatorName) = bizLogger.resolveOperator()
    bizLogger.logUserOperation(
      targetUserId = saved.id!!,
      operation = "USER_UPDATED",
      result = "SUCCESS",
      operatorId = operatorId,
      operatorName = operatorName,
    )
    return saved
  }

  override fun existsByWechatOpenId(wechatOpenId: String): Boolean = userRepository.existsByWechatOpenId(wechatOpenId)

  // ===== 后台用户管理(F-USR-022,G11 落地)=====

  @Transactional
  override fun createUserByAdmin(
    nickname: String?,
    phone: String?,
    avatarUrl: String?,
  ): UserModel {
    // 微信登录用户由登录自动创建;后台手动创建的用户以 manual_{随机} 占位 openid
    // (WeChat openid 不以 manual_ 开头,不会与真实微信登录冲突)
    val placeholderOpenId = "manual_${java.util.UUID.randomUUID().toString().replace("-", "")}"
    val user =
      UserModel(
        wechatOpenId = placeholderOpenId,
        nickname = nickname,
        avatarUrl = avatarUrl,
        phone = phone,
        email = "",
        status = UserStatus.ACTIVE,
        role = UserRole.USER,
        lastLoginAt = LocalDateTime.now(),
      )

    val savedUser = userRepository.save(user)

    // 用户创建落默认通知开关(user_notification_settings,AC-NTF-005-04)
    notificationMutationApiService.createDefaultUserSettings(savedUser.id!!)

    // 业务日志(F-LOG-003 USER 域):后台创建用户
    val (operatorId, operatorName) = bizLogger.resolveOperator()
    bizLogger.logUserOperation(
      targetUserId = savedUser.id!!,
      operation = "USER_CREATED",
      result = "SUCCESS",
      operatorId = operatorId,
      operatorName = operatorName,
      additionalData = mapOf("source" to "ADMIN"),
    )
    return savedUser
  }

  @Transactional
  override fun updateUserByAdmin(
    id: Long,
    nickname: String?,
    phone: String?,
    avatarUrl: String?,
    status: UserStatus?,
  ): UserModel {
    val user =
      userRepository
        .findById(id)
        .orElseThrow { IllegalArgumentException("User not found: $id") }

    nickname?.let { user.nickname = it }
    phone?.let { user.phone = it }
    avatarUrl?.let { user.avatarUrl = it }
    status?.let { user.status = it }
    val saved = userRepository.save(user)

    // 业务日志(F-LOG-003 USER 域):后台编辑用户
    val (operatorId, operatorName) = bizLogger.resolveOperator()
    bizLogger.logUserOperation(
      targetUserId = saved.id!!,
      operation = "USER_UPDATED",
      result = "SUCCESS",
      operatorId = operatorId,
      operatorName = operatorName,
      additionalData = mapOf("source" to "ADMIN"),
    )
    return saved
  }

  @Transactional
  override fun deleteUserById(id: Long): Boolean {
    if (!userRepository.existsById(id)) {
      return false
    }
    userRepository.deleteById(id)

    // 业务日志(F-LOG-003 USER 域):后台删除用户(软删除)
    val (operatorId, operatorName) = bizLogger.resolveOperator()
    bizLogger.logUserOperation(
      targetUserId = id,
      operation = "USER_DELETED",
      result = "SUCCESS",
      operatorId = operatorId,
      operatorName = operatorName,
    )
    return true
  }

  override fun findAllUsers(pageable: Pageable): Page<UserModel> = userRepository.findAll(pageable)

  override fun findUsersByKeyword(
    keyword: String,
    pageable: Pageable,
  ): Page<UserModel> = userRepository.findByNicknameContainingIgnoreCaseOrPhoneContainingIgnoreCase(keyword, keyword, pageable)

  override fun findUsersByStatus(
    status: UserStatus,
    pageable: Pageable,
  ): Page<UserModel> = userRepository.findByStatus(status, pageable)

  override fun findUsersByKeywordAndStatus(
    keyword: String,
    status: UserStatus,
    pageable: Pageable,
  ): Page<UserModel> =
    userRepository.findByNicknameContainingIgnoreCaseAndStatusOrPhoneContainingIgnoreCaseAndStatus(
      keyword,
      status,
      pageable,
    )

  // ===== 客户分组(F-MKT-001,营销受众维度)=====
  override fun findUsersByCustomerGroupId(
    groupId: Long,
    pageable: Pageable,
  ): Page<UserModel> = userRepository.findByCustomerGroupId(groupId, pageable)

  @Transactional
  override fun setCustomerGroup(
    userId: Long,
    customerGroupId: Long?,
  ): UserModel {
    val user =
      userRepository
        .findById(userId)
        .orElseThrow { IllegalArgumentException("User not found: $userId") }
    val before = user.customerGroupId
    user.customerGroupId = customerGroupId
    val saved = userRepository.save(user)

    // 业务日志(F-LOG-003 MARKETING 域):用户客户分组变更
    val (operatorId, operatorName) = bizLogger.resolveOperator()
    bizLogger.logMarketingOperation(
      targetId = saved.id!!,
      operation = "CUSTOMER_GROUP_USER_SET",
      result = "SUCCESS",
      userId = operatorId,
      username = operatorName,
      additionalData =
        buildMap<String, Any> {
          before?.let { put("from", it) }
          customerGroupId?.let { put("to", it) }
        },
    )
    return saved
  }
}
