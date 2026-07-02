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

package dev.yidafu.aqua.api.service

import dev.yidafu.aqua.common.domain.model.UserModel
import java.time.LocalDateTime

/**
 * 跨模块用户查询服务接口
 * 由 aqua-user 模块实现，供其他模块使用
 */
interface UserQueryApiService {
  fun findAll(): List<UserModel>

  fun findById(id: Long): UserModel?

  fun findByIds(ids: List<Long>): List<UserModel>

  fun findByPhone(phone: String): UserModel?

  fun existsById(id: Long): Boolean

  fun findUsersCreatedAfter(startDate: LocalDateTime): List<UserModel>

  fun findUsersCreatedBetween(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): List<UserModel>

  fun countUsersCreatedBetween(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): Long
}
