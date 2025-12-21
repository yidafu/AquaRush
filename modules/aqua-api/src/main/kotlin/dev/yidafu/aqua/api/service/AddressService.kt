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

import dev.yidafu.aqua.common.domain.model.AddressModel
import dev.yidafu.aqua.common.dto.AddressUpdateRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest

/**
 * 地址服务接口
 */
interface AddressService {
  /**
   * 获取用户的所有地址
   */
  fun getUserAddresses(userId: Long): List<AddressModel>

  /**
   * 获取用户默认地址
   */
  fun getUserDefaultAddress(userId: Long): AddressModel?

  /**
   * 根据ID获取地址
   */
  fun getAddressById(addressId: Long): AddressModel?

  /**
   * 创建新地址 (使用Address对象)
   */
  fun createAddress(address: AddressModel): AddressModel

  /**
   * 更新地址
   */
  fun updateAddress(
    addressId: Long,
    userId: Long,
    province: String? = null,
    city: String? = null,
    district: String? = null,
    detailAddress: String? = null,
    provinceCode: String? = null,
    cityCode: String? = null,
    districtCode: String? = null,
    longitude: Double? = null,
    latitude: Double? = null,
    isDefault: Boolean? = null,
  ): AddressModel?

  /**
   * 更新地址 (使用AddressUpdateRequest对象)
   */
  fun updateAddress(
    addressId: Long,
    updates: AddressUpdateRequest,
  ): AddressModel?

  /**
   * 删除地址
   */
  fun deleteAddress(
    addressId: Long,
    userId: Long,
  ): Boolean

  /**
   * 设置默认地址
   */
  fun setDefaultAddress(
    addressId: Long,
    userId: Long,
  ): Boolean

  /**
   * 根据坐标获取附近地址
   */
  fun findNearbyAddresses(
    longitude: Double,
    latitude: Double,
    radiusKm: Double = 5.0,
  ): List<AddressModel>

  /**
   * 检查地址是否重复
   */
  fun isDuplicateAddress(
    userId: Long,
    address: AddressModel,
  ): Boolean

  // Additional methods for compatibility with existing resolvers

  /**
   * 获取用户地址分页列表
   */
  fun findByUserId(userId: Long): List<AddressModel>

  /**
   * 获取用户默认地址
   */
  fun findDefaultByUserId(userId: Long): AddressModel?

  /**
   * 根据ID获取地址
   */
  fun findById(addressId: Long): AddressModel?

  /**
   * 搜索用户地址
   */
  fun searchByUserIdAndKeyword(
    userId: Long,
    keyword: String,
    pageable: PageRequest,
  ): Page<AddressModel>

  /**
   * 获取用户地址数量
   */
  fun countByUserId(userId: Long): Int

  // Legacy method for backward compatibility
  fun save(address: AddressModel): AddressModel

  // Legacy method for backward compatibility
  fun deleteById(addressId: Long)

  // Legacy method for backward compatibility
  fun unsetDefaultAddresses(userId: Long)
}
