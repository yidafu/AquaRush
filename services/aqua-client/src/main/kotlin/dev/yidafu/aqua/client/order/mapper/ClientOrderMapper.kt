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
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.graphql.generated.Address
import dev.yidafu.aqua.common.graphql.generated.Order
import dev.yidafu.aqua.common.graphql.generated.OrderStatus
import dev.yidafu.aqua.common.graphql.generated.PaymentType
import dev.yidafu.aqua.common.graphql.generated.PriceSource
import dev.yidafu.aqua.common.graphql.generated.Product
import dev.yidafu.aqua.common.graphql.generated.ProductStatus

/**
 * 用户端订单域 GraphQL 映射(纯函数直构,不使用 mappie —— aqua-client 未应用 mappie 编译插件,
 * 其默认 map 实现会抛 KotlinNothingValueException;参见 ClientMarketingMapper 说明)。
 * 枚举按同名条目 valueOf 互转;金额一律为分(R-MON-01)。
 */

fun OrderModel.toGraphQL(): Order =
  Order(
    id = id ?: 0L,
    orderNo = orderNo,
    quantity = quantity,
    amountCents = amountCents,
    deliveryFeeCents = deliveryFeeCents,
    payAmountCents = payAmountCents,
    status = OrderStatus.valueOf(status.name),
    paymentMethod = paymentMethod?.name,
    paymentTransactionId = paymentTransactionId,
    paymentTime = paymentTime,
    isSelfCollect = isSelfCollect,
    paymentType = paymentType?.let { PaymentType.valueOf(it.name) },
    deliveryStartedAt = deliveryStartedAt,
    deliveryConfirmedAt = deliveryConfirmedAt,
    completedAt = completedAt,
    remark = remark,
    createdAt = createdAt,
    updatedAt = updatedAt,
    firstOrderDiscountCents = firstOrderDiscountCents,
    product = product?.toGraphQL(),
    address = address?.toGraphQL(),
    user = null,
    deliveryWorker = null,
    deliveryPhotos = null,
    giftItems = null,
  )

fun ProductModel.toGraphQL(): Product =
  Product(
    id = id ?: 0L,
    name = name,
    subtitle = subtitle,
    priceCents = price,
    originalPriceCents = originalPrice,
    depositPriceCents = depositPrice,
    coverImageUrl = coverImageUrl,
    imageGallery = imageGallery,
    specification = specification,
    waterSource = waterSource,
    mineralContent = mineralContent,
    stock = stock,
    salesVolume = salesVolume,
    status = ProductStatus.valueOf(status.name),
    sortOrder = sortOrder,
    tags = tags,
    detailContent = detailContent,
    certificateImages = certificateImages,
    deliverySettings = deliverySettings,
    createdAt = createdAt,
    updatedAt = updatedAt,
    // 订单快照视角:成交单价=目录价,不参与营销解析
    effectivePriceCents = price,
    hasGroupPrice = false,
    priceSource = PriceSource.CATALOG_PRICE,
  )

fun AddressModel.toGraphQL(): Address =
  Address(
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
