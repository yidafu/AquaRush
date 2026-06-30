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

import dev.yidafu.aqua.api.service.UserQueryApiService
import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.user.domain.repository.UserRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * 跨模块用户查询服务实现
 * 供其他模块使用
 */
@Service
class UserQueryApiServiceImpl(
  private val userRepository: UserRepository,
) : UserQueryApiService {
  override fun findAll(): List<UserModel> = userRepository.findAll()

  override fun findById(id: Long): UserModel? = userRepository.findById(id).orElse(null)

  override fun findByIds(ids: List<Long>): List<UserModel> = userRepository.findAllById(ids)

  override fun findByPhone(phone: String): UserModel? = userRepository.findByPhone(phone)

  override fun existsById(id: Long): Boolean = userRepository.existsById(id)

  override fun findUsersCreatedAfter(startDate: LocalDateTime): List<UserModel> =
    userRepository.findAll().filter {
      it.createdAt?.isAfter(startDate) == true
    }

  override fun findUsersCreatedBetween(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): List<UserModel> =
    userRepository.findAll().filter {
      it.createdAt?.isAfter(startDate) == true && it.createdAt?.isBefore(endDate) == true
    }

  override fun countUsersCreatedBetween(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): Long = userRepository.count()
}
