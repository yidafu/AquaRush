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

import dev.yidafu.aqua.api.service.AddressQueryApiService
import dev.yidafu.aqua.api.service.AdminQueryApiService
import dev.yidafu.aqua.common.domain.model.AddressModel
import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.user.domain.repository.AddressRepository
import dev.yidafu.aqua.user.domain.repository.AdminRepository
import org.springframework.stereotype.Service

/**
 * 跨模块地址查询服务实现
 * 供其他模块使用
 */
@Service
class AddressQueryApiServiceImpl(
  private val addressRepository: AddressRepository,
) : AddressQueryApiService {
  override fun findById(id: Long): AddressModel? = addressRepository.findById(id).orElse(null)

  override fun findByUserId(userId: Long): List<AddressModel> = addressRepository.findByUserId(userId)

  override fun findDefaultByUserId(userId: Long): AddressModel? = addressRepository.findByUserIdAndIsDefaultTrue(userId)

  override fun existsById(id: Long): Boolean = addressRepository.existsById(id)
}

/**
 * 跨模块管理员查询服务实现
 * 供其他模块使用
 */
@Service
class AdminQueryApiServiceImpl(
  private val adminRepository: AdminRepository,
) : AdminQueryApiService {
  override fun findById(id: Long): AdminModel? = adminRepository.findAdminById(id)

  override fun findByUsername(username: String): AdminModel? = adminRepository.findByUsername(username)

  override fun findAll(): List<AdminModel> = adminRepository.findAllAdmins()

  override fun existsById(id: Long): Boolean = adminRepository.existsById(id)
}
