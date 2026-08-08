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

import dev.yidafu.aqua.api.service.admin.AddressService
import dev.yidafu.aqua.common.domain.model.AddressModel
import dev.yidafu.aqua.common.messaging.service.SimplifiedEventPublishService
import dev.yidafu.aqua.user.domain.repository.AddressRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class AddressServiceImpl(
  private val addressRepository: AddressRepository,
  private val eventPublishService: SimplifiedEventPublishService,
) : AddressService {
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
    val address =
      addressRepository.findById(addressId).orElse(null)
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

  override fun findByUserId(userId: Long): List<AddressModel> = addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId)

  override fun findById(addressId: Long): AddressModel? = addressRepository.findById(addressId).orElse(null)

  /**
   * 搜索所有地址 (管理员功能)
   */
  override fun searchAllAddresses(
    keyword: String?,
    pageable: PageRequest,
  ): Page<AddressModel> = addressRepository.searchAllAddresses(keyword, pageable)

  override fun countByUserId(userId: Long): Int = addressRepository.countByUserId(userId)

  // Legacy method for backward compatibility
  @CacheEvict(
    value = ["user_addresses", "user_default_address", "address"],
    allEntries = true,
  )
  override fun save(address: AddressModel): AddressModel {
    val saved = addressRepository.save(address)
    // 发布地址更新事件
    eventPublishService.publishAddressUpdate(address.userId, saved.id!!)
    return saved
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

  /**
   * 批量保存地址列表 (管理员功能)
   */
  @CacheEvict(
    value = ["user_addresses", "user_default_address", "address"],
    allEntries = true,
  )
  override fun saveAll(addresses: List<AddressModel>): List<AddressModel> = addressRepository.saveAll(addresses)

  /**
   * 获取所有地址 (管理员功能)
   */
  override fun findAllAddresses(): List<AddressModel> = addressRepository.findAll()

  /**
   * 根据ID删除地址 (管理员功能，不校验userId)
   */
  @CacheEvict(
    value = ["user_addresses", "user_default_address", "address"],
    allEntries = true,
  )
  override fun deleteAddressById(addressId: Long): Boolean =
    try {
      addressRepository.deleteById(addressId)
      true
    } catch (e: Exception) {
      false
    }
}
