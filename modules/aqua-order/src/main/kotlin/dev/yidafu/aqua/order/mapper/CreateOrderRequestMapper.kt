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

package dev.yidafu.aqua.order.mapper

import dev.yidafu.aqua.api.dto.CreateOrderRequest
import dev.yidafu.aqua.common.domain.model.Order
import dev.yidafu.aqua.common.domain.model.OrderStatus
import dev.yidafu.aqua.common.id.DefaultIdGenerator
import org.springframework.stereotype.Component
import tech.mappie.api.ObjectMappie
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * CreateOrderRequest 到 Order 的映射器
 */
@Component
object CreateOrderRequestMapper : ObjectMappie<CreateOrderRequest, Order>() {
  override fun map(from: CreateOrderRequest): Order {
    // 由于 Order 需要复杂的构造，我们手动创建对象
    return Order(
      id = DefaultIdGenerator().generate(),
      orderNumber = from.orderNumber,
      userId = from.userId,
      productId = from.productId,
      quantity = from.quantity,
      amount = from.amount,
      addressId = from.addressId,
      deliveryAddressId = from.addressId, // 映射到同一字段
      status = OrderStatus.PENDING_PAYMENT,
      paymentMethod = null,
      paymentTransactionId = null,
      paymentTime = null,
      deliveryWorkerId = null,
      deliveryPhotos = null,
      completedAt = null,
      totalAmount = from.amount, // 映射到同一字段
      createdAt = LocalDateTime.now(),
      updatedAt = LocalDateTime.now(),
    )
  }
}
