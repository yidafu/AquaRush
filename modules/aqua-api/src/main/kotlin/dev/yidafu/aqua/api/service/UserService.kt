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

import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.graphql.generated.UserStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * 用户服务接口
 */
interface UserService {
  fun findById(id: Long): UserModel?

  fun findByWechatOpenId(wechatOpenId: String): UserModel?

  fun createUser(
    wechatOpenId: String,
    nickname: String?,
    avatarUrl: String?,
  ): UserModel

  fun updateUserInfo(
    userId: Long,
    nickname: String?,
    phone: String?,
    avatarUrl: String?,
  ): UserModel

  fun existsByWechatOpenId(wechatOpenId: String): Boolean

  fun findAllUsers(pageable: Pageable): Page<UserModel>

  fun findUsersByKeyword(
    keyword: String,
    pageable: Pageable,
  ): Page<UserModel>

  fun findUsersByStatus(
    status: UserStatus,
    pageable: Pageable,
  ): Page<UserModel>

  fun findUsersByKeywordAndStatus(
    keyword: String,
    status: UserStatus,
    pageable: Pageable,
  ): Page<UserModel>
}
