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

import dev.yidafu.aqua.api.dto.ClaimCouponRequest
import dev.yidafu.aqua.api.service.marketing.CouponMutationApiService
import dev.yidafu.aqua.client.marketing.mapper.toGraphQL
import dev.yidafu.aqua.common.annotation.ClientService
import dev.yidafu.aqua.common.graphql.generated.UserCouponVo
import dev.yidafu.aqua.common.security.UserPrincipal
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

/**
 * 用户端优惠券变更 Resolver(F-MKT-003 领券中心领取)
 */
@ClientService
@Controller
class ClientCouponMutationResolver(
  private val couponMutationApiService: CouponMutationApiService,
) {
  /**
   * 领取优惠券(仅 ACTIVE+AUTO_CLAIM 模板,服务端限领数校验;usable 由计价引擎按订单上下文计算,此处置 false)
   */
  @MutationMapping
  @PreAuthorize("isAuthenticated()")
  fun claimCoupon(
    @Argument couponTemplateId: Long,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): UserCouponVo = couponMutationApiService.claimCoupon(principal.id, ClaimCouponRequest(couponTemplateId)).toGraphQL(usable = false)
}
