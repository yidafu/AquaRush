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

package dev.yidafu.aqua.api.service

import dev.yidafu.aqua.api.common.PagedResponse
import dev.yidafu.aqua.api.dto.*
import java.util.*

/**
 * 用户API服务接口
 */
interface UserApiService {
  /**
   * 根据ID获取用户信息
   */
  fun getUserById(userId: Long): UserDTO?

  /**
   * 根据openId获取用户信息
   */
  fun getUserByOpenId(openId: String): UserDTO?

  /**
   * 根据手机号获取用户信息
   */
  fun getUserByPhone(phone: String): UserDTO?

  /**
   * 创建新用户
   */
  fun createUser(request: CreateUserRequest): UserDTO

  /**
   * 更新用户信息
   */
  fun updateUser(
    userId: Long,
    request: UpdateUserRequest,
  ): UserDTO

  /**
   * 更新用户状态
   */
  fun updateUserStatus(
    userId: Long,
    status: UserStatus,
  ): UserDTO

  /**
   * 获取用户列表（分页）
   */
  fun getUserList(
    page: Int = 0,
    size: Int = 20,
  ): PagedResponse<UserDTO>

  /**
   * 删除用户
   */
  fun deleteUser(userId: Long): Boolean

  /**
   * 用户登录
   */
  fun loginUser(
    openId: String,
    phone: String,
  ): UserDTO

  /**
   * 更新最后登录时间
   */
  fun updateLastLogin(userId: Long): UserDTO

  /**
   * 更新用户通知设置
   */
  fun updateNotificationSettings(
    userId: Long,
    settings: NotificationSettingsDTO,
  ): UserDTO
}
