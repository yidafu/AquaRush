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

package dev.yidafu.aqua.client.marketing.resolver

import dev.yidafu.aqua.api.service.marketing.CouponQueryApiService
import dev.yidafu.aqua.client.marketing.mapper.toDomain
import dev.yidafu.aqua.client.marketing.mapper.toGraphQL
import dev.yidafu.aqua.common.annotation.ClientService
import dev.yidafu.aqua.common.graphql.generated.CouponTemplateVo
import dev.yidafu.aqua.common.graphql.generated.UserCouponStatus
import dev.yidafu.aqua.common.graphql.generated.UserCouponVo
import dev.yidafu.aqua.common.security.UserPrincipal
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

/**
 * 用户端优惠券查询 Resolver(F-MKT-003:领券中心/我的券包)
 */
@ClientService
@Controller
class ClientCouponQueryResolver(
  private val couponQueryApiService: CouponQueryApiService,
) {
  /**
   * 领券中心(仅自动领取模板,含当前用户已领数量用于限领展示)
   */
  @QueryMapping
  @PreAuthorize("isAuthenticated()")
  fun couponCenter(
    @AuthenticationPrincipal principal: UserPrincipal,
  ): List<CouponTemplateVo> =
    couponQueryApiService.findClaimableTemplates(principal.id).map { template ->
      template.toGraphQL(
        claimedCount = couponQueryApiService.countUserCouponsByTemplate(principal.id, template.id!!).toInt(),
      )
    }

  /**
   * 我的券包(按状态过滤,空=全部;usable 由确认订单计价引擎按订单上下文计算,此处列表置 false)
   */
  @QueryMapping
  @PreAuthorize("isAuthenticated()")
  fun myCoupons(
    @Argument status: UserCouponStatus?,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): List<UserCouponVo> = couponQueryApiService.findUserCoupons(principal.id, status?.toDomain()).map { it.toGraphQL(usable = false) }
}
