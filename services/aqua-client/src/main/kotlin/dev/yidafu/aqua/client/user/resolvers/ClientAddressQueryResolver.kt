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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package dev.yidafu.aqua.client.user.resolvers

import dev.yidafu.aqua.api.service.admin.AddressService
import dev.yidafu.aqua.common.annotation.ClientService
import dev.yidafu.aqua.common.graphql.generated.AddressVo
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.user.mapper.toGraphQL
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
  ): List<AddressVo> = addressService.findByUserId(userPrincipal.id).map { it.toGraphQL() }

  /**
   * 根据ID获取用户地址（只能查看自己的地址）
   */
  @PreAuthorize("isAuthenticated()")
  @QueryMapping
  fun address(
    @Argument id: Long,
    @AuthenticationPrincipal userPrincipal: UserPrincipal,
  ): AddressVo? {
    // 先获取地址，再验证所有权
    val address = addressService.findById(id)
    if (address != null && address.userId != userPrincipal.id) {
      throw IllegalArgumentException("无权访问此地址")
    }
    return address?.toGraphQL()
  }
}
