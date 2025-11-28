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

import dev.yidafu.aqua.user.domain.model.Address
import dev.yidafu.aqua.user.domain.repository.AddressRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AddressService(
  private val addressRepository: AddressRepository,
) {
  fun findByUserId(userId: Long): List<Address> = addressRepository.findByUserId(userId)

  fun findById(addressId: Long): Address? = addressRepository.findById(addressId).orElse(null)

  fun findDefaultAddress(userId: Long): Address? = addressRepository.findByUserIdAndIsDefault(userId, true)

  @Transactional
  fun createAddress(
    userId: Long,
    receiverName: String,
    phone: String,
    province: String,
    city: String,
    district: String,
    detailAddress: String,
    isDefault: Boolean = false,
  ): Address {
    // 如果设置为默认地址，需要先清除该用户的其他默认地址
    if (isDefault) {
      addressRepository.clearDefaultByUserId(userId)
    }

    val address =
      Address(
        userId = userId,
        receiverName = receiverName,
        phone = phone,
        province = province,
        city = city,
        district = district,
        detailAddress = detailAddress,
        isDefault = isDefault,
      )
    return addressRepository.save(address)
  }

  @Transactional
  fun updateAddress(
    addressId: Long,
    userId: Long,
    receiverName: String?,
    phone: String?,
    province: String?,
    city: String?,
    district: String?,
    detailAddress: String?,
    isDefault: Boolean?,
  ): Address {
    val address =
      addressRepository
        .findById(addressId)
        .orElseThrow { IllegalArgumentException("Address not found: $addressId") }

    if (address.userId != userId) {
      throw IllegalArgumentException("Address does not belong to user")
    }

    // 如果要设置为默认地址，先清除其他默认地址
    if (isDefault == true && !address.isDefault) {
      addressRepository.clearDefaultByUserId(userId)
    }

    receiverName?.let { address.receiverName = it }
    phone?.let { address.phone = it }
    province?.let { address.province = it }
    city?.let { address.city = it }
    district?.let { address.district = it }
    detailAddress?.let { address.detailAddress = it }
    isDefault?.let { address.isDefault = it }

    return addressRepository.save(address)
  }

  @Transactional
  fun deleteAddress(
    addressId: Long,
    userId: Long,
  ): Boolean {
    val count = addressRepository.deleteByIdAndUserId(addressId, userId)
    return count > 0
  }

  @Transactional
  fun setDefaultAddress(
    addressId: Long,
    userId: Long,
  ): Address {
    val address =
      addressRepository
        .findById(addressId)
        .orElseThrow { IllegalArgumentException("Address not found: $addressId") }

    if (address.userId != userId) {
      throw IllegalArgumentException("Address does not belong to user")
    }

    if (!address.isDefault) {
      addressRepository.clearDefaultByUserId(userId)
      address.isDefault = true
      return addressRepository.save(address)
    }

    return address
  }
}
