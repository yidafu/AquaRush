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

package dev.yidafu.aqua.admin.user.resolvers

import dev.yidafu.aqua.common.graphql.generated.*
import dev.yidafu.aqua.user.domain.model.Address
import dev.yidafu.aqua.user.service.AddressService
import jakarta.validation.Valid
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.data.domain.PageRequest
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Controller

@Controller
class AddressResolver(
  private val addressService: AddressService,
) {
  @QueryMapping
  fun getMyAddresses(): List<Address> {
    val authentication = SecurityContextHolder.getContext().authentication
    val userId = authentication?.name!!.toLong()
    val addresses = addressService.getUserAddresses(userId)
    return addresses
  }

  @QueryMapping
  fun getAddressById(
    @Argument id: Long,
  ): Address? {
    val authentication = SecurityContextHolder.getContext().authentication
    val address = addressService.getAddressById(id)
    val userId = authentication!!.name.toLong()

    // Check if user owns this address or is admin
    if (address != null &&
      !authentication.authorities.any { it.authority == "ROLE_ADMIN" } &&
      address.userId != userId
    ) {
      throw AccessDeniedException("无权访问此地址")
    }

    return address
  }

  @QueryMapping
  fun getDefaultAddress(): Address? {
    val authentication = SecurityContextHolder.getContext().authentication
    val userId = authentication!!.name.toLong()
    val defaultAddress = addressService.getUserDefaultAddress(userId)
    return defaultAddress
  }

  @MutationMapping
  fun createAddress(
    @Argument @Valid input: AddressInput,
  ): Address {
    val authentication = SecurityContextHolder.getContext().authentication
    val userId = authentication!!.name.toLong()
    val request = input

    val createdAddress =
      addressService.createAddress(
        userId = userId,
        province = request.province.toString(),
        city = request.city.toString(),
        district = request.district.toString(),
        detailAddress = request.detailAddress.toString(),
        provinceCode = request.provinceCode?.toString(),
        cityCode = request.cityCode?.toString(),
        districtCode = request.districtCode?.toString(),
        longitude = request.longitude?.toDouble(),
        latitude = request.latitude?.toDouble(),
        isDefault = (request.isDefault as Boolean?) ?: false,
      )

    return createdAddress
  }

  @MutationMapping
  fun updateAddress(
    @Argument id: Long,
    @Argument @Valid input: UpdateAddressInput,
  ): Address? {
    val authentication = SecurityContextHolder.getContext().authentication
    val userId = authentication!!.name.toLong()
    val request = input

    val updatedAddress =
      addressService.updateAddress(
        addressId = id,
        userId = userId,
        province = request.province?.toString(),
        city = request.city?.toString(),
        district = request.district?.toString(),
        detailAddress = request.detailAddress?.toString(),
        provinceCode = request.provinceCode?.toString(),
        cityCode = request.cityCode?.toString(),
        districtCode = request.districtCode?.toString(),
        longitude = request.longitude?.toDouble(),
        latitude = request.latitude?.toDouble(),
        isDefault = (request.isDefault as Boolean?) ?: false,
      )

    return updatedAddress
  }

  @MutationMapping
  fun setDefaultAddress(
    @Argument id: Long,
  ): Boolean {
    val authentication = SecurityContextHolder.getContext().authentication
    val userId = authentication!!.name.toLong()

    return addressService.setDefaultAddress(id, userId)
  }

  @MutationMapping
  fun deleteAddress(
    @Argument id: Long,
  ): Boolean {
    val authentication = SecurityContextHolder.getContext().authentication
    val userId = authentication!!.name.toLong()

    return addressService.deleteAddress(id, userId)
  }
}
