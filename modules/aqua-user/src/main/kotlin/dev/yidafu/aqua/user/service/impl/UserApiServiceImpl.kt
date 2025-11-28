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

import dev.yidafu.aqua.api.common.PagedResponse
import dev.yidafu.aqua.api.dto.*
import dev.yidafu.aqua.api.service.UserApiService
import dev.yidafu.aqua.common.exception.BusinessException
import dev.yidafu.aqua.common.exception.NotFoundException
import dev.yidafu.aqua.common.messaging.service.SimplifiedEventPublishService
import dev.yidafu.aqua.user.domain.ext.toDTO
import dev.yidafu.aqua.user.domain.model.User
import dev.yidafu.aqua.user.domain.repository.AddressRepository
import dev.yidafu.aqua.user.domain.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.jvm.optionals.getOrNull

/**
 * 用户API服务实现类
 */
@Service
@Transactional
class UserApiServiceImpl(
  private val userRepository: UserRepository,
  private val addressRepository: AddressRepository,
  private val eventPublishService: SimplifiedEventPublishService,
) : UserApiService {
  private val logger = LoggerFactory.getLogger(UserApiServiceImpl::class.java)

  override fun getUserById(userId: Long): UserDTO? {
    logger.info("Getting user by ID: $userId")
    val user = userRepository.findById(userId)
    return user.getOrNull()?.toDTO()
  }

  override fun getUserByOpenId(openId: String): UserDTO? {
    logger.info("Getting user by openId: $openId")
    val user = userRepository.findByWechatOpenId(openId)
    return user?.toDTO()
  }

  override fun getUserByPhone(phone: String): UserDTO? {
    logger.info("Getting user by phone: $phone")
    val user = userRepository.findById(0L).getOrNull()
//        val user = userRepository.findByPhone(phone)
    return user?.toDTO()
  }

  override fun createUser(request: CreateUserRequest): UserDTO {
    logger.info("Creating user with request: $request")

    if (userRepository.existsByWechatOpenId(request.openId)) {
      throw BusinessException("用户已存在: ${request.openId}")
    }

    val user =
      User(
        wechatOpenId = request.openId,
        nickname = request.nickname,
        phone = request.phone,
        avatarUrl = request.avatar,

        updatedAt = LocalDateTime.now(),
        email = "",
        status = UserStatus.ACTIVE,
        role = UserRole.USER,
        balance = BigDecimal(0),
        totalSpent = BigDecimal(0),
        lastLoginAt = LocalDateTime.now(),
      )

    val savedUser = userRepository.save(user)
    logger.info("Created user: ${savedUser.id}")

    // 发布用户创建事件
    eventPublishService.publishDomainEvent(
      eventType = "USER_CREATED",
      aggregateId = savedUser.id.toString(),
      eventData =
        mapOf<String, Any>(
          "userId" to (savedUser.id ?: 0L),
          "nickname" to (savedUser.nickname ?: ""),
          "phone" to (savedUser.phone ?: ""),
          "avatar" to (savedUser.avatarUrl ?: ""),
          "timestamp" to System.currentTimeMillis(),
        ),
    )

    return savedUser.toDTO()
  }

  override fun updateUser(
    userId: Long,
    request: UpdateUserRequest,
  ): UserDTO {
    logger.info("Updating user $userId with request: $request")

    val user =
      userRepository.findById(userId).getOrNull()
        ?: throw NotFoundException("用户不存在: $userId")

    // 更新字段
    request.nickname?.let { user.nickname = it }
    request.phone?.let { user.phone = it }
    request.avatar?.let { user.avatarUrl = it }

    val savedUser = userRepository.save(user)
    logger.info("Updated user: ${savedUser.id}")

    // 发布用户更新事件
    eventPublishService.publishDomainEvent(
      eventType = "USER_UPDATED",
      aggregateId = savedUser.id.toString(),
      eventData =
        mapOf<String, Any>(
          "userId" to (savedUser.id ?: 0L),
          "nickname" to (savedUser.nickname ?: ""),
          "phone" to (savedUser.phone ?: ""),
          "avatar" to (savedUser.avatarUrl ?: ""),
          "timestamp" to System.currentTimeMillis(),
        ),
    )

    return savedUser.toDTO()
  }

  override fun updateUserStatus(
    userId: Long,
    status: UserStatus,
  ): UserDTO {
    logger.info("Updating user $userId status to: $status")

    val user =
      userRepository.findById(userId).getOrNull()
        ?: throw NotFoundException("用户不存在: $userId")

    val savedUser = userRepository.save(user)
    return savedUser.toDTO()
  }

  override fun getUserList(
    page: Int,
    size: Int,
  ): PagedResponse<UserDTO> {
    logger.info("Getting user list: page=$page, size=$size")

    val pageable: Pageable = PageRequest.of(page - 1, size)
    val userPage: Page<User> = userRepository.findAll(pageable)

    val userDTOs = userPage.content.map { it.toDTO() }

    return PagedResponse(
      content = userDTOs,
      page = page,
      size = size,
      totalElements = userPage.totalElements,
      totalPages = userPage.totalPages,
      first = userPage.isFirst,
      last = userPage.isLast,
    )
  }

  override fun deleteUser(userId: Long): Boolean {
    logger.info("Deleting user: $userId")

    val user =
      userRepository.findById(userId).getOrNull()
        ?: throw NotFoundException("用户不存在: $userId")

    userRepository.delete(user)

    // 发布用户删除事件
    eventPublishService.publishDomainEvent(
      eventType = "USER_DELETED",
      aggregateId = userId.toString(),
      eventData =
        mapOf(
          "userId" to userId,
          "timestamp" to System.currentTimeMillis(),
        ),
    )

    return true
  }

  override fun loginUser(
    openId: String,
    phone: String,
  ): UserDTO {
    logger.info("User login: openId=$openId, phone=$phone")

    val user =
      userRepository.findByWechatOpenId(openId)
        ?: throw NotFoundException("用户不存在: $openId")

    return user.toDTO().copy(
      lastLoginAt = LocalDateTime.now(),
    )
  }

  override fun updateLastLogin(userId: Long): UserDTO {
    logger.info("Updating last login for user: $userId")

    val user =
      userRepository.findById(userId).getOrNull()
        ?: throw NotFoundException("用户不存在: $userId")

    return user.toDTO().copy(
      lastLoginAt = LocalDateTime.now(),
    )
  }

  override fun updateNotificationSettings(
    userId: Long,
    settings: NotificationSettingsDTO,
  ): UserDTO {
    logger.info("Updating notification settings for user: $userId")

    val user =
      userRepository.findById(userId).getOrNull()
        ?: throw NotFoundException("用户不存在: $userId")

    return user.toDTO().copy(
      notificationSettings = settings,
    )
  }
}
