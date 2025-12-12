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

package dev.yidafu.aqua.user.domain.ext

import dev.yidafu.aqua.api.dto.NotificationSettingsDTO
import dev.yidafu.aqua.api.dto.UserDTO
import dev.yidafu.aqua.api.dto.UserRole
import dev.yidafu.aqua.api.dto.UserStatus
import dev.yidafu.aqua.user.domain.model.UserModel
import java.math.BigDecimal
import java.time.LocalDateTime

fun UserModel.toDTO(): UserDTO =
  UserDTO(
    id = id ?: 0L,
    wechatOpenId = null,
    nickname = nickname,
    phone = phone,
    email = "",
    avatarUrl = avatarUrl,
    status = UserStatus.ACTIVE,
    role = UserRole.USER,
    balance = BigDecimal(0),
    totalSpent = BigDecimal(0),
    addresses = listOf(),
    createdAt = LocalDateTime.now(),
    updatedAt = LocalDateTime.now(),
    lastLoginAt = LocalDateTime.now(),
    notificationSettings = NotificationSettingsDTO(),
  )
