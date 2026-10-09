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

package dev.yidafu.aqua.marketing.barrel.resolvers

import dev.yidafu.aqua.api.dto.CreateBarrelFeeRequest
import dev.yidafu.aqua.api.dto.UpdateBarrelFeeRequest
import dev.yidafu.aqua.api.service.admin.AdminService
import dev.yidafu.aqua.api.service.barrel.BarrelFeeMutationService
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.exception.UnauthorizedException
import dev.yidafu.aqua.common.graphql.BaseGraphQLResolver
import dev.yidafu.aqua.common.graphql.generated.BarrelFee
import dev.yidafu.aqua.common.graphql.generated.CreateBarrelFeeInput
import dev.yidafu.aqua.common.graphql.generated.UpdateBarrelFeeInput
import dev.yidafu.aqua.marketing.barrel.mapper.toGraphQL
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Controller

/**
 * 管理端水桶费价目变更解析器(G06 水桶费管理-价目表 CRUD/停用)
 */
@Controller
class AdminBarrelFeeMutationResolver(
  private val barrelFeeMutationService: BarrelFeeMutationService,
  private val adminService: AdminService,
) : BaseGraphQLResolver() {
  private val logger = LoggerFactory.getLogger(AdminBarrelFeeMutationResolver::class.java)

  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_PAYMENT_WRITE')")
  fun createBarrelFee(
    @Argument @Valid input: CreateBarrelFeeInput,
    @AuthenticationPrincipal userDetails: UserDetails?,
  ): BarrelFee {
    requireLogin(userDetails)
    val fee =
      barrelFeeMutationService.create(
        CreateBarrelFeeRequest(
          specification = input.specification,
          priceCents = input.priceCents,
          sortOrder = input.sortOrder,
        ),
      )
    logger.info("Admin {} 新增水桶费价目: {}", userDetails?.username, input.specification)
    return fee.toGraphQL()
  }

  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_PAYMENT_WRITE')")
  fun updateBarrelFee(
    @Argument id: Long,
    @Argument @Valid input: UpdateBarrelFeeInput,
    @AuthenticationPrincipal userDetails: UserDetails?,
  ): BarrelFee {
    requireLogin(userDetails)
    val fee =
      barrelFeeMutationService.update(
        id,
        UpdateBarrelFeeRequest(
          specification = input.specification,
          priceCents = input.priceCents,
          sortOrder = input.sortOrder,
        ),
      )
    logger.info("Admin {} 更新水桶费价目: id={}", userDetails?.username, id)
    return fee.toGraphQL()
  }

  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_PAYMENT_WRITE')")
  fun disableBarrelFee(
    @Argument id: Long,
    @AuthenticationPrincipal userDetails: UserDetails?,
  ): BarrelFee {
    requireLogin(userDetails)
    val fee = barrelFeeMutationService.disable(id)
    logger.info("Admin {} 停用水桶费价目: id={}", userDetails?.username, id)
    return fee.toGraphQL()
  }

  private fun requireLogin(userDetails: UserDetails?) {
    if (userDetails == null) {
      throw UnauthorizedException("请先登录")
    }
    if (adminService.findByUsername(userDetails.username) == null) {
      throw BadRequestException("管理员不存在")
    }
  }
}
