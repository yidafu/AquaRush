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
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.domain.model.enums.PaymentType
import dev.yidafu.aqua.common.domain.model.enums.PriceSource
import dev.yidafu.aqua.common.graphql.generated.CreateDeliveryOrderInput
import dev.yidafu.aqua.common.graphql.generated.CreateOrderInput
import dev.yidafu.aqua.common.graphql.generated.DailyStat
import dev.yidafu.aqua.common.graphql.generated.DeliveryAddress
import dev.yidafu.aqua.common.graphql.generated.Order
import dev.yidafu.aqua.common.graphql.generated.OrderItemVo
import dev.yidafu.aqua.common.graphql.generated.Product
import dev.yidafu.aqua.common.graphql.generated.ProductStatus
import dev.yidafu.aqua.common.graphql.generated.TodayStatistics
import dev.yidafu.aqua.common.graphql.generated.UpdateOrderAddressInput
import dev.yidafu.aqua.common.graphql.generated.WeekStatistics
import dev.yidafu.aqua.delivery.mapper.DeliveryWorkerMapper
import dev.yidafu.aqua.order.dto.OrderDTO
import dev.yidafu.aqua.order.dto.UpdateOrderStatusDTO
import dev.yidafu.aqua.user.mapper.toGraphQL
import tech.mappie.api.EnumMappie
import tech.mappie.api.ObjectMappie
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDateTime
import dev.yidafu.aqua.common.graphql.generated.DeliveryPhotoReviewStatus as DeliveryPhotoReviewStatusG
import dev.yidafu.aqua.common.graphql.generated.DeliveryTimeSlot as DeliveryTimeSlotG
import dev.yidafu.aqua.common.graphql.generated.OrderStatus as OrderStatusG
import dev.yidafu.aqua.common.graphql.generated.PaymentType as PaymentTypeG
import dev.yidafu.aqua.common.graphql.generated.PriceSource as PriceSourceG

// ============================================================================
// OrderModel ↔ OrderDTO Mappers
// ============================================================================

/**
 * Enum mapper for converting domain PaymentType to GraphQL PaymentType
 */
object PaymentTypeMapper : EnumMappie<PaymentType, PaymentTypeG>()

/**
 * Enum mapper for converting domain PriceSource to GraphQL PriceSource(R-MKT-01 成交单价来源)
 */
object PriceSourceMapper : EnumMappie<PriceSource, PriceSourceG>()

/**
 * Mapper for converting OrderModel to OrderDTO
 */
object OrderModelToDTOMapper : ObjectMappie<OrderModel, OrderDTO>() {
  override fun map(from: OrderModel) =
    mapping {
      to::id fromValue (from.id ?: 0L)
      // Fields with same name and type - auto-mapped by Mappie
      to::amount fromProperty from::amountCents
      to::deliveryPhotos fromExpression {
        from.deliveryPhotos?.let { photos ->
          try {
            jacksonObjectMapper().readValue(photos, Array<String>::class.java).toList()
          } catch (e: Exception) {
            emptyList()
          }
        }
      }
      to::totalAmount fromProperty from::amountCents
      // Nested objects are set to null and should be populated by the service layer
      to::user fromValue null
      to::address fromValue null
      to::deliveryWorker fromValue null
    }
}

/**
 * Mapper for applying UpdateOrderStatusDTO to OrderModel
 */
object UpdateOrderStatusMapper : ObjectMappie<UpdateOrderStatusDTO, OrderModel>() {
  override fun map(from: UpdateOrderStatusDTO) =
    mapping {
      to::id fromValue -1L // This needs to be set by the calling code
      to::orderNo fromValue "" // This needs to be set by the calling code
      to::userId fromValue -1L // This needs to be set by the calling code
      to::amountCents fromValue 0L // This needs to be set by the calling code
      to::addressId fromValue -1L // This needs to be set by the calling code
      to::status fromExpression { from.status }
      to::deliveryPhotos fromExpression {
        from.deliveryPhotos?.let { photos ->
          jacksonObjectMapper().writeValueAsString(photos)
        }
      }
      to::createdAt fromValue LocalDateTime.now()
      to::updatedAt fromValue LocalDateTime.now()
      to::deliveryStartedAt fromValue null
      to::deliveryConfirmedAt fromValue null
      to::isSelfCollect fromValue false
      to::paymentType fromValue null
    }
}

// ============================================================================
// OrderDTO ↔ GraphQL Order Mappers
// ============================================================================

/**
 * Mapper for converting OrderDTO to GraphQL Order type
 */
object OrderDTOToGraphQLMapper : ObjectMappie<OrderDTO, Order>() {
  override fun map(from: OrderDTO) =
    mapping {
      // Note: GraphQL Order requires non-null nested objects (address, product, user)
      // This mapper should only be used when nested objects are properly populated
      to::amountCents fromProperty from::amount
      to::deliveryPhotos fromExpression { from.deliveryPhotos }
      // DTO 链路不承载配送备注(权威值由 OrderModel 直映 OrderMapper 提供 —— R-ORD-07)
      to::deliveryRemark fromValue null
      to::paymentMethod fromExpression { from.paymentMethod?.name }
      to::status fromExpression { OrderStatusG.valueOf(from.status.name) }
      // These are required by GraphQL Order constructor - use requireNotNull
      to::address fromExpression { requireNotNull(from.address) { "address must not be null" } }
      to::user fromExpression { requireNotNull(from.user) { "user must not be null" } }
      // 商品明细(F-ORD-004)由 OrderMapper 直映 OrderModel 提供,DTO 链路不承载
      to::items fromValue emptyList()
      // DTO 链路不承载配送时段(AC-ORD-001-05,由 OrderModel 直映提供)
      to::deliveryTimeSlot fromValue null
      // DTO 无首单立减/赠品快照(由 OrderModel 直映或字段级 Resolver 提供 —— R-MKT-09/10)
      to::firstOrderDiscountCents fromValue 0L
      to::giftItems fromValue null
      to::promotionDiscountCents fromValue 0L
      to::couponDiscountCents fromValue 0L
      // 营销快照/核验/押金字段:DTO 链路不承载,统一兜底默认值(权威值由 OrderModel 直映提供)
      to::couponName fromValue null
      to::promotionActivityName fromValue null
      to::firstOrderActivityName fromValue null
      to::ticketDiscountCents fromValue 0L
      to::ticketRedeemCount fromValue 0
      to::depositCents fromValue 0L
      to::deliveryPhotoReviewStatus fromValue null
      to::deliveryPhotoReviewedAt fromValue null
      to::deliveryPhotoReviewedBy fromValue null
      to::deliveryPhotoReviewRemark fromValue null
    }
}

/**
 * Mapper for converting GraphQL Order type to OrderDTO
 */
object GraphQLToOrderDTOMapper : ObjectMappie<Order, OrderDTO>() {
  override fun map(from: Order) =
    mapping {
      to::userId fromExpression { from.user?.id ?: 0L }
      to::user fromValue from.user
      to::amount fromProperty from::amountCents
      to::addressId fromExpression { from.address?.id ?: 0L }
      to::address fromValue from.address
      to::status fromExpression {
        GenerateOrderStatusMapper.map(from.status)
      }
      to::paymentMethod fromExpression {
        when (from.paymentMethod) {
          "WECHAT_PAY" -> dev.yidafu.aqua.common.domain.model.PaymentMethod.WECHAT_PAY
          else -> null
        }
      }
      to::deliveryWorkerId fromExpression { from.deliveryWorker?.id }
      to::deliveryWorker fromValue from.deliveryWorker
      to::deliveryPhotos fromExpression { from.deliveryPhotos?.toList() }
      to::totalAmount fromProperty from::amountCents
    }
}

// ============================================================================
// OrderModel → GraphQL Order Direct Mapper (with placeholders)
// ============================================================================
object OrderModelStatusMapper : EnumMappie<OrderModelStatus, OrderStatusG>()

object OrderStatusMapper : EnumMappie<OrderStatusG, OrderModelStatus>()

object GenerateOrderStatusMapper : EnumMappie<OrderStatusG, OrderModelStatus>()

/**
 * Simple OrderMapper that converts OrderModel to GraphQL Order
 * Note: This mapper creates placeholder objects for required nested fields
 * In a complete implementation, these should be loaded from their respective services
 */
object OrderMapper : ObjectMappie<OrderModel, Order>() {
  override fun map(from: OrderModel) =
    mapping {
      to::id fromValue (from.id ?: 0L)
      // Create placeholder objects for required nested fields
      // In a real implementation, these should be loaded from their respective services
      to::amountCents fromProperty from::amountCents
      // 配送备注(完成配送时配送员填写,随 completeDelivery 落库 —— R-ORD-07)
      to::deliveryRemark fromProperty from::deliveryRemark
      to::deliveryPhotos fromExpression {
        from.deliveryPhotos?.let { photos ->
          try {
            jacksonObjectMapper().readValue(photos, Array<String>::class.java).toList()
          } catch (e: Exception) {
            emptyList()
          }
        }
      }
      to::paymentMethod fromExpression { from.paymentMethod?.name }
      to::status fromExpression {
        OrderModelStatusMapper.map(from.status)
      }
      // Create placeholder objects for required nested fields
      to::address fromExpression { from.address?.let { it.toGraphQL() } }

      to::deliveryWorker fromExpression { from.deliveryWorker?.let { DeliveryWorkerMapper.map(it) } }
      to::user fromExpression {
        from.user?.let { it.toGraphQL() }
      }
      to::paymentType fromExpression { from.paymentType?.let { PaymentTypeMapper.map(it) } }
      // 配送时段(AC-ORD-001-05,领域枚举 → GraphQL 枚举同名互转;null=未指定)
      to::deliveryTimeSlot fromExpression { from.deliveryTimeSlot?.let { DeliveryTimeSlotG.valueOf(it.name) } }
      // 商品明细(F-ORD-004)由调用方经 OrderItemRepository 填充(见 OrderItemModel.toOrderItemVo)
      to::items fromValue emptyList()
      // 赠品行由字段级 Resolver 联查 order_gifts 提供(R-MKT-09,配置变更不影响已生成订单)
      to::giftItems fromValue null
      // 营销快照(R-MKT-01/02/07/08/10:订单快照直映,配置变更不影响已生成订单)
      to::couponName fromProperty from::couponName
      to::promotionActivityName fromProperty from::promotionActivityName
      to::firstOrderActivityName fromProperty from::firstOrderActivityName
      to::ticketRedeemCount fromProperty from::ticketRedeemCount
      to::ticketDiscountCents fromProperty from::ticketDiscountCents
      // 桶押金快照(R-MON-03/G06)
      to::depositCents fromProperty from::depositCents
      // 配送照片核验留痕(G23,领域枚举 → GraphQL 枚举同名互转)
      to::deliveryPhotoReviewStatus fromExpression {
        from.deliveryPhotoReviewStatus?.let { DeliveryPhotoReviewStatusG.valueOf(it.name) }
      }
      to::deliveryPhotoReviewedAt fromProperty from::deliveryPhotoReviewedAt
      to::deliveryPhotoReviewedBy fromProperty from::deliveryPhotoReviewedBy
      to::deliveryPhotoReviewRemark fromProperty from::deliveryPhotoReviewRemark
    }
}

/**
 * Mapper for converting DeliveryAddressModel domain entity to GraphQL DeliveryAddress type
 */
object DeliveryAddressMapper : ObjectMappie<DeliveryAddressModel, DeliveryAddress>() {
  override fun map(from: DeliveryAddressModel) =
    mapping {
      to::id fromValue (from.id ?: 0L)
      to::latitude fromValue from.latitude?.toFloat()
      to::longitude fromValue from.longitude?.toFloat()
      // Most fields map automatically by name
      // Note: Mappie automatically handles Double to Float conversion for nullable fields
    }
}

object CreateDeliveryOrderInputMapper : ObjectMappie<CreateDeliveryOrderInput, CreateOrderRequest>() {
  override fun map(from: CreateDeliveryOrderInput): CreateOrderRequest =
    mapping {
      to::userId fromValue 0L
      to::isSelfCollect fromProperty from::isSelfCollect
      // 代客下单(F-ORD-012)多商品:items 直传(F-ORD-004 多商品唯一口径;单商品=单元素)
      to::items fromExpression {
        from.items.map { OrderItemRequest(productId = it.productId, quantity = it.quantity) }
      }
    }
}

/**
 * 送水端订单页地址信息修改输入映射(F-ORD-024)
 * GraphQL 经纬度为 Float,领域模型/存储统一 Double(WGS-84)。
 */
object UpdateOrderAddressInputMapper : ObjectMappie<UpdateOrderAddressInput, UpdateOrderAddressRequest>() {
  override fun map(from: UpdateOrderAddressInput): UpdateOrderAddressRequest =
    mapping {
      to::receiverName fromProperty from::receiverName
      to::phone fromProperty from::phone
      to::detailAddress fromProperty from::detailAddress
      to::floor fromProperty from::floor
      to::longitude fromValue from.longitude?.toDouble()
      to::latitude fromValue from.latitude?.toDouble()
    }
}

object CreateOrderInputMapper : ObjectMappie<CreateOrderInput, CreateOrderRequest>() {
  override fun map(from: CreateOrderInput): CreateOrderRequest =
    mapping {
      to::userId fromValue 0L
      // 商品明细(F-ORD-004 多商品下单;单商品=单元素)
      to::items fromExpression {
        from.items.map { OrderItemRequest(productId = it.productId, quantity = it.quantity) }
      }
      // 服务端下单幂等键(R-ORD-06)
      to::clientRequestId fromProperty from::clientRequestId
      // 配送时段(AC-ORD-001-05,缺省=立即配送 IMMEDIATE;GraphQL 枚举 → 领域枚举同名互转)
      to::deliveryTimeSlot fromExpression {
        from.deliveryTime?.let {
          dev.yidafu.aqua.common.domain.model.enums.DeliveryTimeSlot
            .valueOf(it.name)
        }
      }
      // 促销引擎(R-MKT-07 叠加③):省略=引擎自动选最优,0=不用券;一单一券
      to::preferredCouponId fromProperty from::preferredCouponId
      // 水票核销张数(一票抵一桶 R-MKT-08)
      to::ticketRedeemCount fromValue (from.ticketRedeemCount ?: 0)
    }
}

object TodayStatisticsMapper : ObjectMappie<TodayStatisticsDTO, TodayStatistics>()

object WeekStatisticsMapper : ObjectMappie<WeekStatisticsDTO, WeekStatistics>() {
  override fun map(from: WeekStatisticsDTO): WeekStatistics =
    mapping {
      to::dailyStats fromExpression { DailyStatMapper.mapList(from.dailyStats) }
      to::totalEarningCents fromValue from.totalEarningCents
    }
}

object DailyStatMapper : ObjectMappie<DailyStatDTO, DailyStat>() {
  override fun map(from: DailyStatDTO): DailyStat =
    mapping {
      to::earningCents fromValue from.earningCents
    }
}

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
      Product(
        certificateImages = null,
        coverImageUrl = snapshot?.get("coverImageUrl")?.toString() ?: "",
        createdAt = createdAt,
        detailContent = null,
        effectivePriceCents = unitPriceCents,
        hasGroupPrice = false,
        priceSource = dev.yidafu.aqua.common.graphql.generated.PriceSource.CATALOG_PRICE,
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
