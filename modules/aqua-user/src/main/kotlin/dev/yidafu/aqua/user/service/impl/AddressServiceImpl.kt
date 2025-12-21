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

package dev.yidafu.aqua.user.service.impl

import dev.yidafu.aqua.api.service.AddressService
import dev.yidafu.aqua.common.domain.model.AddressModel
import dev.yidafu.aqua.common.dto.AddressUpdateRequest
import dev.yidafu.aqua.common.exception.NotFoundException
import dev.yidafu.aqua.user.domain.exception.AquaException
import dev.yidafu.aqua.user.domain.repository.AddressRepository
import dev.yidafu.aqua.user.domain.repository.RegionRepository
import dev.yidafu.aqua.user.service.GeolocationService
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class AddressServiceImpl(
  private val addressRepository: AddressRepository,
  private val regionRepository: RegionRepository,
  private val geolocationService: GeolocationService,
) : AddressService {
  /**
   * 获取用户的所有地址
   */
  @Cacheable(value = ["user_addresses"], key = "#userId")
  override fun getUserAddresses(userId: Long): List<AddressModel> = addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId)

  /**
   * 获取用户默认地址
   */
  @Cacheable(value = ["user_default_address"], key = "#userId")
  override fun getUserDefaultAddress(userId: Long): AddressModel? = addressRepository.findByUserIdAndIsDefaultTrue(userId)

  /**
   * 根据ID获取地址
   */
  @Cacheable(value = ["address"], key = "#addressId")
  override fun getAddressById(addressId: Long): AddressModel? = addressRepository.findById(addressId).orElse(null)

  /**
   * 创建新地址 (使用Address对象)
   */
  @CacheEvict(value = ["user_addresses", "user_default_address"], key = "#address.userId")
  override fun createAddress(address: AddressModel): AddressModel {
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
  override fun updateAddress(
    addressId: Long,
    updates: AddressUpdateRequest,
  ): AddressModel? {
    val address = findById(addressId) ?: throw NotFoundException("Address not found")

    // Apply updates
    updates.province?.let { address.province = it }
    updates.city?.let { address.city = it }
    updates.district?.let { address.district = it }
    updates.detailAddress?.let { address.detailAddress = it }
    updates.provinceCode?.let { address.provinceCode = it }
    updates.cityCode?.let { address.cityCode = it }
    updates.districtCode?.let { address.districtCode = it }
    updates.longitude?.let { address.longitude = it }
    updates.latitude?.let { address.latitude = it }
    updates.isDefault?.let { if (it) setAsDefault(address.id!!, address.userId) }

    return save(address)
  }

  /**
   * 更新地址 (使用参数)
   */
  @CacheEvict(
    value = ["user_addresses", "user_default_address", "address"],
    key = "#addressId",
    allEntries = true,
  )
  override fun updateAddress(
    addressId: Long,
    userId: Long,
    province: String?,
    city: String?,
    district: String?,
    detailAddress: String?,
    provinceCode: String?,
    cityCode: String?,
    districtCode: String?,
    longitude: Double?,
    latitude: Double?,
    isDefault: Boolean?,
  ): AddressModel? {
    val address = addressRepository.findById(addressId).orElse(null)
      ?: throw NotFoundException("Address not found")

    if (address.userId != userId) {
      throw NotFoundException("Address not found for user")
    }

    // Apply updates
    province?.let { address.province = it }
    city?.let { address.city = it }
    district?.let { address.district = it }
    detailAddress?.let { address.detailAddress = it }
    provinceCode?.let { address.provinceCode = it }
    cityCode?.let { address.cityCode = it }
    districtCode?.let { address.districtCode = it }
    longitude?.let { address.longitude = it }
    latitude?.let { address.latitude = it }
    isDefault?.let { if (it) setDefaultAddress(address.id!!, address.userId) }

    return save(address)
  }

  /**
   * 删除地址
   */
  @CacheEvict(
    value = ["user_addresses", "user_default_address", "address"],
    allEntries = true,
  )
  override fun deleteAddress(
    addressId: Long,
    userId: Long,
  ): Boolean {
    val address = addressRepository.findById(addressId).orElse(null)
      ?: return false

    if (address.userId != userId) {
      return false
    }

    addressRepository.delete(address)

    // 如果删除的是默认地址，需要设置其他地址为默认
    if (address.isDefault) {
      val remainingAddresses = addressRepository.findByUserId(userId)
      if (remainingAddresses.isNotEmpty()) {
        val newDefault = remainingAddresses.first()
        newDefault.isDefault = true
        addressRepository.save(newDefault)
      }
    }

    return true
  }

  /**
   * 设置默认地址
   */
  @CacheEvict(
    value = ["user_addresses", "user_default_address"],
    key = "#userId",
    allEntries = true,
  )
  override fun setDefaultAddress(
    addressId: Long,
    userId: Long,
  ): Boolean {
    val address = addressRepository.findById(addressId).orElse(null)
      ?: return false

    if (address.userId != userId) {
      return false
    }

    // 取消其他默认地址
    addressRepository.clearDefaultAddresses(userId)

    // 设置新的默认地址
    address.isDefault = true
    addressRepository.save(address)

    return true
  }

  /**
   * 根据坐标获取附近地址
   */
  @Cacheable(value = ["nearby_addresses"], key = "#longitude + '_' + #latitude + '_' + #radiusKm")
  override fun findNearbyAddresses(
    longitude: Double,
    latitude: Double,
    radiusKm: Double,
  ): List<AddressModel> {
    // For now, return empty list as the repository method may not be implemented yet
    return emptyList()
  }

  /**
   * 检查地址是否重复
   */
  @Cacheable(value = ["duplicate_address_check"], key = "#userId + '_' + #address.hashCode()")
  override fun isDuplicateAddress(
    userId: Long,
    address: AddressModel,
  ): Boolean {
    val existingAddresses = addressRepository.findByUserId(userId)
    return existingAddresses.any { existing ->
      existing.province == address.province &&
      existing.city == address.city &&
      existing.district == address.district &&
      existing.detailAddress == address.detailAddress
    }
  }

  // Additional methods for compatibility with existing resolvers

  override fun findByUserId(userId: Long): List<AddressModel> {
    return addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId)
  }

  override fun findDefaultByUserId(userId: Long): AddressModel? {
    return addressRepository.findByUserId(userId).find { it.isDefault }
  }

  override fun findById(addressId: Long): AddressModel? {
    return addressRepository.findById(addressId).orElse(null)
  }

  override fun searchByUserIdAndKeyword(
    userId: Long,
    keyword: String,
    pageable: PageRequest,
  ): Page<AddressModel> {
    // For now, return all user addresses filtered by keyword
    val allAddresses = addressRepository.findByUserId(userId)
    val filteredAddresses = allAddresses.filter { address ->
      address.province?.contains(keyword, ignoreCase = true) == true ||
      address.city?.contains(keyword, ignoreCase = true) == true ||
      address.district?.contains(keyword, ignoreCase = true) == true ||
      address.detailAddress?.contains(keyword, ignoreCase = true) == true
    }

    val start = pageable.offset.toInt()
    val end = (start + pageable.pageSize).coerceAtMost(filteredAddresses.size)

    return if (start >= filteredAddresses.size) {
      Page.empty(pageable)
    } else {
      PageImpl(filteredAddresses.subList(start, end), pageable, filteredAddresses.size.toLong())
    }
  }

  override fun countByUserId(userId: Long): Int {
    return addressRepository.countByUserId(userId)
  }

  // Legacy method for backward compatibility
  @CacheEvict(
    value = ["user_addresses", "user_default_address", "address"],
    allEntries = true,
  )
  override fun save(address: AddressModel): AddressModel {
    return addressRepository.save(address)
  }

  // Legacy method for backward compatibility
  @CacheEvict(
    value = ["user_addresses", "user_default_address", "address"],
    allEntries = true,
  )
  override fun deleteById(addressId: Long) {
    addressRepository.deleteById(addressId)
  }

  // Legacy method for backward compatibility
  @CacheEvict(
    value = ["user_addresses", "user_default_address"],
    allEntries = true,
  )
  override fun unsetDefaultAddresses(userId: Long) {
    addressRepository.clearDefaultAddresses(userId)
  }

  // Private helper methods

  private fun validateAddress(address: AddressModel) {
    if (address.province.isNullOrBlank() || address.city.isNullOrBlank()) {
      throw AquaException("Province and city are required")
    }
  }

  private fun setAsDefault(addressId: Long, userId: Long) {
    addressRepository.clearDefaultAddresses(userId)
    val address = addressRepository.findById(addressId).orElse(null)
    address?.let {
      it.isDefault = true
      addressRepository.save(it)
    }
  }

}
