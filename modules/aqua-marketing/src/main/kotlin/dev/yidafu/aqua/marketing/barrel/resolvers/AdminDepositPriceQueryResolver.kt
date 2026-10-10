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

import dev.yidafu.aqua.api.service.deposit.DepositPriceQueryService
import dev.yidafu.aqua.common.annotation.AdminService
import dev.yidafu.aqua.common.graphql.BaseGraphQLResolver
import dev.yidafu.aqua.common.graphql.generated.DepositPriceListInput
import dev.yidafu.aqua.common.graphql.generated.DepositPriceVoPage
import dev.yidafu.aqua.marketing.barrel.mapper.toDomain
import dev.yidafu.aqua.marketing.barrel.mapper.toGraphQL
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

/**
 * 管理端押金价目查询(R-MON-03 后台押金管理「价目」;PERMISSION_PRODUCT_READ)
 */
@Controller
@AdminService
class AdminDepositPriceQueryResolver(
  private val depositPriceQueryService: DepositPriceQueryService,
) : BaseGraphQLResolver() {
  /** 押金价目分页(状态/关键词筛选,强制分页) */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_PRODUCT_READ')")
  fun depositPrices(
    @Argument input: DepositPriceListInput?,
  ): DepositPriceVoPage {
    val result =
      depositPriceQueryService.list(
        status = input?.status?.toDomain(),
        keyword = input?.keyword,
        page = input?.page ?: 0,
        size = input?.size ?: 20,
      )
    return DepositPriceVoPage(
      content = result.content.map { it.toGraphQL() },
      totalElements = result.totalElements.toInt(),
      totalPages = result.totalPages,
      size = result.size,
      number = result.number,
    )
  }
}
