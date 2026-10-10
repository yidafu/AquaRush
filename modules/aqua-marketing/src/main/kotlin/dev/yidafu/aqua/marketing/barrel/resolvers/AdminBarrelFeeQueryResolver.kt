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

import dev.yidafu.aqua.api.service.barrel.BarrelFeeQueryService
import dev.yidafu.aqua.common.exception.UnauthorizedException
import dev.yidafu.aqua.common.graphql.BaseGraphQLResolver
import dev.yidafu.aqua.common.graphql.generated.BarrelFeeListInput
import dev.yidafu.aqua.common.graphql.generated.BarrelFeeVoPage
import dev.yidafu.aqua.marketing.barrel.mapper.toGraphQL
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Controller
import dev.yidafu.aqua.common.domain.model.enums.BarrelFeeStatus as BarrelFeeStatusModel

/**
 * 管理端水桶费价目查询解析器(G06 水桶费管理-价目表)
 */
@Controller
class AdminBarrelFeeQueryResolver(
  private val barrelFeeQueryService: BarrelFeeQueryService,
) : BaseGraphQLResolver() {
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_PAYMENT_READ')")
  fun barrelFees(
    @Argument input: BarrelFeeListInput?,
    @AuthenticationPrincipal userDetails: UserDetails?,
  ): BarrelFeeVoPage {
    if (userDetails == null) {
      throw UnauthorizedException("请先登录")
    }
    val result =
      barrelFeeQueryService.list(
        status = input?.status?.let { it.toDomain() },
        keyword = input?.keyword,
        page = input?.page ?: 0,
        size = input?.size ?: 20,
      )
    return BarrelFeeVoPage(
      content = result.content.map { it.toGraphQL() },
      totalElements = result.totalElements.toInt(),
      totalPages = result.totalPages,
      size = result.size,
      number = result.number,
    )
  }
}

private fun dev.yidafu.aqua.common.graphql.generated.BarrelFeeStatus.toDomain(): BarrelFeeStatusModel =
  when (this) {
    dev.yidafu.aqua.common.graphql.generated.BarrelFeeStatus.DISABLED -> BarrelFeeStatusModel.DISABLED
    dev.yidafu.aqua.common.graphql.generated.BarrelFeeStatus.ENABLED -> BarrelFeeStatusModel.ENABLED
  }
