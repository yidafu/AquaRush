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

package dev.yidafu.aqua.client.order.mapper

import dev.yidafu.aqua.common.domain.model.AddressModel
import dev.yidafu.aqua.common.domain.model.OrderItemModel
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.ReviewModel
import dev.yidafu.aqua.common.graphql.generated.AddressVo
import dev.yidafu.aqua.common.graphql.generated.DeliveryPhotoReviewStatus
import dev.yidafu.aqua.common.graphql.generated.DeliveryTimeSlot
import dev.yidafu.aqua.common.graphql.generated.OrderVo
import dev.yidafu.aqua.common.graphql.generated.PriceSource
import dev.yidafu.aqua.common.graphql.generated.ProductVo
import dev.yidafu.aqua.common.graphql.generated.ReviewVo
import dev.yidafu.aqua.order.mapper.toGraphQL
import dev.yidafu.aqua.order.mapper.toOrderItemVo
import dev.yidafu.aqua.product.mapper.toGraphQL
import dev.yidafu.aqua.common.domain.model.enums.DeliveryPhotoReviewStatus as DomainDeliveryPhotoReviewStatus
import dev.yidafu.aqua.common.domain.model.enums.DeliveryTimeSlot as DomainDeliveryTimeSlot

/**
 * 用户端订单域 GraphQL 映射(纯函数直构,手写转换不依赖编译期生成 mapper,
 * 避免通用映射框架默认 map 实现抛出 KotlinNothingValueException;参见 ClientMarketingMapper 说明)。
 * 枚举按同名条目 when 穷举互转;金额一律为分(R-MON-01)。
 */

fun OrderModel.toGraphQL(items: List<OrderItemModel> = emptyList()): OrderVo =
  OrderVo(
    id = id ?: 0L,
    orderNo = orderNo,
    amountCents = amountCents,
    deliveryFeeCents = deliveryFeeCents,
    payAmountCents = payAmountCents,
    status = status.toGraphQL(),
    paymentMethod = paymentMethod?.name,
    paymentTransactionId = paymentTransactionId,
    paymentTime = paymentTime,
    isSelfCollect = isSelfCollect,
    paymentType = paymentType?.let { it.toGraphQL() },
    deliveryStartedAt = deliveryStartedAt,
    assignedAt = assignedAt,
    completedAt = completedAt,
    remark = remark,
    // 配送时段(AC-ORD-001-05,领域枚举 → GraphQL 枚举同名互转)
    deliveryTimeSlot = deliveryTimeSlot?.let { it.toGraphQL() },
    createdAt = createdAt,
    updatedAt = updatedAt,
    firstOrderDiscountCents = firstOrderDiscountCents,
    // 营销计价快照(R-MKT-01/02/07:优惠明细展示依据,2026-08 快照落库)
    promotionDiscountCents = promotionDiscountCents,
    couponDiscountCents = couponDiscountCents,
    // 营销快照名称/水票(R-MKT-03/04/07/08/10:一单一券/满减/首单立减/水票抵扣展示)
    couponName = couponName,
    promotionActivityName = promotionActivityName,
    firstOrderActivityName = firstOrderActivityName,
    ticketRedeemCount = ticketRedeemCount,
    ticketDiscountCents = ticketDiscountCents,
    // 桶押金快照(R-MON-03/G06)
    depositCents = depositCents,
    // 配送照片核验留痕(G23,领域枚举 → GraphQL 枚举同名互转;用户端不展示,随单透传)
    deliveryPhotoReviewStatus =
      deliveryPhotoReviewStatus?.let { it.toGraphQL() },
    deliveryPhotoReviewedAt = deliveryPhotoReviewedAt,
    deliveryPhotoReviewedBy = deliveryPhotoReviewedBy,
    deliveryPhotoReviewRemark = deliveryPhotoReviewRemark,
    // 商品明细(F-ORD-004 多商品下单,order_items 权威数据源,随单快照映射)
    items = items.map { it.toOrderItemVo() },
    address = address?.toGraphQL(),
    user = null,
    deliveryWorker = null,
    deliveryPhotos = null,
    deliveryRemark = deliveryRemark,
    giftItems = null,
  )

fun ProductModel.toGraphQL(): ProductVo =
  ProductVo(
    id = id ?: 0L,
    name = name,
    subtitle = subtitle,
    priceCents = price,
    originalPriceCents = originalPrice,
    coverImageUrl = coverImageUrl,
    imageGallery = imageGallery,
    specification = specification,
    waterSource = waterSource,
    mineralContent = mineralContent,
    stock = stock,
    salesVolume = salesVolume,
    status = status.toGraphQL(),
    sortOrder = sortOrder,
    tags = tags,
    detailContent = detailContent,
    certificateImages = certificateImages,
    createdAt = createdAt,
    updatedAt = updatedAt,
    // 订单快照视角:成交单价=目录价,不参与营销解析
    effectivePriceCents = price,
    hasGroupPrice = false,
    priceSource = PriceSource.CATALOG_PRICE,
  )

fun AddressModel.toGraphQL(): AddressVo =
  AddressVo(
    id = id ?: 0L,
    userId = userId,
    receiverName = receiverName,
    phone = phone,
    province = province,
    provinceCode = provinceCode,
    city = city,
    cityCode = cityCode,
    district = district,
    districtCode = districtCode,
    detailAddress = detailAddress,
    floor = floor,
    longitude = longitude?.toFloat(),
    latitude = latitude?.toFloat(),
    isDefault = isDefault,
    createdAt = createdAt,
    updatedAt = updatedAt,
  )

/**
 * 订单评价 GraphQL 映射(F-ORD-007,一单一评):
 * deliveryWorkerName 由送水工端/后台联查展示,用户端评价展示不依赖该字段。
 */
fun ReviewModel.toGraphQL(): ReviewVo =
  ReviewVo(
    reviewId = id ?: 0L,
    orderId = orderId,
    userId = userId,
    deliveryWorkerId = deliveryWorkerId,
    deliveryWorkerName = null,
    rating = rating,
    comment = comment,
    isAnonymous = isAnonymous,
    createdAt = createdAt,
  )

private fun DomainDeliveryTimeSlot.toGraphQL(): DeliveryTimeSlot =
  when (this) {
    DomainDeliveryTimeSlot.IMMEDIATE -> DeliveryTimeSlot.IMMEDIATE
    DomainDeliveryTimeSlot.MORNING -> DeliveryTimeSlot.MORNING
    DomainDeliveryTimeSlot.AFTERNOON -> DeliveryTimeSlot.AFTERNOON
    DomainDeliveryTimeSlot.EVENING -> DeliveryTimeSlot.EVENING
  }

private fun DomainDeliveryPhotoReviewStatus.toGraphQL(): DeliveryPhotoReviewStatus =
  when (this) {
    DomainDeliveryPhotoReviewStatus.PENDING -> DeliveryPhotoReviewStatus.PENDING
    DomainDeliveryPhotoReviewStatus.APPROVED -> DeliveryPhotoReviewStatus.APPROVED
    DomainDeliveryPhotoReviewStatus.REJECTED -> DeliveryPhotoReviewStatus.REJECTED
  }
