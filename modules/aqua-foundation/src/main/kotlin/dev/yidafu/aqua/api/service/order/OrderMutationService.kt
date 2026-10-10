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

package dev.yidafu.aqua.api.service.order

import dev.yidafu.aqua.api.dto.CreateOrderRequest
import dev.yidafu.aqua.api.dto.OrderPricingRequest
import dev.yidafu.aqua.api.dto.OrderPricingResult
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus

/**
 * 订单变更服务接口
 */
interface OrderMutationService {
  /**
   * 创建订单，给普通用户使用
   */
  fun createOrder(input: CreateOrderRequest): OrderModel

  /**
   * 配送员创建订单
   */
  fun createDeliveryOrder(
    adminId: Long,
    input: CreateOrderRequest,
  ): OrderModel

  /**
   * 取消订单
   */
  fun cancelOrder(orderId: Long): OrderModel

  /**
   * 用户取消订单
   */
  fun cancelOrder(
    orderId: Long,
    userId: Long,
  ): OrderModel?

  /**
   * 管理员取消订单
   */
  fun cancelOrderForAdmin(orderId: Long): OrderModel?

  fun updateOrder(
    orderId: Long,
    orderDTO: OrderModel,
  ): OrderModel

  /**
   * 更新订单状态
   */
  fun updateOrderStatus(
    orderId: Long,
    status: OrderModelStatus,
  ): OrderModel

  /**
   * 处理支付成功
   */
  fun handlePaymentSuccess(
    orderId: Long,
    paymentTransactionId: String,
  )

  /**
   * 处理支付超时
   */
  fun handlePaymentTimeout(orderId: Long)

  /**
   * 用户催单(F-ORD-008):仅本人「配送中」订单可催;防抖(短时间重复催单仅一次生效,AC-ORD-008-01);
   * 记录操作时间线并发布 ORDER_URGING 事件(通知域据此向用户/送水工推送)。
   */
  fun urgeOrder(
    orderId: Long,
    userId: Long,
  ): OrderModel?

  /**
   * 确认订单计价预览(F-ORD-023,PRD 10-marketing.md §R-MKT-02/07/08):
   * 委托促销引擎纯计算,无副作用;返回完整费用明细(单价/水票核销/满减/优惠券/封顶)。
   */
  fun confirmOrderPreview(input: OrderPricingRequest): OrderPricingResult
}
