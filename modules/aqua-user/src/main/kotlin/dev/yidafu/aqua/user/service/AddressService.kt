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

package dev.yidafu.aqua.user.service

import dev.yidafu.aqua.user.domain.exception.AquaException
import dev.yidafu.aqua.user.domain.model.AddressModel
import dev.yidafu.aqua.user.domain.repository.AddressRepository
import dev.yidafu.aqua.user.domain.repository.RegionRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class AddressService(
  private val addressRepository: AddressRepository,
  private val regionRepository: RegionRepository,
  private val geolocationService: GeolocationService,
) {
  /**
   * 获取用户的所有地址
   */
  @Cacheable(value = ["user_addresses"], key = "#userId")
  fun getUserAddresses(userId: Long): List<AddressModel> = addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId)

  /**
   * 获取用户默认地址
   */
  @Cacheable(value = ["user_default_address"], key = "#userId")
  fun getUserDefaultAddress(userId: Long): AddressModel? = addressRepository.findByUserIdAndIsDefaultTrue(userId)

  /**
   * 根据ID获取地址
   */
  @Cacheable(value = ["address"], key = "#addressId")
  fun getAddressById(addressId: Long): AddressModel? = addressRepository.findById(addressId).orElse(null)

  /**
   * 创建新地址 (使用Address对象)
   */
  @CacheEvict(value = ["user_addresses", "user_default_address"], key = "#address.userId")
  fun createAddress(address: AddressModel): AddressModel {
    // 验证用户权限
    if (address.userId <= 0) {
      throw AquaException("无效的用户ID")
    }

    // 验证地址信息
    validateAddress(address)

    // 如果设置为默认地址，先取消其他默认地址
    if (address.isDefault) {
      addressRepository.clearDefaultAddresses(address.userId)
    }

    // 如果用户没有其他地址，自动设置为默认地址
    val userAddresses = addressRepository.findByUserId(address.userId)
    if (userAddresses.isEmpty()) {
      address.isDefault = true
    }

    // 如果没有经纬度，尝试通过地址解析获取
    if (address.longitude == null || address.latitude == null) {
      geolocationService.geocode(address)?.let { coordinates ->
        address.longitude = coordinates.first
        address.latitude = coordinates.second
      }
    }

    return addressRepository.save(address)
  }

  /**
   * 更新地址
   */
  @CacheEvict(
    value = ["user_addresses", "user_default_address", "address"],
    key = "#addressId",
    allEntries = true,
  )
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
  ): AddressModel? {
    val updates =
      AddressUpdateRequest(
        province = province,
        city = city,
        district = district,
        detailAddress = detailAddress,
        provinceCode = provinceCode,
        cityCode = cityCode,
        districtCode = districtCode,
        longitude = longitude,
        latitude = latitude,
        isDefault = isDefault,
      )
    return updateAddress(addressId, updates)
  }

  /**
   * 更新地址 (使用AddressUpdateRequest对象)
   */
  @CacheEvict(
    value = ["user_addresses", "user_default_address", "address"],
    key = "#addressId",
    allEntries = true,
  )
  fun updateAddress(
    addressId: Long,
    updates: AddressUpdateRequest,
  ): AddressModel? {
    val existingAddress =
      addressRepository
        .findById(addressId)
        .orElseThrow { AquaException("地址不存在") }

    // 更新字段
    updates.province?.let { existingAddress.province = it }
    updates.provinceCode?.let { existingAddress.provinceCode = it }
    updates.city?.let { existingAddress.city = it }
    updates.cityCode?.let { existingAddress.cityCode = it }
    updates.district?.let { existingAddress.district = it }
    updates.districtCode?.let { existingAddress.districtCode = it }
    updates.detailAddress?.let { existingAddress.detailAddress = it }
    updates.longitude?.let { existingAddress.longitude = it }
    updates.latitude?.let { existingAddress.latitude = it }

    // 验证更新后的地址
    validateAddress(existingAddress)

    // 如果设置为默认地址，先取消其他默认地址
    if (updates.isDefault == true && !existingAddress.isDefault) {
      addressRepository.clearDefaultAddresses(existingAddress.userId)
      existingAddress.isDefault = true
    }

    return addressRepository.save(existingAddress)
  }

  /**
   * 删除地址
   */
  @CacheEvict(
    value = ["user_addresses", "user_default_address", "address"],
    key = "#addressId",
    allEntries = true,
  )
  fun deleteAddress(
    addressId: Long,
    userId: Long,
  ): Boolean {
    val address =
      addressRepository
        .findById(addressId)
        .orElseThrow { AquaException("地址不存在") }

    if (address.userId != userId) {
      throw AquaException("无权限删除该地址")
    }

    // 如果删除的是默认地址，需要将其他地址设为默认
    if (address.isDefault) {
      val otherAddresses = addressRepository.findByUserIdAndIdNot(userId, addressId)
      if (otherAddresses.isNotEmpty()) {
        val newDefault = otherAddresses.first()
        newDefault.isDefault = true
        addressRepository.save(newDefault)
      }
    }

    addressRepository.deleteById(addressId)
    return true
  }

  /**
   * 设置默认地址
   */
  @CacheEvict(value = ["user_addresses", "user_default_address"], key = "#userId")
  fun setDefaultAddress(
    addressId: Long,
    userId: Long,
  ): Boolean {
    val address =
      addressRepository
        .findById(addressId)
        .orElseThrow { AquaException("地址不存在") }

    if (address.userId != userId) {
      throw AquaException("无权限设置该地址")
    }

    // 取消其他默认地址
    addressRepository.clearDefaultAddresses(userId)

    // 设置为默认地址
    address.isDefault = true
    addressRepository.save(address)

    return true
  }

  /**
   * 根据坐标获取附近地址
   */
  fun findNearbyAddresses(
    longitude: Double,
    latitude: Double,
    radiusKm: Double = 5.0,
  ): List<AddressModel> = addressRepository.findNearby(longitude, latitude, radiusKm)

  /**
   * 检查地址是否重复
   */
  fun isDuplicateAddress(
    userId: Long,
    address: AddressModel,
  ): Boolean =
    addressRepository.findByUserId(userId).any { existing ->
      existing.province == address.province &&
        existing.city == address.city &&
        existing.district == address.district &&
        existing.detailAddress == address.detailAddress
    }

  /**
   * 验证地址信息
   */
  private fun validateAddress(address: AddressModel) {
    if (address.province.isBlank()) {
      throw AquaException("省份不能为空")
    }
    if (address.city.isBlank()) {
      throw AquaException("城市不能为空")
    }
    if (address.district.isBlank()) {
      throw AquaException("区县不能为空")
    }
    if (address.detailAddress.isBlank()) {
      throw AquaException("详细地址不能为空")
    }
    if (address.detailAddress.length > 500) {
      throw AquaException("详细地址长度不能超过500字符")
    }

    // 验证经纬度范围
    address.longitude?.let { lng ->
      if (lng < -180.0 || lng > 180.0) {
        throw AquaException("经度范围无效")
      }
    }
    address.latitude?.let { lat ->
      if (lat < -90.0 || lat > 90.0) {
        throw AquaException("纬度范围无效")
      }
    }

    // 验证行政区划代码
    address.provinceCode?.let { code ->
      if (!regionRepository.existsByCodeAndLevel(code, 1)) {
        throw AquaException("无效的省份代码")
      }
    }
    address.cityCode?.let { code ->
      if (!regionRepository.existsByCodeAndLevel(code, 2)) {
        throw AquaException("无效的城市代码")
      }
    }
    address.districtCode?.let { code ->
      if (!regionRepository.existsByCodeAndLevel(code, 3)) {
        throw AquaException("无效的区县代码")
      }
    }
  }

  // Additional methods for compatibility with existing resolvers

  /**
   * 获取用户地址分页列表
   */
  fun findByUserId(userId: Long): List<AddressModel> {
    val addresses = addressRepository.findByUserIdOrderByIdDesc(userId)
    return addresses
  }

  /**
   * 获取用户默认地址
   */
  fun findDefaultByUserId(userId: Long): AddressModel? = addressRepository.findByUserIdAndIsDefaultTrue(userId)

  /**
   * 根据ID获取地址
   */
  fun findById(addressId: Long): AddressModel? = addressRepository.findById(addressId).orElse(null)

  /**
   * 搜索用户地址
   */
  fun searchByUserIdAndKeyword(
    userId: Long,
    keyword: String,
    pageable: PageRequest,
  ): Page<AddressModel> {
    val addresses = addressRepository.searchByUserIdAndKeyword(userId, keyword)
    val start = pageable.offset.toInt()
    val end = (start + pageable.pageSize).coerceAtMost(addresses.size)
    return if (start >= addresses.size) {
      Page.empty(pageable)
    } else {
      PageImpl(addresses.subList(start, end), pageable, addresses.size.toLong())
    }
  }

  /**
   * 获取用户地址数量
   */
  fun countByUserId(userId: Long): Int = addressRepository.countByUserId(userId)

  // Legacy method for backward compatibility
  fun save(address: AddressModel): AddressModel = addressRepository.save(address)

  // Legacy method for backward compatibility
  fun deleteById(addressId: Long) {
    addressRepository.deleteById(addressId)
  }

  // Legacy method for backward compatibility
  fun unsetDefaultAddresses(userId: Long) {
    addressRepository.clearDefaultAddresses(userId)
  }
}

/**
 * 地址更新请求
 */
data class AddressUpdateRequest(
  val province: String? = null,
  val provinceCode: String? = null,
  val city: String? = null,
  val cityCode: String? = null,
  val district: String? = null,
  val districtCode: String? = null,
  val detailAddress: String? = null,
  val longitude: Double? = null,
  val latitude: Double? = null,
  val isDefault: Boolean? = null,
)
