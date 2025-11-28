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

import dev.yidafu.aqua.user.domain.model.Admin
import dev.yidafu.aqua.user.domain.model.AdminRole
import org.springframework.stereotype.Component
import tech.mappie.api.ObjectMappie

// Admin DTO (可根据实际需要定义)
data class AdminDTO(
  val id: Long?,
  val username: String,
  val realName: String?,
  val phone: String?,
  val role: AdminRole,
  val createdAt: java.time.LocalDateTime,
  val lastLoginAt: java.time.LocalDateTime?,
  val updatedAt: java.time.LocalDateTime,
)

@Component
object AdminMapper : ObjectMappie<Admin, AdminDTO>()
