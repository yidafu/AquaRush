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

import dev.yidafu.aqua.api.dto.UserRole
import dev.yidafu.aqua.api.dto.UserStatus
import dev.yidafu.aqua.user.domain.model.UserModel
import dev.yidafu.aqua.user.domain.model.NotificationSettingsModel
import dev.yidafu.aqua.user.domain.repository.UserRepository
import dev.yidafu.aqua.user.domain.repository.NotificationSettingsRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class UserService(
  private val userRepository: UserRepository,
  private val notificationSettingsRepository: NotificationSettingsRepository,
) {
  fun findById(id: Long): UserModel? = userRepository.findById(id).orElse(null)

  fun findByWechatOpenId(wechatOpenId: String): UserModel? = userRepository.findByWechatOpenId(wechatOpenId)

  @Transactional
  fun createUser(
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

    // Create default notification settings for the user
    val notificationSettings = NotificationSettingsModel(
      userId = savedUser.id!!,
      orderUpdates = true,
      paymentNotifications = true,
      deliveryNotifications = true,
      promotionalNotifications = false,
    )
    notificationSettingsRepository.save(notificationSettings)

    return savedUser
  }

  @Transactional
  fun updateUserInfo(
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

    return userRepository.save(user)
  }

  fun existsByWechatOpenId(wechatOpenId: String): Boolean = userRepository.existsByWechatOpenId(wechatOpenId)

  fun findAllUsers(pageable: Pageable): Page<UserModel> = userRepository.findAll(pageable)

  fun findUsersByKeyword(keyword: String, pageable: Pageable): Page<UserModel> {
    return userRepository.findByNicknameContainingIgnoreCaseOrPhoneContainingIgnoreCase(keyword, keyword, pageable)
  }

  fun findUsersByStatus(status: UserStatus, pageable: Pageable): Page<UserModel> {
    return userRepository.findByStatus(status, pageable)
  }

  fun findUsersByKeywordAndStatus(keyword: String, status: UserStatus, pageable: Pageable): Page<UserModel> {
    return userRepository.findByNicknameContainingIgnoreCaseAndStatusOrPhoneContainingIgnoreCaseAndStatus(
      keyword, status, pageable
    )
  }
}
