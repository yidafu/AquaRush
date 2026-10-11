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
import dev.yidafu.aqua.common.graphql.generated.OrderPreviewItemVo
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
// 枚举按同名条目 when 穷举互转;手写转换(不依赖编译期生成 mapper),
// 避免通用映射框架默认 map 实现抛出 KotlinNothingValueException。

// 枚举映射(域枚举 → 生成枚举,条目同名)
fun DomainCouponType.toGraphQL(): GraphQLCouponType =
  when (this) {
    DomainCouponType.CASH -> GraphQLCouponType.CASH
    DomainCouponType.DISCOUNT -> GraphQLCouponType.DISCOUNT
    DomainCouponType.NO_THRESHOLD_CASH -> GraphQLCouponType.NO_THRESHOLD_CASH
  }

fun DomainCouponScope.toGraphQL(): GraphQLCouponScope =
  when (this) {
    DomainCouponScope.ALL -> GraphQLCouponScope.ALL
    DomainCouponScope.PRODUCT -> GraphQLCouponScope.PRODUCT
  }

fun DomainValidityType.toGraphQL(): GraphQLValidityType =
  when (this) {
    DomainValidityType.FIXED_RANGE -> GraphQLValidityType.FIXED_RANGE
    DomainValidityType.DAYS_AFTER_CLAIM -> GraphQLValidityType.DAYS_AFTER_CLAIM
    DomainValidityType.DAYS_AFTER_PURCHASE -> GraphQLValidityType.DAYS_AFTER_PURCHASE
  }

fun DomainTicketTypeKind.toGraphQL(): GraphQLTicketTypeKind =
  when (this) {
    DomainTicketTypeKind.SPECIFIC_PRODUCT -> GraphQLTicketTypeKind.SPECIFIC_PRODUCT
    DomainTicketTypeKind.UNIVERSAL -> GraphQLTicketTypeKind.UNIVERSAL
  }

fun DomainUserCouponStatus.toGraphQL(): GraphQLUserCouponStatus =
  when (this) {
    DomainUserCouponStatus.RECEIVED -> GraphQLUserCouponStatus.RECEIVED
    DomainUserCouponStatus.USED -> GraphQLUserCouponStatus.USED
    DomainUserCouponStatus.RETURNED -> GraphQLUserCouponStatus.RETURNED
    DomainUserCouponStatus.EXPIRED -> GraphQLUserCouponStatus.EXPIRED
  }

fun DomainUserTicketStatus.toGraphQL(): GraphQLUserTicketStatus =
  when (this) {
    DomainUserTicketStatus.UNUSED -> GraphQLUserTicketStatus.UNUSED
    DomainUserTicketStatus.USED -> GraphQLUserTicketStatus.USED
    DomainUserTicketStatus.RETURNED -> GraphQLUserTicketStatus.RETURNED
    DomainUserTicketStatus.EXPIRED -> GraphQLUserTicketStatus.EXPIRED
  }

fun DomainTicketPurchaseStatus.toGraphQL(): GraphQLTicketPurchaseStatus =
  when (this) {
    DomainTicketPurchaseStatus.PENDING -> GraphQLTicketPurchaseStatus.PENDING
    DomainTicketPurchaseStatus.PAID -> GraphQLTicketPurchaseStatus.PAID
    DomainTicketPurchaseStatus.FAILED -> GraphQLTicketPurchaseStatus.FAILED
    DomainTicketPurchaseStatus.CANCELLED -> GraphQLTicketPurchaseStatus.CANCELLED
  }

fun DomainPriceSource.toGraphQL(): GraphQLPriceSource =
  when (this) {
    DomainPriceSource.CATALOG_PRICE -> GraphQLPriceSource.CATALOG_PRICE
    DomainPriceSource.GROUP_PRICE -> GraphQLPriceSource.GROUP_PRICE
    DomainPriceSource.FLASH_SALE_PRICE -> GraphQLPriceSource.FLASH_SALE_PRICE
    DomainPriceSource.NEWCOMER_PRICE -> GraphQLPriceSource.NEWCOMER_PRICE
    DomainPriceSource.MANUAL -> GraphQLPriceSource.MANUAL
  }

// ============ 查询过滤参数反转换(生成枚举 → 域枚举) ============

fun GraphQLUserCouponStatus.toDomain(): DomainUserCouponStatus =
  when (this) {
    GraphQLUserCouponStatus.EXPIRED -> DomainUserCouponStatus.EXPIRED
    GraphQLUserCouponStatus.RECEIVED -> DomainUserCouponStatus.RECEIVED
    GraphQLUserCouponStatus.RETURNED -> DomainUserCouponStatus.RETURNED
    GraphQLUserCouponStatus.USED -> DomainUserCouponStatus.USED
  }

fun GraphQLUserTicketStatus.toDomain(): DomainUserTicketStatus =
  when (this) {
    GraphQLUserTicketStatus.EXPIRED -> DomainUserTicketStatus.EXPIRED
    GraphQLUserTicketStatus.RETURNED -> DomainUserTicketStatus.RETURNED
    GraphQLUserTicketStatus.UNUSED -> DomainUserTicketStatus.UNUSED
    GraphQLUserTicketStatus.USED -> DomainUserTicketStatus.USED
  }

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
    isLeadGeneration = isLeadGeneration,
    activationOrderCount = activationOrderCount,
  )

/**
 * 我的水票 → UserTicketVO(票种名称/通兑票限定规格取自票种,由服务端查询注入;
 * pendingActivation/activationOrderCount 为 G97 复购激活展示态,由服务端判定注入)
 */
fun UserTicketModel.toGraphQL(
  ticketTypeName: String,
  specification: String?,
  pendingActivation: Boolean,
  activationOrderCount: Int?,
): UserTicketVo =
  UserTicketVo(
    id = id ?: 0L,
    ticketNo = ticketNo,
    ticketTypeName = ticketTypeName,
    specification = specification,
    status = status.toGraphQL(),
    expireTime = expireTime,
    isGift = isGift,
    pendingActivation = pendingActivation,
    activationOrderCount = activationOrderCount,
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
 * 计价结果 → OrderPreviewVO(deliveryFeeCents/depositCents/payAmountCents 由调用方
 * 按配送费规则与押金试算(R-MON-03)计算后注入;availableCoupons 为可用券详情列表,
 * 由服务端按 couponOptions 补全后注入)
 */
fun OrderPricingResult.toGraphQL(
  deliveryFeeCents: Long,
  depositCents: Long,
  payAmountCents: Long,
  availableCoupons: List<UserCouponVo>,
): OrderPreviewVo =
  OrderPreviewVo(
    items = items.map { it.toGraphQL() },
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
    // 随单押金试算(分,R-MON-03:首购规格收取,与 createOrder 同源)
    depositCents = depositCents,
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

/**
 * 商品计价明细 → OrderPreviewItemVO(F-ORD-004 多商品下单)
 */
fun dev.yidafu.aqua.api.dto.OrderPricingItemResult.toGraphQL(): OrderPreviewItemVo =
  OrderPreviewItemVo(
    productId = productId,
    quantity = quantity,
    unitPriceCents = unitPriceCents,
    priceSource = priceSource.toGraphQL(),
    catalogPriceCents = catalogPriceCents,
    subtotalCents = subtotalCents,
    ticketCoveredQuantity = ticketCoveredQuantity,
    cashSubtotalCents = cashSubtotalCents,
  )
