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

import dev.yidafu.aqua.common.domain.model.AddressModel
import dev.yidafu.aqua.common.domain.model.AdminModel

/**
 * 跨模块地址查询服务接口
 * 由 aqua-user 模块实现，供其他模块使用
 */
interface AddressQueryApiService {
    fun findById(id: Long): AddressModel?

    fun findByUserId(userId: Long): List<AddressModel>

    fun findDefaultByUserId(userId: Long): AddressModel?

    fun existsById(id: Long): Boolean
}

/**
 * 跨模块管理员查询服务接口
 * 由 aqua-user 模块实现，供其他模块使用
 */
interface AdminQueryApiService {
    fun findById(id: Long): AdminModel?

    fun findByUsername(username: String): AdminModel?

    fun findAll(): List<AdminModel>

    fun existsById(id: Long): Boolean
}
