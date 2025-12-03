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
import dev.yidafu.aqua.common.graphql.generated.CreateAddressInput
import dev.yidafu.aqua.common.graphql.generated.UpdateAddressInput
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.user.domain.model.Address
import dev.yidafu.aqua.user.service.AddressService
import org.slf4j.LoggerFactory
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import org.springframework.transaction.annotation.Transactional
import jakarta.validation.Valid

/**
 * 客户端地址变更解析器
 * 提供用户地址管理功能，用户只能管理自己的地址
 */
@ClientService
@Controller
class ClientAddressMutationResolver(
    private val addressService: AddressService
) {
    private val logger = LoggerFactory.getLogger(ClientAddressMutationResolver::class.java)

    /**
     * 创建用户地址
     */
    @PreAuthorize("isAuthenticated()")
    @Transactional
    fun createUserAddress(
      @Valid input: CreateAddressInput,
      @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): Address {
        try {
            // 验证输入
            validateCreateAddressInput(input)

            val address = Address(
                userId = userPrincipal.id,
                province = input.province,
                city = input.city,
                district = input.district,
                detailAddress = input.detailAddress,
                postalCode = input.postalCode,
                longitude = input.longitude?.toDouble(),
                latitude = input.latitude?.toDouble(),
                isDefault = input.isDefault ?: false
            )

            val savedAddress = addressService.save(address)
            logger.info("Successfully created address for user: ${userPrincipal.id}")
            return savedAddress
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
    fun updateUserAddress(
      id: Long,
      @Valid input: UpdateAddressInput,
      @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): Address {
        try {
            // 验证地址存在且属于当前用户
            val existingAddress = addressService.findById(id)
            if (existingAddress == null || existingAddress.userId != userPrincipal.id) {
                throw IllegalArgumentException("地址不存在或无权访问")
            }

            // 验证输入
            validateUpdateAddressInput(input)

            // 更新地址字段
            input.province?.let { existingAddress.province = it }
            input.city?.let { existingAddress.city = it }
            input.district?.let { existingAddress.district = it }
            input.detailAddress?.let { existingAddress.detailAddress = it }
            input.postalCode?.let { existingAddress.postalCode = it }
            input.longitude?.let { existingAddress.longitude = it.toDouble() }
            input.latitude?.let { existingAddress.latitude = it.toDouble() }
            input.isDefault?.let {
                if ((it as Boolean?) == true && !existingAddress.isDefault) {
                    // 如果设为默认地址，先取消其他默认地址
                    addressService.unsetDefaultAddresses(userPrincipal.id)
                }
                existingAddress.isDefault = (it as Boolean?) ?: false
            }

            val updatedAddress = addressService.save(existingAddress)
            logger.info("Successfully updated address: $id for user: ${userPrincipal.id}")
            return updatedAddress
        } catch (e: Exception) {
            logger.error("Failed to update user address", e)
            throw BadRequestException("更新地址失败: ${e.message}")
        }
    }

    /**
     * 删除用户地址
     */
    @PreAuthorize("isAuthenticated()")
    @Transactional
    fun deleteUserAddress(
        id: Long,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): Boolean {
        return try {
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
    }

    /**
     * 设置默认地址
     */
    @PreAuthorize("isAuthenticated()")
    @Transactional
    fun setDefaultUserAddress(
        id: Long,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): Boolean {
        return try {
            // 验证地址存在且属于当前用户
            val existingAddress = addressService.findById(id)
            if (existingAddress == null || existingAddress.userId != userPrincipal.id) {
                throw IllegalArgumentException("地址不存在或无权访问")
            }

            addressService.setDefaultAddress(userPrincipal.id, id)
            logger.info("Successfully set default address: $id for user: ${userPrincipal.id}")
            true
        } catch (e: Exception) {
            logger.error("Failed to set default address", e)
            throw BadRequestException("设置默认地址失败: ${e.message}")
        }
    }

    /**
     * 复制地址
     */
    @PreAuthorize("isAuthenticated()")
    @Transactional
    fun copyUserAddress(
        id: Long,
        newName: String,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): Address {
        return try {
            // 验证源地址存在且属于当前用户
            val sourceAddress = addressService.findById(id)
            if (sourceAddress == null || sourceAddress.userId != userPrincipal.id) {
                throw IllegalArgumentException("源地址不存在或无权访问")
            }

            val copiedAddress = Address(
                userId = userPrincipal.id,
                province = sourceAddress.province,
                city = sourceAddress.city,
                district = sourceAddress.district,
                detailAddress = sourceAddress.detailAddress,
                postalCode = sourceAddress.postalCode,
                longitude = sourceAddress.longitude,
                latitude = sourceAddress.latitude,
                isDefault = false
            )

            val savedAddress = addressService.save(copiedAddress)
            logger.info("Successfully copied address: $id to new address for user: ${userPrincipal.id}")
            return savedAddress
        } catch (e: Exception) {
            logger.error("Failed to copy user address", e)
            throw BadRequestException("复制地址失败: ${e.message}")
        }
    }

    /**
     * 批量删除地址
     */
    @PreAuthorize("isAuthenticated()")
    @Transactional
    fun deleteUserAddresses(
        ids: List<Long>,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): Boolean {
        return try {
            if (ids.isEmpty()) {
                throw BadRequestException("地址ID列表不能为空")
            }
            if (ids.size > 10) {
                throw BadRequestException("一次最多只能删除10个地址")
            }

            // 验证所有地址都属于当前用户
            ids.forEach { id ->
                val address = addressService.findById(id)
                if (address == null || address.userId != userPrincipal.id) {
                    throw IllegalArgumentException("地址不存在或无权访问: $id")
                }
            }

            ids.forEach { id ->
                addressService.deleteById(id)
            }

            logger.info("Successfully deleted ${ids.size} addresses for user: ${userPrincipal.id}")
            true
        } catch (e: Exception) {
            logger.error("Failed to delete user addresses", e)
            throw BadRequestException("批量删除地址失败: ${e.message}")
        }
    }

    /**
     * 验证创建地址输入
     */
    private fun validateCreateAddressInput(input: CreateAddressInput) {
        if (input.province.isBlank()) {
            throw BadRequestException("省份不能为空")
        }
        if (input.city.isBlank()) {
            throw BadRequestException("城市不能为空")
        }
        if (input.district.isBlank()) {
            throw BadRequestException("区县不能为空")
        }
        if (input.detailAddress.isBlank()) {
            throw BadRequestException("详细地址不能为空")
        }
        if (input.detailAddress.length > 200) {
            throw BadRequestException("详细地址长度不能超过200个字符")
        }
                input.postalCode?.let { postalCode ->
            if (postalCode.length > 10) {
                throw BadRequestException("邮编长度不能超过10个字符")
            }
        }
    }

    /**
     * 验证更新地址输入
     */
    private fun validateUpdateAddressInput(input: UpdateAddressInput) {
        input.province?.let { province ->
            if (province.isBlank()) {
                throw BadRequestException("省份不能为空")
            }
            if (province.length > 50) {
                throw BadRequestException("省份长度不能超过50个字符")
            }
        }

        input.detailAddress?.let { detailAddress ->
            if (detailAddress.isBlank()) {
                throw BadRequestException("详细地址不能为空")
            }
            if (detailAddress.length > 200) {
                throw BadRequestException("详细地址长度不能超过200个字符")
            }
        }


        input.postalCode?.let { postalCode ->
            if (postalCode.length > 10) {
                throw BadRequestException("邮编长度不能超过10个字符")
            }
        }
    }

    /**
     * 验证手机号格式（简单验证）
     */
    private fun isValidPhone(phone: String): Boolean {
        return phone.matches(Regex("^1[3-9]\\d{9}$"))
    }

    companion object {



    }
}
