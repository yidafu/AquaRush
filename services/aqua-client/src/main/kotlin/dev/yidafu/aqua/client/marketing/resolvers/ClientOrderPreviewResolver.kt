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

package dev.yidafu.aqua.client.marketing.resolvers

import dev.yidafu.aqua.api.dto.OrderDepositItem
import dev.yidafu.aqua.api.dto.OrderPricingRequest
import dev.yidafu.aqua.api.service.deposit.DepositMutationApiService
import dev.yidafu.aqua.api.service.marketing.CouponQueryApiService
import dev.yidafu.aqua.api.service.ops.DeliveryFeeRuleQueryApiService
import dev.yidafu.aqua.api.service.order.OrderMutationService
import dev.yidafu.aqua.api.service.user.AddressQueryApiService
import dev.yidafu.aqua.client.marketing.mapper.toGraphQL
import dev.yidafu.aqua.common.annotation.ClientService
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.graphql.generated.OrderPreviewInput
import dev.yidafu.aqua.common.graphql.generated.OrderPreviewVo
import dev.yidafu.aqua.common.security.UserPrincipal
import jakarta.validation.Valid
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

/**
 * 用户端确认订单计价预览 Resolver(F-ORD-023)
 * 促销引擎全链路纯计算(R-MKT-02/07/08),金额一律服务端计算(R-MON-01);
 * 配送费按楼层分档 + 距离阶梯叠加(R-MON-02/R-OPS-03,PRD 12-operations-config.md §F-OPS-005),
 * 并返回起送门槛信息(minimumOrderCents/belowMinimum,R-OPS-03 前端预检)。
 */
@ClientService
@Controller
class ClientOrderPreviewResolver(
  private val orderMutationService: OrderMutationService,
  private val deliveryFeeRuleQueryApiService: DeliveryFeeRuleQueryApiService,
  private val addressQueryApiService: AddressQueryApiService,
  private val couponQueryApiService: CouponQueryApiService,
  private val depositMutationApiService: DepositMutationApiService,
) {
  /**
   * 确认订单计价预览:校验地址归属后委托促销引擎纯计算,返回完整费用明细与可用券列表。
   */
  @QueryMapping
  @PreAuthorize("isAuthenticated()")
  fun orderPreview(
    @Argument @Valid input: OrderPreviewInput,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): OrderPreviewVo {
    // 校验地址归属:仅可预览本人收货地址
    val address =
      addressQueryApiService.findById(input.addressId)
        ?: throw BadRequestException("无权使用此收货地址")
    if (address.userId != principal.id) {
      throw BadRequestException("无权使用此收货地址")
    }
    val result =
      orderMutationService.confirmOrderPreview(
        OrderPricingRequest(
          userId = principal.id,
          items =
            input.items.map {
              dev.yidafu.aqua.api.dto
                .OrderPricingItemRequest(productId = it.productId, quantity = it.quantity)
            },
          isSelfCollect = false,
          preferredCouponId = input.preferredCouponId,
          ticketRedeemCount = input.ticketRedeemCount ?: 0,
        ),
      )
    // 配送费规则单次读取:距离阶梯 + 楼层分档叠加(R-MON-02/R-OPS-03),门槛判定同源
    val rule = deliveryFeeRuleQueryApiService.getEffectiveDeliveryFeeRule()
    val deliveryFeeCents =
      rule.feeCentsForDelivery(
        floor = address.floor,
        addressLongitude = address.longitude,
        addressLatitude = address.latitude,
        isSelfCollect = false,
      )
    // 押金试算(R-MON-03):与 createOrder 同源判定,返回行含规格/单价/桶数
    val depositCents =
      depositMutationApiService
        .calcOrderDeposit(
          userId = principal.id,
          items = input.items.map { OrderDepositItem(productId = it.productId, quantity = it.quantity) },
        ).totalCents
    // 可用券详情列表:引擎按 couponOptions 计算各券优惠,此处按 couponId 补全券面信息
    val availableCoupons =
      result.couponOptions.mapNotNull { option ->
        couponQueryApiService
          .findUserCouponByIdAndUser(principal.id, option.couponId)
          ?.toGraphQL(usable = option.usable)
      }
    return result.toGraphQL(
      deliveryFeeCents = deliveryFeeCents,
      // 起送门槛(R-OPS-03):与 createOrder 同源判定(商品成交小计 < 门槛;0=不启用)
      minimumOrderCents = rule.minimumOrderCents,
      belowMinimum = rule.belowMinimumOrder(result.subtotalCents),
      // 押金(R-MON-03):随单首购规格收取,预览与 createOrder 同源判定
      depositCents = depositCents,
      payAmountCents = result.payableProductCents + deliveryFeeCents + depositCents,
      availableCoupons = availableCoupons,
    )
  }
}
