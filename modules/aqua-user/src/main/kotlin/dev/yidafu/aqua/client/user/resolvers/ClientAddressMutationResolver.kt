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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package dev.yidafu.aqua.client.user.resolvers

import dev.yidafu.aqua.common.annotation.ClientService
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.graphql.generated.Address
import dev.yidafu.aqua.common.graphql.generated.AddressInput
import dev.yidafu.aqua.common.graphql.generated.UpdateAddressInput
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.common.domain.model.AddressModel
import dev.yidafu.aqua.user.mapper.AddressInputMapper
import dev.yidafu.aqua.user.mapper.AddressMapper
import dev.yidafu.aqua.user.mapper.AddressUpdateMapper
import dev.yidafu.aqua.user.mapper.merge
import dev.yidafu.aqua.api.service.AddressService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import org.springframework.transaction.annotation.Transactional

/**
 * 客户端地址变更解析器
 * 提供用户地址管理功能，用户只能管理自己的地址
 */
@ClientService
@Controller
class ClientAddressMutationResolver(
  private val addressService: AddressService,
) {
  private val logger = LoggerFactory.getLogger(ClientAddressMutationResolver::class.java)

  /**
   * 创建用户地址
   */
  @PreAuthorize("isAuthenticated()")
  @MutationMapping
  @Transactional
  fun createAddress(
    @Argument @Valid input: AddressInput,
    @AuthenticationPrincipal userPrincipal: UserPrincipal,
  ): Address {
    try {
      // 检查地址数量限制
      val currentAddressCount = addressService.countByUserId(userPrincipal.id)
      if (currentAddressCount >= 20) {
        throw BadRequestException("地址数量已达上限（最多20个地址）")
      }

      // 如果新地址设为默认地址，先取消其他默认地址
      if (input.isDefault == true) {
        addressService.unsetDefaultAddresses(userPrincipal.id)
      }

      val address = AddressInputMapper.map(input)
      address.userId = userPrincipal.id

      val savedAddress = addressService.save(address)

      // Ensure the saved address has an ID before returning
      if (savedAddress.id == null) {
        logger.error("Address was saved but ID is still null")
        throw BadRequestException("地址保存失败：ID 生成失败")
      }

      // Convert entity to GraphQL type
      val graphqlAddress =  AddressMapper.map(savedAddress)
      logger.info("Successfully created address for user: ${userPrincipal.id} with ID: ${graphqlAddress.id}")
      return graphqlAddress
    } catch (e: Exception) {
      logger.error("Failed to create user address", e)
      throw BadRequestException("创建地址失败: ${e.message}")
    }
  }

  /**
   * 更新用户地址
   */
  @PreAuthorize("isAuthenticated()")
  @Transactional
  @MutationMapping
  fun updateAddress(
    @Argument id: Long,
    @Argument @Valid input: UpdateAddressInput,
    @AuthenticationPrincipal userPrincipal: UserPrincipal,
  ): Address {
    try {
      // 验证地址存在且属于当前用户
      val existingAddress = addressService.findById(id)
      if (existingAddress == null || existingAddress.userId != userPrincipal.id) {
        throw IllegalArgumentException("地址不存在或无权访问")
      }

      // 检查是否需要设置默认地址（在应用更新之前检查）
      val willBeDefault = input.isDefault
      if (willBeDefault == true && !existingAddress.isDefault) {
        // 如果设为默认地址，先取消其他默认地址
        addressService.unsetDefaultAddresses(userPrincipal.id)
      }

      // 地址更新
      existingAddress.merge(input)

      val updatedAddress = addressService.save(existingAddress)
      logger.info("Successfully updated address: $id for user: ${userPrincipal.id}")
      return  AddressMapper.map(updatedAddress)
    } catch (e: Exception) {
      logger.error("Failed to update user address", e)
      throw BadRequestException("更新地址失败: ${e.message}")
    }
  }

  /**
   * 删除用户地址
   */
  @PreAuthorize("isAuthenticated()")
  @MutationMapping
  @Transactional
  fun deleteAddress(
    @Argument id: Long,
    @AuthenticationPrincipal userPrincipal: UserPrincipal,
  ): Boolean =
    try {
      // 验证地址存在且属于当前用户
      val existingAddress = addressService.findById(id)
      if (existingAddress == null || existingAddress.userId != userPrincipal.id) {
        throw IllegalArgumentException("地址不存在或无权访问")
      }
      // TODO: 检查地址是否有关联的未完成订单
      // 如果有关联订单，不允许删除

      addressService.deleteById(id)
      logger.info("Successfully deleted address: $id for user: ${userPrincipal.id}")
      true
    } catch (e: Exception) {
      logger.error("Failed to delete user address", e)
      throw BadRequestException("删除地址失败: ${e.message}")
    }

  /**
   * 设置默认地址
   */
  @PreAuthorize("isAuthenticated()")
  @Transactional
  @MutationMapping
  fun setDefaultAddress(
    @Argument id: Long,
    @AuthenticationPrincipal userPrincipal: UserPrincipal,
  ): Boolean =
    try {
      // 验证地址存在且属于当前用户
      val existingAddress = addressService.findById(id)
      if (existingAddress == null || existingAddress.userId != userPrincipal.id) {
        throw IllegalArgumentException("地址不存在或无权访问")
      }

      addressService.setDefaultAddress(existingAddress.id!!, userPrincipal.id)
      logger.info("Successfully set default address: $id for user: ${userPrincipal.id}")
      true
    } catch (e: Exception) {
      logger.error("Failed to set default address", e)
      throw BadRequestException("设置默认地址失败: ${e.message}")
    }

  /**
   * 复制地址
   */
  @PreAuthorize("isAuthenticated()")
  @Transactional
  fun copyUserAddress(
    id: Long,
    newName: String,
    @AuthenticationPrincipal userPrincipal: UserPrincipal,
  ): Address {
    return try {
      // 验证源地址存在且属于当前用户
      val sourceAddress = addressService.findById(id)
      if (sourceAddress == null || sourceAddress.userId != userPrincipal.id) {
        throw IllegalArgumentException("源地址不存在或无权访问")
      }

      // 创建 AddressInput 从源地址，然后转换为 AddressModel
      val addressInput =
        AddressInput(
          receiverName = sourceAddress.receiverName,
          phone = sourceAddress.phone,
          province = sourceAddress.province,
          provinceCode = sourceAddress.provinceCode,
          city = sourceAddress.city,
          cityCode = sourceAddress.cityCode,
          district = sourceAddress.district,
          districtCode = sourceAddress.districtCode,
          detailAddress = sourceAddress.detailAddress,
          longitude = sourceAddress.longitude?.toFloat(),
          latitude = sourceAddress.latitude?.toFloat(),
          isDefault = false,
        )

      val copiedAddress = AddressInputMapper.map(addressInput)

      val savedAddress = addressService.save(copiedAddress)
      logger.info("Successfully copied address: $id to new address for user: ${userPrincipal.id}")
      return AddressMapper.map(savedAddress)
    } catch (e: Exception) {
      logger.error("Failed to copy user address", e)
      throw BadRequestException("复制地址失败: ${e.message}")
    }
  }

}
