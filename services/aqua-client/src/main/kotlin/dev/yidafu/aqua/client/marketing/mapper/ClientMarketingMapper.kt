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

package dev.yidafu.aqua.client.marketing.mapper

import dev.yidafu.aqua.api.dto.CouponOption
import dev.yidafu.aqua.api.dto.GiftItem
import dev.yidafu.aqua.api.dto.OrderPricingResult
import dev.yidafu.aqua.api.service.marketing.PriceResolution
import dev.yidafu.aqua.common.domain.model.CouponTemplateModel
import dev.yidafu.aqua.common.domain.model.TicketPurchaseModel
import dev.yidafu.aqua.common.domain.model.TicketTypeModel
import dev.yidafu.aqua.common.domain.model.UserCouponModel
import dev.yidafu.aqua.common.domain.model.UserTicketModel
import dev.yidafu.aqua.common.graphql.generated.CouponOptionVo
import dev.yidafu.aqua.common.graphql.generated.CouponTemplateVo
import dev.yidafu.aqua.common.graphql.generated.GiftItemVo
import dev.yidafu.aqua.common.graphql.generated.OrderPreviewVo
import dev.yidafu.aqua.common.graphql.generated.ProductPriceVo
import dev.yidafu.aqua.common.graphql.generated.TicketPurchaseVo
import dev.yidafu.aqua.common.graphql.generated.TicketTypeVo
import dev.yidafu.aqua.common.graphql.generated.UserCouponVo
import dev.yidafu.aqua.common.graphql.generated.UserTicketVo
import tools.jackson.databind.node.ObjectNode
import dev.yidafu.aqua.common.domain.model.enums.CouponScope as DomainCouponScope
import dev.yidafu.aqua.common.domain.model.enums.CouponType as DomainCouponType
import dev.yidafu.aqua.common.domain.model.enums.PriceSource as DomainPriceSource
import dev.yidafu.aqua.common.domain.model.enums.TicketPurchaseStatus as DomainTicketPurchaseStatus
import dev.yidafu.aqua.common.domain.model.enums.TicketTypeKind as DomainTicketTypeKind
import dev.yidafu.aqua.common.domain.model.enums.UserCouponStatus as DomainUserCouponStatus
import dev.yidafu.aqua.common.domain.model.enums.UserTicketStatus as DomainUserTicketStatus
import dev.yidafu.aqua.common.domain.model.enums.ValidityType as DomainValidityType
import dev.yidafu.aqua.common.graphql.generated.CouponScope as GraphQLCouponScope
import dev.yidafu.aqua.common.graphql.generated.CouponType as GraphQLCouponType
import dev.yidafu.aqua.common.graphql.generated.PriceSource as GraphQLPriceSource
import dev.yidafu.aqua.common.graphql.generated.TicketPurchaseStatus as GraphQLTicketPurchaseStatus
import dev.yidafu.aqua.common.graphql.generated.TicketTypeKind as GraphQLTicketTypeKind
import dev.yidafu.aqua.common.graphql.generated.UserCouponStatus as GraphQLUserCouponStatus
import dev.yidafu.aqua.common.graphql.generated.UserTicketStatus as GraphQLUserTicketStatus
import dev.yidafu.aqua.common.graphql.generated.ValidityType as GraphQLValidityType

// 用户端营销域 GraphQL VO 映射(F-MKT-002/003/007、R-MKT-01/02/07/08):
// 所有映射均带外部数据(已领数量/可用性/票种名/配送费等),统一采用纯函数直接构造生成 VO;
// 枚举按同名条目 valueOf 互转;不采用 ObjectMappie/EnumMappie —— aqua-client 服务模块未应用
// mappie 编译插件,其默认 map 实现会抛 KotlinNothingValueException。

// 枚举映射(域枚举 → 生成枚举,条目同名)
fun DomainCouponType.toGraphQL(): GraphQLCouponType = GraphQLCouponType.valueOf(name)

fun DomainCouponScope.toGraphQL(): GraphQLCouponScope = GraphQLCouponScope.valueOf(name)

fun DomainValidityType.toGraphQL(): GraphQLValidityType = GraphQLValidityType.valueOf(name)

fun DomainTicketTypeKind.toGraphQL(): GraphQLTicketTypeKind = GraphQLTicketTypeKind.valueOf(name)

fun DomainUserCouponStatus.toGraphQL(): GraphQLUserCouponStatus = GraphQLUserCouponStatus.valueOf(name)

fun DomainUserTicketStatus.toGraphQL(): GraphQLUserTicketStatus = GraphQLUserTicketStatus.valueOf(name)

fun DomainTicketPurchaseStatus.toGraphQL(): GraphQLTicketPurchaseStatus = GraphQLTicketPurchaseStatus.valueOf(name)

fun DomainPriceSource.toGraphQL(): GraphQLPriceSource = GraphQLPriceSource.valueOf(name)

// ============ 查询过滤参数反转换(生成枚举 → 域枚举) ============

fun GraphQLUserCouponStatus.toDomain(): DomainUserCouponStatus = DomainUserCouponStatus.valueOf(name)

fun GraphQLUserTicketStatus.toDomain(): DomainUserTicketStatus = DomainUserTicketStatus.valueOf(name)

// ============ 优惠券(F-MKT-003) ============

/**
 * 领券中心模板 → CouponTemplateVO(claimedCount=当前用户已领数量,由服务端查询注入)
 */
fun CouponTemplateModel.toGraphQL(claimedCount: Int): CouponTemplateVo =
  CouponTemplateVo(
    id = id ?: 0L,
    name = name,
    couponType = couponType.toGraphQL(),
    thresholdCents = thresholdCents,
    discountAmountCents = discountAmountCents,
    discountRatePermille = discountRatePermille,
    scope = scope.toGraphQL(),
    productId = productId,
    validityType = validityType.toGraphQL(),
    validFrom = validFrom,
    validTo = validTo,
    validDays = validDays,
    perUserLimit = perUserLimit,
    claimedCount = claimedCount,
  )

/**
 * 我的券 → UserCouponVO(usable=是否在当前订单上下文可用,由计价引擎计算,普通列表置 false)
 */
fun UserCouponModel.toGraphQL(usable: Boolean): UserCouponVo =
  UserCouponVo(
    id = id ?: 0L,
    name = name,
    couponType = couponType.toGraphQL(),
    thresholdCents = thresholdCents,
    discountAmountCents = discountAmountCents,
    discountRatePermille = discountRatePermille,
    scope = scope.toGraphQL(),
    productId = productId,
    status = status.toGraphQL(),
    expireTime = expireTime,
    usable = usable,
  )

// ============ 桶水票(F-MKT-007) ============

/**
 * 水票商城票种 → TicketTypeVO(myPurchasedCount=当前用户已购张数,由服务端查询注入)
 */
fun TicketTypeModel.toGraphQL(myPurchasedCount: Int): TicketTypeVo =
  TicketTypeVo(
    id = id ?: 0L,
    name = name,
    ticketType = ticketType.toGraphQL(),
    productId = productId,
    specification = specification,
    packageCount = packageCount,
    packagePriceCents = packagePriceCents,
    giftCount = giftCount,
    validityType = validityType.toGraphQL(),
    validFrom = validFrom,
    validTo = validTo,
    validDays = validDays,
    perUserLimit = perUserLimit,
    myPurchasedCount = myPurchasedCount,
  )

/**
 * 我的水票 → UserTicketVO(票种名称/通兑票限定规格取自票种,由服务端查询注入)
 */
fun UserTicketModel.toGraphQL(
  ticketTypeName: String,
  specification: String?,
): UserTicketVo =
  UserTicketVo(
    id = id ?: 0L,
    ticketTypeName = ticketTypeName,
    specification = specification,
    status = status.toGraphQL(),
    expireTime = expireTime,
    isGift = isGift,
  )

/**
 * 购票单 → TicketPurchaseVO(requiresSimulation=支付桩实现标记;paymentParams=微信支付参数,待 SDK 接入)
 */
fun TicketPurchaseModel.toGraphQL(
  ticketTypeName: String,
  requiresSimulation: Boolean,
  paymentParams: ObjectNode?,
): TicketPurchaseVo =
  TicketPurchaseVo(
    id = id ?: 0L,
    orderNo = orderNo,
    ticketTypeName = ticketTypeName,
    totalCount = totalCount,
    packagePriceCents = packagePriceCents,
    paymentStatus = paymentStatus.toGraphQL(),
    paymentParams = paymentParams,
    requiresSimulation = requiresSimulation,
  )

// ============ 计价预览(F-ORD-023,R-MKT-02/07/08) ============

/**
 * 引擎单张可用券计价选项 → CouponOptionVO
 */
fun CouponOption.toGraphQL(): CouponOptionVo =
  CouponOptionVo(
    couponId = couponId,
    name = name,
    discountCents = discountCents,
    usable = usable,
    isBest = isBest,
  )

/**
 * 计价结果 → OrderPreviewVO(deliveryFeeCents/payAmountCents 由订单域楼层规则计算后注入;
 * availableCoupons 为可用券详情列表,由服务端按 couponOptions 补全后注入)
 */
fun OrderPricingResult.toGraphQL(
  deliveryFeeCents: Long,
  payAmountCents: Long,
  availableCoupons: List<UserCouponVo>,
): OrderPreviewVo =
  OrderPreviewVo(
    unitPriceCents = unitPriceCents,
    priceSource = priceSource.toGraphQL(),
    subtotalCents = subtotalCents,
    ticketRedeemCount = ticketRedeemCount,
    ticketCoveredQuantity = ticketCoveredQuantity,
    cashSubtotalCents = cashSubtotalCents,
    promotionDiscountCents = promotionDiscountCents,
    promotionActivityId = promotionActivityId,
    promotionActivityName = promotionActivityName,
    firstOrderDiscountCents = firstOrderDiscountCents,
    firstOrderActivityId = firstOrderActivityId,
    firstOrderActivityName = firstOrderActivityName,
    couponDiscountCents = couponDiscountCents,
    selectedCouponId = selectedCouponId,
    selectedCouponName = selectedCouponName,
    totalDiscountCents = totalDiscountCents,
    payableProductCents = payableProductCents,
    deliveryFeeCents = deliveryFeeCents,
    payAmountCents = payAmountCents,
    availableTicketCount = availableTicketCount,
    couponOptions = couponOptions.map { it.toGraphQL() },
    availableCoupons = availableCoupons,
    giftItems = giftItems.map { it.toGraphQL() },
  )

// ============ 买赠/满赠赠品(R-MKT-09) ============

/**
 * 赠品行 → GiftItemVO(赠品为附加 0 价行,单价由前端呈现 ¥0.00,不传价)
 */
fun GiftItem.toGraphQL(): GiftItemVo =
  GiftItemVo(
    productId = productId,
    productName = productName,
    quantity = quantity,
  )

// ============ 商品成交单价(R-MKT-01) ============

/**
 * 成交单价解析 → ProductPriceVO(catalogPriceCents=商品目录价,由服务端查询注入)
 */
fun PriceResolution.toGraphQL(
  productId: Long,
  catalogPriceCents: Long,
): ProductPriceVo =
  ProductPriceVo(
    productId = productId,
    effectivePriceCents = unitPriceCents,
    priceSource = priceSource.toGraphQL(),
    hasGroupPrice = hasGroupPrice,
    catalogPriceCents = catalogPriceCents,
  )
