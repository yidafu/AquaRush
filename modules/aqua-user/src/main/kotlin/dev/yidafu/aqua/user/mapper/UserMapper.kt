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

package dev.yidafu.aqua.user.mapper

import dev.yidafu.aqua.api.dto.UserDTO
import dev.yidafu.aqua.api.dto.AddressDTO
import dev.yidafu.aqua.api.dto.NotificationSettingsDTO
import dev.yidafu.aqua.user.domain.model.User
import org.springframework.stereotype.Component
import tech.mappie.api.ObjectMappie

@Component
object UserMapper : ObjectMappie<User, UserDTO>() {
  override fun map(from: User): UserDTO {
    return UserDTO(
      id = from.id,
      wechatOpenId = from.wechatOpenId,
      nickname = from.nickname,
      phone = from.phone,
      email = from.email,
      avatarUrl = from.avatarUrl,
      status = from.status,
      role = from.role,
      balance = from.balance,
      totalSpent = from.totalSpent,
      addresses = emptyList(), // Will be loaded separately from Address entities
      createdAt = from.createdAt,
      updatedAt = from.updatedAt,
      lastLoginAt = from.lastLoginAt,
      notificationSettings = NotificationSettingsDTO(), // Will be loaded separately from NotificationSettings entity
    )
  }
}
