package dev.yidafu.aqua.api.service.order

import dev.yidafu.aqua.api.dto.MarketingUsageItemRow
import dev.yidafu.aqua.api.dto.MarketingUsageKind
import dev.yidafu.aqua.api.dto.MarketingUsageOrderCriteria
import dev.yidafu.aqua.common.domain.model.OrderItemModel
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import org.springframework.data.domain.Page
import java.time.LocalDateTime

/*
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

/**
 * 跨模块订单查询服务接口
 * 由 aqua-order 模块实现，供其他模块使用
 */
interface OrderQueryApiService {
  fun findById(id: Long): OrderModel?

  /**
   * 查询订单商品明细(F-ORD-004 多商品下单,order_items 表):
   * 订单商品以明细为准,按创建时间升序返回。
   */
  fun findItemsByOrderId(orderId: Long): List<OrderItemModel>

  fun findByIds(ids: List<Long>): List<OrderModel>

  fun findByOrderNumber(orderNumber: String): OrderModel?

  fun findByUserId(userId: Long): List<OrderModel>

  fun findByUserIdAndStatus(
    userId: Long,
    status: OrderModelStatus,
  ): List<OrderModel>

  /**
   * 用户订单分页查询(用户端订单列表,F-ORD-002,PRD 04-order.md):
   * statuses 为空=全部状态;「待发货」口径=待派单+待配送(PENDING_DISPATCH/PENDING_DELIVERY);
   * 按下单时间倒序;userId 由调用方强制为登录用户(防越权)。
   */
  fun findByUserIdPaginated(
    userId: Long,
    statuses: List<OrderModelStatus>?,
    page: Int,
    size: Int,
  ): Page<OrderModel>

  /**
   * 用户是否已有已完成订单(PRD 10-marketing.md §R-MKT-10 新用户判定=无已完成订单)
   */
  fun hasCompletedOrders(userId: Long): Boolean

  fun findByStatus(status: OrderModelStatus): List<OrderModel>

  fun findByDeliveryWorkerId(deliveryWorkerId: Long): List<OrderModel>

  fun findByCreatedAtBetween(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): List<OrderModel>

  fun findByStatusAndCreatedAtBetween(
    statuses: List<OrderModelStatus>,
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): List<OrderModel>

  fun countByStatusAndCreatedAtBetween(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
    deliveryWorkerId: Long?,
    statuses: List<OrderModelStatus>?,
  ): Long

  /**
   * 按订单状态分组统计指定用户的订单数(F-USR-002,G31 我的页订单角标):
   * 仅本人订单;缺失状态不计入(调用方补 0)。
   */
  fun countByUserIdGroupByStatus(userId: Long): Map<OrderModelStatus, Long>

  fun sumAmountCentsByStatusAndCreatedAtBetween(
    statuses: List<OrderModelStatus>,
    startDate: LocalDateTime,
    endDate: LocalDateTime,
    deliveryWorkerId: Long?,
  ): Long

  // ========================================================================
  // 营销使用情况(PRD 10-marketing.md 营销方式详情:相关订单记录 + 营销使用汇总)
  // ========================================================================

  /**
   * 按营销追溯条件分页查询相关订单(营销方式详情「相关订单记录」,按下单时间倒序)。
   * 匹配语义见 [MarketingUsageKind];限时特价/新人专享价按商品+活动价+时间窗匹配 order_items。
   */
  fun findOrdersByMarketingUsage(
    criteria: MarketingUsageOrderCriteria,
    page: Int,
    size: Int,
  ): Page<OrderModel>

  /**
   * 营销使用订单数(与 [findOrdersByMarketingUsage] 同口径的汇总计数)。
   */
  fun countOrdersByMarketingUsage(criteria: MarketingUsageOrderCriteria): Long

  /**
   * 营销优惠金额合计(分,订单快照列口径):
   * 券=coupon_discount_cents、满减=promotion_discount_cents、首单立减=first_order_discount_cents;
   * 限时特价/新人专享价/买赠满赠无订单列优惠,返回 0,让利金额由 [aggregateMarketingUsageItems] 计算。
   */
  fun sumMarketingDiscountCents(criteria: MarketingUsageOrderCriteria): Long

  /**
   * 营销使用商品聚合行(让利金额计算用):
   * 限时特价/新人专享价 = order_items 按 (product_id, unit_price_cents) 分组合计数量;
   * 买赠/满赠 = order_gifts 按 product_id 分组合计赠品数量。
   */
  fun aggregateMarketingUsageItems(criteria: MarketingUsageOrderCriteria): List<MarketingUsageItemRow>
}
