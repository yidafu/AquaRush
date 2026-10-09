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

package dev.yidafu.aqua.admin.marketing.resolvers

import dev.yidafu.aqua.api.service.marketing.GroupProductPriceMutationService
import dev.yidafu.aqua.common.graphql.generated.SaveGroupProductPricesInput
import dev.yidafu.aqua.common.security.UserPrincipal
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

/**
 * 分组商品专属价变更 Resolver(管理后台,PERMISSION_MKT_WRITE)
 * 保存后订单计价即时生效,已生成订单成交价不变(R-MKT-01 快照)。
 */
@Controller
class AdminGroupProductPriceMutationResolver(
  private val groupProductPriceMutationService: GroupProductPriceMutationService,
) {
  private val logger = LoggerFactory.getLogger(AdminGroupProductPriceMutationResolver::class.java)

  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_MKT_WRITE')")
  fun saveGroupProductPrices(
    @Argument @Valid input: SaveGroupProductPricesInput,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): Boolean {
    logger.info("Admin {} saving group product prices for group {}", principal.username, input.customerGroupId)
    return groupProductPriceMutationService.savePrices(
      adminId = principal.id,
      adminName = principal.username,
      input = input,
    )
  }
}
