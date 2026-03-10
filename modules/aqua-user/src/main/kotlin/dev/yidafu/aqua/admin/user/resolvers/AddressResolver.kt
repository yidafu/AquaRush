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

import dev.yidafu.aqua.api.service.AddressService
import dev.yidafu.aqua.common.graphql.generated.Address
import dev.yidafu.aqua.common.graphql.generated.AddressInput
import dev.yidafu.aqua.common.graphql.generated.BatchImportAddressesResult
import dev.yidafu.aqua.common.graphql.generated.UpdateAddressInput
import dev.yidafu.aqua.user.mapper.AddressInputMapper
import dev.yidafu.aqua.user.mapper.AddressMapper
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Controller

@Controller
class AddressResolver(
  private val addressService: AddressService,
) {
  private val logger = LoggerFactory.getLogger(AddressResolver::class.java)
  @QueryMapping
  fun userAddresses(
    @Argument userId: Long,
  ): List<Address> {
    val list = addressService.findByUserId(userId)
    return AddressMapper.mapList(list)
  }

  @QueryMapping
  fun getMyAddresses(): List<Address> {
    val authentication = SecurityContextHolder.getContext().authentication
    val userId = authentication?.name!!.toLong()
    val addresses = addressService.getUserAddresses(userId)
    return AddressMapper.mapList(addresses)
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

    return address?.let { AddressMapper.map(it) }
  }

  @QueryMapping
  fun getDefaultAddress(): Address? {
    val authentication = SecurityContextHolder.getContext().authentication
    val userId = authentication!!.name.toLong()
    val defaultAddress = addressService.getUserDefaultAddress(userId)
    return defaultAddress?.let { AddressMapper.map(it) }
  }

  // Admin-only queries

  /**
   * 获取所有地址 (管理员)
   */
  @QueryMapping
  fun addresses(): List<Address> {
    val allAddresses = addressService.findAllAddresses()
    return AddressMapper.mapList(allAddresses)
  }

  /**
   * 根据ID获取地址 (管理员)
   */
  @QueryMapping
  fun address(
    @Argument id: Long,
  ): Address? {
    val address = addressService.getAddressById(id)
    return address?.let { AddressMapper.map(it) }
  }

  /**
   * 搜索用户地址 (管理员)
   */
  @QueryMapping
  fun searchUserAddresses(
    @Argument keyword: String,
    @Argument userId: Long,
    @Argument page: Int = 0,
    @Argument size: Int = 10,
  ): List<Address> {
    val pageable = org.springframework.data.domain.PageRequest.of(page, size)
    val addresses = addressService.searchByUserIdAndKeyword(userId, keyword, pageable)
    return AddressMapper.mapList(addresses.content)
  }

  /**
   * 搜索所有地址 (管理员)
   */
  @QueryMapping
  fun searchAllAddresses(
    @Argument keyword: String?,
    @Argument page: Int = 0,
    @Argument size: Int = 10,
  ): List<Address> {
    val pageable = org.springframework.data.domain.PageRequest.of(page, size)
    val addresses = addressService.searchAllAddresses(keyword, pageable)
    return AddressMapper.mapList(addresses.content)
  }

  // Admin-only mutations

  /**
   * 管理员创建地址 (userId 可以为 null)
   */
  @MutationMapping
  fun createAdminAddress(
    @Argument @Valid input: AddressInput,
  ): Address {
    try {
      val address = AddressInputMapper.map(input)
      // userId 保持为 null，由 AddressInput 中的值决定
      val savedAddress = addressService.save(address)
      logger.info("Admin created address with ID: ${savedAddress.id}")
      return AddressMapper.map(savedAddress)
    } catch (e: Exception) {
      logger.error("Failed to create admin address", e)
      throw RuntimeException("创建地址失败: ${e.message}")
    }
  }

  /**
   * 批量导入地址 (管理员)
   */
  @MutationMapping
  fun batchImportAddresses(
    @Argument input: List<AddressInput>,
  ): BatchImportAddressesResult {
    try {
      val totalCount = input.size
      var successCount = 0
      var failureCount = 0

      val addresses = input.mapIndexed { index, addressInput ->
        try {
          val address = AddressInputMapper.map(addressInput)
          // userId 保持为 null
          successCount++
          address
        } catch (e: Exception) {
          failureCount++
          logger.warn("Failed to parse address at index $index: ${e.message}")
          null
        }
      }.filterNotNull()

      // 批量保存
      if (addresses.isNotEmpty()) {
        addressService.saveAll(addresses)
      }

      logger.info("Batch import completed: $successCount success, $failureCount failed out of $totalCount")

      return BatchImportAddressesResult(
        successCount = successCount,
        failureCount = failureCount,
        totalCount = totalCount,
      )
    } catch (e: Exception) {
      logger.error("Failed to batch import addresses", e)
      throw RuntimeException("批量导入地址失败: ${e.message}")
    }
  }

  /**
   * 管理员删除地址 (不校验 userId)
   */
  @MutationMapping
  fun deleteAdminAddress(
    @Argument id: Long,
  ): Boolean {
    return addressService.deleteAddressById(id)
  }

  @MutationMapping
  fun createAddress(
    @Argument @Valid input: AddressInput,
  ): Address {
    val authentication = SecurityContextHolder.getContext().authentication
    val userId = authentication!!.name.toLong()
    val address = AddressInputMapper.map(input)
    address.userId = userId
    val createdAddress =
      addressService.createAddress(
        address,
      )

    return createdAddress.let { AddressMapper.map(it) }
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

    return updatedAddress?.let { AddressMapper.map(it) }
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
