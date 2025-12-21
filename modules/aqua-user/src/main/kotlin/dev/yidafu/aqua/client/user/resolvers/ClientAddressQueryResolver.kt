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

import dev.yidafu.aqua.client.user.resolvers.ClientAddressQueryResolver.Companion.AddressWithDistance
import dev.yidafu.aqua.common.annotation.ClientService
import dev.yidafu.aqua.common.graphql.generated.Address
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.common.domain.model.AddressModel
import dev.yidafu.aqua.user.mapper.AddressMapper
import dev.yidafu.aqua.api.service.AddressService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

/**
 * 客户端地址查询解析器
 * 提供用户地址查询功能，用户只能管理自己的地址
 */
@ClientService
@Controller
class ClientAddressQueryResolver(
  private val addressService: AddressService,
) {
  /**
   * 获取用户的所有地址
   */
  @QueryMapping
  @PreAuthorize("isAuthenticated()")
  fun userAddresses(
    @AuthenticationPrincipal userPrincipal: UserPrincipal,
  ): List<Address> = AddressMapper.mapList(addressService.findByUserId(userPrincipal.id))

  /**
   * 获取用户的默认地址
   */
  @PreAuthorize("isAuthenticated()")
  @QueryMapping
  fun userDefaultAddress(
    @AuthenticationPrincipal userPrincipal: UserPrincipal,
  ): Address? = addressService.findDefaultByUserId(userPrincipal.id)?.let { AddressMapper.map(it) }

  /**
   * 根据ID获取用户地址（只能查看自己的地址）
   */
  @PreAuthorize("isAuthenticated()")
  @QueryMapping
  fun address(
    @Argument id: Long,
    @AuthenticationPrincipal userPrincipal: UserPrincipal,
  ): Address? {
    // 先获取地址，再验证所有权
    val address = addressService.findById(id)
    if (address != null && address.userId != userPrincipal.id) {
      throw IllegalArgumentException("无权访问此地址")
    }
    return address?.let { AddressMapper.map(it) }
  }

  /**
   * 搜索用户地址
   */
  @PreAuthorize("isAuthenticated()")
  fun searchUserAddresses(
    keyword: String,
    page: Int = 0,
    size: Int = 10,
    @AuthenticationPrincipal userPrincipal: UserPrincipal,
  ): Page<Address> {
    val pageable = PageRequest.of(page, size)
    return addressService
      .searchByUserIdAndKeyword(userPrincipal.id, keyword, pageable)
      .map { it.let { AddressMapper.map(it) } }
  }

  /**
   * 获取用户地址数量
   */
  @PreAuthorize("isAuthenticated()")
  @QueryMapping
  fun userAddressCount(
    @AuthenticationPrincipal userPrincipal: UserPrincipal,
  ): Int = addressService.countByUserId(userPrincipal.id)

  /**
   * 获取地址附近的配送员（基于用户地址）
   */
  @PreAuthorize("isAuthenticated()")
  fun getNearbyDeliveryWorkers(
    addressId: Long,
    radiusKm: Double = 5.0,
    limit: Int = 10,
    @AuthenticationPrincipal userPrincipal: UserPrincipal,
  ): List<DeliveryWorkerInfo> {
    // 验证地址所有权
    val address = addressService.findById(addressId)
    if (address == null || address.userId != userPrincipal.id) {
      throw IllegalArgumentException("无权访问此地址")
    }

    // TODO: 实现从服务获取附近配送员
    // 目前返回空列表
    return emptyList()
  }

  /**
   * 根据坐标搜索附近地址
   */
  @PreAuthorize("isAuthenticated()")
  fun getNearbyAddresses(
    longitude: Double,
    latitude: Double,
    radiusKm: Double = 2.0,
    limit: Int = 20,
    @AuthenticationPrincipal userPrincipal: UserPrincipal,
  ): List<AddressWithDistance> {
    // TODO: 实现从服务获取附近地址
    // 目前返回空列表
    return emptyList()
  }

  /**
   * 验证地址是否在配送范围内
   */
  @PreAuthorize("isAuthenticated()")
  fun validateAddressForDelivery(
    addressId: Long,
    @AuthenticationPrincipal userPrincipal: UserPrincipal,
  ): DeliveryValidationResult {
    // 验证地址所有权
    val address = addressService.findById(addressId)
    if (address == null || address.userId != userPrincipal.id) {
      throw IllegalArgumentException("无权访问此地址")
    }

    // TODO: 实现从服务验证地址是否在配送范围内
    // 目前返回默认验证结果
    return DeliveryValidationResult(
      isValid = true,
      isInRange = true,
      estimatedDeliveryTime = "30-45分钟",
      deliveryFee = java.math.BigDecimal("5.00"),
      message = "地址在配送范围内",
      suggestedAlternatives = emptyList(),
    )
  }

  companion object {
    /**
     * 地址相关类型定义
     */
    data class AddressWithDistance(
      val address: Address,
      val distanceKm: Double,
      val estimatedDeliveryTime: String,
    )

    data class DeliveryWorkerInfo(
      val id: Long,
      val name: String,
      val phone: String,
      val avatarUrl: String?,
      val rating: Double,
      val distanceKm: Double,
      val estimatedDeliveryTime: String,
      val isOnline: Boolean,
    )

    data class DeliveryValidationResult(
      val isValid: Boolean,
      val isInRange: Boolean,
      val estimatedDeliveryTime: String?,
      val deliveryFee: java.math.BigDecimal?,
      val message: String,
      val suggestedAlternatives: List<Address>,
    )
  }
}
