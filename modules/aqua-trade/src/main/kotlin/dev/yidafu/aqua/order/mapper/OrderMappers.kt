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

package dev.yidafu.aqua.order.mapper

import dev.yidafu.aqua.api.dto.CreateOrderRequest
import dev.yidafu.aqua.api.dto.DailyStatDTO
import dev.yidafu.aqua.api.dto.OrderItemRequest
import dev.yidafu.aqua.api.dto.TodayStatisticsDTO
import dev.yidafu.aqua.api.dto.UpdateOrderAddressRequest
import dev.yidafu.aqua.api.dto.WeekStatisticsDTO
import dev.yidafu.aqua.common.domain.model.DeliveryAddressModel
import dev.yidafu.aqua.common.domain.model.OrderItemModel
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.OrderOperationModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.domain.model.enums.PaymentType
import dev.yidafu.aqua.common.graphql.generated.CreateDeliveryOrderInput
import dev.yidafu.aqua.common.graphql.generated.CreateOrderInput
import dev.yidafu.aqua.common.graphql.generated.DailyStatVo
import dev.yidafu.aqua.common.graphql.generated.DeliveryAddressVo
import dev.yidafu.aqua.common.graphql.generated.OrderItemVo
import dev.yidafu.aqua.common.graphql.generated.OrderOperationVo
import dev.yidafu.aqua.common.graphql.generated.OrderVo
import dev.yidafu.aqua.common.graphql.generated.ProductStatus
import dev.yidafu.aqua.common.graphql.generated.ProductVo
import dev.yidafu.aqua.common.graphql.generated.TodayStatisticsVo
import dev.yidafu.aqua.common.graphql.generated.UpdateOrderAddressInput
import dev.yidafu.aqua.common.graphql.generated.WeekStatisticsVo
import dev.yidafu.aqua.delivery.mapper.toDeliveryWorker
import dev.yidafu.aqua.user.mapper.toGraphQL
import tools.jackson.module.kotlin.jacksonObjectMapper
import dev.yidafu.aqua.common.domain.model.enums.DeliveryTimeSlot as DomainDeliveryTimeSlot
import dev.yidafu.aqua.common.graphql.generated.DeliveryPhotoReviewStatus as DeliveryPhotoReviewStatusG
import dev.yidafu.aqua.common.graphql.generated.DeliveryTimeSlot as DeliveryTimeSlotG
import dev.yidafu.aqua.common.graphql.generated.OperatorType as OperatorTypeG
import dev.yidafu.aqua.common.graphql.generated.OrderOperationType as OrderOperationTypeG
import dev.yidafu.aqua.common.graphql.generated.OrderStatus as OrderStatusG
import dev.yidafu.aqua.common.graphql.generated.PaymentType as PaymentTypeG
import dev.yidafu.aqua.common.graphql.generated.PriceSource as PriceSourceG

// ============================================================================
// 枚举同名 valueOf 互转(领域 ↔ GraphQL)
// ============================================================================

/**
 * 领域订单状态 → GraphQL OrderStatus(同名 valueOf 互转)
 */
fun OrderModelStatus.toGraphQL(): OrderStatusG = OrderStatusG.valueOf(name)

/**
 * GraphQL OrderStatus → 领域订单状态(列表筛选入参,原 OrderStatusMapper)
 */
fun OrderStatusG.toDomain(): OrderModelStatus = OrderModelStatus.valueOf(name)

/**
 * 领域收款方式 → GraphQL PaymentType(R-MKT 收款方式快照)
 */
fun PaymentType.toGraphQL(): PaymentTypeG = PaymentTypeG.valueOf(name)

// ============================================================================
// OrderModel → GraphQL OrderVo
// ============================================================================

/**
 * OrderModel → GraphQL OrderVo 直映(权威链路,原 OrderMapper)。
 *
 * - [items] 商品明细(F-ORD-004)由调用方经 OrderItemRepository 填充(见 [OrderItemModel.toOrderItemVo]);
 * - giftItems 由字段级 Resolver 联查 order_gifts 提供(R-MKT-09,配置变更不影响已生成订单);
 * - 配送备注 deliveryRemark 为完成配送时配送员填写,随 completeDelivery 落库(R-ORD-07)。
 */
fun OrderModel.toGraphQL(items: List<OrderItemVo> = emptyList()): OrderVo =
  OrderVo(
    id = id ?: 0L,
    orderNo = orderNo,
    amountCents = amountCents,
    // 配送费(下单时按配送费规则快照 —— R-MON-02)
    deliveryFeeCents = deliveryFeeCents,
    // 应付合计(分)= 商品金额 + 配送费 + 桶押金(计算属性直取)
    payAmountCents = payAmountCents,
    // 领域枚举 → GraphQL 枚举同名互转
    status = status.toGraphQL(),
    paymentMethod = paymentMethod?.name,
    paymentTransactionId = paymentTransactionId,
    paymentTime = paymentTime,
    isSelfCollect = isSelfCollect,
    paymentType = paymentType?.toGraphQL(),
    deliveryStartedAt = deliveryStartedAt,
    deliveryConfirmedAt = deliveryConfirmedAt,
    completedAt = completedAt,
    remark = remark,
    // 配送备注(完成配送时配送员填写,随 completeDelivery 落库 —— R-ORD-07)
    deliveryRemark = deliveryRemark,
    // 配送时段(AC-ORD-001-05,领域枚举 → GraphQL 枚举同名互转;null=未指定)
    deliveryTimeSlot = deliveryTimeSlot?.let { DeliveryTimeSlotG.valueOf(it.name) },
    // deliveryPhotos 为 JSON 字符串,GraphQL 为 List<String>;解析失败兜底空列表
    deliveryPhotos =
      deliveryPhotos?.let { photos ->
        runCatching {
          jacksonObjectMapper().readValue(photos, Array<String>::class.java).toList()
        }.getOrElse { emptyList() }
      },
    // 配送照片核验留痕(G23,领域枚举 → GraphQL 枚举同名互转)
    deliveryPhotoReviewStatus = deliveryPhotoReviewStatus?.let { DeliveryPhotoReviewStatusG.valueOf(it.name) },
    deliveryPhotoReviewedAt = deliveryPhotoReviewedAt,
    deliveryPhotoReviewedBy = deliveryPhotoReviewedBy,
    deliveryPhotoReviewRemark = deliveryPhotoReviewRemark,
    // 营销计价快照(R-MKT-01/02/07/08/10:订单快照直映,配置变更不影响已生成订单)
    promotionDiscountCents = promotionDiscountCents,
    couponDiscountCents = couponDiscountCents,
    couponName = couponName,
    promotionActivityName = promotionActivityName,
    firstOrderDiscountCents = firstOrderDiscountCents,
    firstOrderActivityName = firstOrderActivityName,
    ticketRedeemCount = ticketRedeemCount,
    ticketDiscountCents = ticketDiscountCents,
    // 桶押金快照(R-MON-03/G06)
    depositCents = depositCents,
    // 商品明细(F-ORD-004)由调用方注入
    items = items,
    // 赠品行由字段级 Resolver 提供(R-MKT-09,配置变更不影响已生成订单)
    giftItems = null,
    address = address?.toGraphQL(),
    user = user?.toGraphQL(),
    deliveryWorker = deliveryWorker?.toDeliveryWorker(),
    createdAt = createdAt,
    updatedAt = updatedAt,
  )

/**
 * DeliveryAddressModel → GraphQL DeliveryAddressVo(原 DeliveryAddressMapper)。
 * GraphQL 经纬度为 Float,领域模型/存储统一 Double(WGS-84)。
 */
fun DeliveryAddressModel.toGraphQL(): DeliveryAddressVo =
  DeliveryAddressVo(
    id = id ?: 0L,
    userId = userId,
    receiverName = receiverName,
    receiverPhone = receiverPhone,
    province = province,
    city = city,
    district = district,
    detailAddress = detailAddress,
    longitude = longitude?.toFloat(),
    latitude = latitude?.toFloat(),
    createdAt = createdAt,
    updatedAt = updatedAt,
  )

// ============================================================================
// GraphQL Input → Request(保留中间 DTO 场景)
// ============================================================================

/**
 * 用户下单输入 → CreateOrderRequest(原 CreateOrderInputMapper)。
 *
 * - userId 占位 0,resolver 以登录 principal 复制注入(防越权);
 * - deliveryTime → deliveryTimeSlot 改名 + 领域枚举同名互转(AC-ORD-001-05,缺省=立即配送);
 * - ticketRedeemCount 缺省回填 0(R-MKT-08);
 * - isSelfCollect 无源字段,保持默认 false(代客下单才为 true)。
 */
fun CreateOrderInput.toRequest(): CreateOrderRequest =
  CreateOrderRequest(
    userId = 0L,
    addressId = addressId,
    remark = remark,
    // 商品明细(F-ORD-004 多商品下单;单商品=单元素)
    items = items.map { OrderItemRequest(productId = it.productId, quantity = it.quantity) },
    // 服务端下单幂等键(R-ORD-06)
    clientRequestId = clientRequestId,
    // 配送时段(AC-ORD-001-05,GraphQL 枚举 → 领域枚举同名互转)
    deliveryTimeSlot = deliveryTime?.let { DomainDeliveryTimeSlot.valueOf(it.name) },
    // 促销引擎(R-MKT-07 叠加③):省略=引擎自动选最优,0=不用券;一单一券
    preferredCouponId = preferredCouponId,
    // 水票核销张数(一票抵一桶 R-MKT-08)
    ticketRedeemCount = ticketRedeemCount ?: 0,
  )

/**
 * 代客下单输入 → CreateOrderRequest(原 CreateDeliveryOrderInputMapper)。
 * 代客下单(F-ORD-012)多商品:items 直传(F-ORD-004 多商品唯一口径;单商品=单元素);
 * clientRequestId/deliveryTimeSlot/preferredCouponId/ticketRedeemCount 无源字段,保持默认。
 */
fun CreateDeliveryOrderInput.toRequest(): CreateOrderRequest =
  CreateOrderRequest(
    userId = 0L,
    addressId = addressId,
    remark = remark,
    isSelfCollect = isSelfCollect,
    items = items.map { OrderItemRequest(productId = it.productId, quantity = it.quantity) },
  )

/**
 * 送水端订单页地址信息修改输入 → UpdateOrderAddressRequest(F-ORD-024)。
 * GraphQL 经纬度为 Float,领域模型/存储统一 Double(WGS-84)。
 */
fun UpdateOrderAddressInput.toRequest(): UpdateOrderAddressRequest =
  UpdateOrderAddressRequest(
    receiverName = receiverName,
    phone = phone,
    detailAddress = detailAddress,
    floor = floor,
    longitude = longitude?.toDouble(),
    latitude = latitude?.toDouble(),
  )

// ============================================================================
// 统计 DTO → GraphQL(统计 DTO 属 S2 保留类)
// ============================================================================

fun TodayStatisticsDTO.toGraphQL(): TodayStatisticsVo =
  TodayStatisticsVo(
    completedOrders = completedOrders,
    earningCents = earningCents,
    totalOrders = totalOrders,
    unfinishedOrders = unfinishedOrders,
  )

fun WeekStatisticsDTO.toGraphQL(): WeekStatisticsVo =
  WeekStatisticsVo(
    dailyStats = dailyStats.map { it.toGraphQL() },
    totalEarningCents = totalEarningCents,
    totalOrders = totalOrders,
  )

fun DailyStatDTO.toGraphQL(): DailyStatVo =
  DailyStatVo(
    date = date,
    earningCents = earningCents,
    orderCount = orderCount,
  )

/**
 * 订单商品明细 → GraphQL OrderItemVO(F-ORD-004):
 * product 取下单时快照(product_snapshot JSON),价格展示以快照为准(配置变更不影响已生成订单)。
 */
fun OrderItemModel.toOrderItemVo(): OrderItemVo {
  val snapshot =
    runCatching {
      jacksonObjectMapper().readValue(
        productSnapshot,
        object : tools.jackson.core.type.TypeReference<Map<String, Any>>() {},
      )
    }.getOrNull()
  val productIdValue = snapshot?.get("id")?.toString()?.toLongOrNull() ?: productId
  return OrderItemVo(
    productId = productId,
    product =
      ProductVo(
        certificateImages = null,
        coverImageUrl = snapshot?.get("coverImageUrl")?.toString() ?: "",
        createdAt = createdAt,
        detailContent = null,
        effectivePriceCents = unitPriceCents,
        hasGroupPrice = false,
        priceSource = PriceSourceG.CATALOG_PRICE,
        id = productIdValue,
        imageGallery = null,
        mineralContent = null,
        name = snapshot?.get("name")?.toString() ?: "商品",
        originalPriceCents = snapshot?.get("priceCents")?.toString()?.toLongOrNull(),
        priceCents = snapshot?.get("priceCents")?.toString()?.toLongOrNull() ?: unitPriceCents,
        salesVolume = 0,
        sortOrder = 0,
        specification = snapshot?.get("specification")?.toString() ?: "",
        status = ProductStatus.OFFLINE,
        stock = 0,
        subtitle = null,
        tags = null,
        updatedAt = createdAt,
        waterSource = null,
      ),
    quantity = quantity,
    unitPriceCents = unitPriceCents,
    totalPriceCents = totalPriceCents,
  )
}

// ============================================================================
// 订单操作留痕 → GraphQL
// ============================================================================

/**
 * OrderOperationModel → GraphQL OrderOperationVo
 *
 * 枚举跨包同名 valueOf 互转;extraData 落库为 JSON 文本,序列化前还原为 ObjectNode
 * (JsonObjectScalar 亦接受 String,但 Vo 字段类型为 ObjectNode,故在 mapper 内转换)。
 */
fun OrderOperationModel.toGraphQL(): OrderOperationVo =
  OrderOperationVo(
    id = id ?: 0L,
    orderId = orderId,
    operationType = OrderOperationTypeG.valueOf(operationType.name),
    operatorId = operatorId,
    operatorType = OperatorTypeG.valueOf(operatorType.name),
    description = description,
    extraData =
      extraData?.let { json ->
        runCatching {
          val node = jacksonObjectMapper().readTree(json)
          node as? tools.jackson.databind.node.ObjectNode
        }.getOrNull()
      },
    createdAt = createdAt,
  )
