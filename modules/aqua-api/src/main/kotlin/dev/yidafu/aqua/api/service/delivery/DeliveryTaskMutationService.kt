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

package dev.yidafu.aqua.api.service.delivery

import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.enums.PaymentType

/**
 * 配送任务变更服务接口
 */
interface DeliveryTaskMutationService {
  /**
   * 分配送水员给订单
   * @param orderId 订单ID
   * @param workerId 配送员ID
   * @param isSelfCollect 是否自收（水钱已收/水票已扣）
   */
  fun assignDeliveryWorker(
    adminId: Long,
    orderId: Long,
    workerId: Long,
    isSelfCollect: Boolean,
  ): OrderModel

  /**
   * 批量分配订单给配送员
   * @param orderIds 订单ID列表
   * @param workerId 配送员ID
   */
  fun batchAssignOrders(
    adminId: Long,
    orderIds: List<Long>,
    workerId: Long,
  ): List<OrderModel>

  /**
   * 配送员接单
   * @param orderId 订单ID
   * @param adminId 配送员ID
   */
  fun acceptDelivery(
    orderId: Long,
    adminId: Long,
  ): OrderModel

  /**
   * 开始配送（配送员点击开始配送按钮）
   * @param orderId 订单ID
   */
  fun startDelivery(orderId: Long): OrderModel

  /**
   * 完成配送任务
   * @param orderId 订单ID
   * @param deliveryPhotos 配送照片列表
   * @param paymentType 收款方式（非自收订单需要记录）
   * @param remark 配送员备注
   */
  fun completeDelivery(
    orderId: Long,
    deliveryPhotos: List<String>,
    paymentType: PaymentType?,
    remark: String?,
  ): OrderModel
}
