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
// 枚举同名枚举 when 穷举互转(领域 ↔ GraphQL)
// ============================================================================

/**
 * 领域订单状态 → GraphQL OrderStatus(同名枚举 when 穷举互转)
 */
fun OrderModelStatus.toGraphQL(): OrderStatusG =
  when (this) {
    OrderModelStatus.PENDING_PAYMENT -> OrderStatusG.PENDING_PAYMENT
    OrderModelStatus.PENDING_DISPATCH -> OrderStatusG.PENDING_DISPATCH
    OrderModelStatus.PENDING_DELIVERY -> OrderStatusG.PENDING_DELIVERY
    OrderModelStatus.DELIVERING -> OrderStatusG.DELIVERING
    OrderModelStatus.AWAITING_CONFIRMATION -> OrderStatusG.AWAITING_CONFIRMATION
    OrderModelStatus.COMPLETED -> OrderStatusG.COMPLETED
    OrderModelStatus.CANCELLED -> OrderStatusG.CANCELLED
    OrderModelStatus.REFUNDED -> OrderStatusG.REFUNDED
  }

/**
 * GraphQL OrderStatus → 领域订单状态(列表筛选入参,原 OrderStatusMapper)
 */
fun OrderStatusG.toDomain(): OrderModelStatus =
  when (this) {
    OrderStatusG.AWAITING_CONFIRMATION -> OrderModelStatus.AWAITING_CONFIRMATION
    OrderStatusG.CANCELLED -> OrderModelStatus.CANCELLED
    OrderStatusG.COMPLETED -> OrderModelStatus.COMPLETED
    OrderStatusG.DELIVERING -> OrderModelStatus.DELIVERING
    OrderStatusG.PENDING_DELIVERY -> OrderModelStatus.PENDING_DELIVERY
    OrderStatusG.PENDING_DISPATCH -> OrderModelStatus.PENDING_DISPATCH
    OrderStatusG.PENDING_PAYMENT -> OrderModelStatus.PENDING_PAYMENT
    OrderStatusG.REFUNDED -> OrderModelStatus.REFUNDED
  }

/**
 * 领域收款方式 → GraphQL PaymentType(R-MKT 收款方式快照)
 */
fun PaymentType.toGraphQL(): PaymentTypeG =
  when (this) {
    PaymentType.WATER_TICKET -> PaymentTypeG.WATER_TICKET
    PaymentType.CASH -> PaymentTypeG.CASH
    PaymentType.QR_CODE -> PaymentTypeG.QR_CODE
  }

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
    deliveryTimeSlot = deliveryTimeSlot?.let { it.toGraphQL() },
    // deliveryPhotos 为 JSON 字符串,GraphQL 为 List<String>;解析失败兜底空列表
    deliveryPhotos =
      deliveryPhotos?.let { photos ->
        runCatching {
          jacksonObjectMapper().readValue(photos, Array<String>::class.java).toList()
        }.getOrElse { emptyList() }
      },
    // 配送照片核验留痕(G23,领域枚举 → GraphQL 枚举同名互转)
    deliveryPhotoReviewStatus = deliveryPhotoReviewStatus?.let { it.toGraphQL() },
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
    deliveryTimeSlot = deliveryTime?.let { it.toDomain() },
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
 * 枚举跨包同名枚举 when 穷举互转;extraData 落库为 JSON 文本,序列化前还原为 ObjectNode
 * (JsonObjectScalar 亦接受 String,但 Vo 字段类型为 ObjectNode,故在 mapper 内转换)。
 */
fun OrderOperationModel.toGraphQL(): OrderOperationVo =
  OrderOperationVo(
    id = id ?: 0L,
    orderId = orderId,
    operationType = operationType.toGraphQL(),
    operatorId = operatorId,
    operatorType = operatorType.toGraphQL(),
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

private fun DomainDeliveryTimeSlot.toGraphQL(): DeliveryTimeSlotG =
  when (this) {
    DomainDeliveryTimeSlot.IMMEDIATE -> DeliveryTimeSlotG.IMMEDIATE
    DomainDeliveryTimeSlot.MORNING -> DeliveryTimeSlotG.MORNING
    DomainDeliveryTimeSlot.AFTERNOON -> DeliveryTimeSlotG.AFTERNOON
    DomainDeliveryTimeSlot.EVENING -> DeliveryTimeSlotG.EVENING
  }

private fun dev.yidafu.aqua.common.domain.model.enums.DeliveryPhotoReviewStatus.toGraphQL(): DeliveryPhotoReviewStatusG =
  when (this) {
    dev.yidafu.aqua.common.domain.model.enums.DeliveryPhotoReviewStatus.PENDING -> DeliveryPhotoReviewStatusG.PENDING
    dev.yidafu.aqua.common.domain.model.enums.DeliveryPhotoReviewStatus.APPROVED -> DeliveryPhotoReviewStatusG.APPROVED
    dev.yidafu.aqua.common.domain.model.enums.DeliveryPhotoReviewStatus.REJECTED -> DeliveryPhotoReviewStatusG.REJECTED
  }

private fun DeliveryTimeSlotG.toDomain(): DomainDeliveryTimeSlot =
  when (this) {
    DeliveryTimeSlotG.AFTERNOON -> DomainDeliveryTimeSlot.AFTERNOON
    DeliveryTimeSlotG.EVENING -> DomainDeliveryTimeSlot.EVENING
    DeliveryTimeSlotG.IMMEDIATE -> DomainDeliveryTimeSlot.IMMEDIATE
    DeliveryTimeSlotG.MORNING -> DomainDeliveryTimeSlot.MORNING
  }

private fun dev.yidafu.aqua.common.domain.model.enums.OrderOperationType.toGraphQL(): OrderOperationTypeG =
  when (this) {
    dev.yidafu.aqua.common.domain.model.enums.OrderOperationType.ORDER_CREATED -> OrderOperationTypeG.ORDER_CREATED
    dev.yidafu.aqua.common.domain.model.enums.OrderOperationType.ORDER_PAID -> OrderOperationTypeG.ORDER_PAID
    dev.yidafu.aqua.common.domain.model.enums.OrderOperationType.ORDER_CANCELLED -> OrderOperationTypeG.ORDER_CANCELLED
    dev.yidafu.aqua.common.domain.model.enums.OrderOperationType.ORDER_URGING -> OrderOperationTypeG.ORDER_URGING
    dev.yidafu.aqua.common.domain.model.enums.OrderOperationType.DELIVERY_ASSIGNED -> OrderOperationTypeG.DELIVERY_ASSIGNED
    dev.yidafu.aqua.common.domain.model.enums.OrderOperationType.DELIVERY_STARTED -> OrderOperationTypeG.DELIVERY_STARTED
    dev.yidafu.aqua.common.domain.model.enums.OrderOperationType.DELIVERY_COMPLETED -> OrderOperationTypeG.DELIVERY_COMPLETED
    dev.yidafu.aqua.common.domain.model.enums.OrderOperationType.ORDER_CONFIRMED -> OrderOperationTypeG.ORDER_CONFIRMED
    dev.yidafu.aqua.common.domain.model.enums.OrderOperationType.AUTO_CONFIRMED -> OrderOperationTypeG.AUTO_CONFIRMED
    dev.yidafu.aqua.common.domain.model.enums.OrderOperationType.ORDER_COMPLETED -> OrderOperationTypeG.ORDER_COMPLETED
    dev.yidafu.aqua.common.domain.model.enums.OrderOperationType.REFUND_INITIATED -> OrderOperationTypeG.REFUND_INITIATED
    dev.yidafu.aqua.common.domain.model.enums.OrderOperationType.REFUND_COMPLETED -> OrderOperationTypeG.REFUND_COMPLETED
    dev.yidafu.aqua.common.domain.model.enums.OrderOperationType.PAYMENT_TIMEOUT -> OrderOperationTypeG.PAYMENT_TIMEOUT
    dev.yidafu.aqua.common.domain.model.enums.OrderOperationType.DELIVERY_PHOTO_APPROVED -> OrderOperationTypeG.DELIVERY_PHOTO_APPROVED
    dev.yidafu.aqua.common.domain.model.enums.OrderOperationType.DELIVERY_PHOTO_REJECTED -> OrderOperationTypeG.DELIVERY_PHOTO_REJECTED
    dev.yidafu.aqua.common.domain.model.enums.OrderOperationType.ADDRESS_UPDATED -> OrderOperationTypeG.ADDRESS_UPDATED
  }

private fun dev.yidafu.aqua.common.domain.model.enums.OperatorType.toGraphQL(): OperatorTypeG =
  when (this) {
    dev.yidafu.aqua.common.domain.model.enums.OperatorType.USER -> OperatorTypeG.USER
    dev.yidafu.aqua.common.domain.model.enums.OperatorType.ADMIN -> OperatorTypeG.ADMIN
    dev.yidafu.aqua.common.domain.model.enums.OperatorType.DELIVERY_WORKER -> OperatorTypeG.DELIVERY_WORKER
    dev.yidafu.aqua.common.domain.model.enums.OperatorType.SYSTEM -> OperatorTypeG.SYSTEM
  }
