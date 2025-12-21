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

import dev.yidafu.aqua.api.dto.*
import dev.yidafu.aqua.common.graphql.generated.Address
import java.util.*

/**
 * 地址API服务接口
 */
interface AddressApiService {
  /**
   * 获取用户的所有地址
   */
  fun getUserAddresses(userId: Long): List<Address>

  /**
   * 获取地址详情
   */
  fun getAddressById(addressId: Long): Address?

  /**
   * 添加新地址
   */
  fun addAddress(
    userId: Long,
    request: CreateAddressRequest,
  ): Address

  /**
   * 更新地址信息
   */
  fun updateAddress(
    userId: Long,
    addressId: Long,
    request: UpdateAddressRequest,
  ): Address

  /**
   * 设置默认地址
   */
  fun setDefaultAddress(
    userId: Long,
    addressId: Long,
  ): Address

  /**
   * 删除地址
   */
  fun deleteAddress(
    userId: Long,
    addressId: Long,
  ): Boolean

  /**
   * 验证地址是否有效
   */
  fun validateAddress(address: String): Boolean

  /**
   * 获取支持的城市列表
   */
  fun getSupportedCities(): List<String>

  /**
   * 获取用户默认地址
   */
  fun getDefaultAddress(userId: Long): Address?
}
