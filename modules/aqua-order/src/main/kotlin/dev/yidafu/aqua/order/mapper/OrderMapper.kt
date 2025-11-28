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

import dev.yidafu.aqua.api.dto.OrderDTO
import dev.yidafu.aqua.common.domain.model.Order
import org.springframework.stereotype.Component
import tech.mappie.api.ObjectMappie

@Component
object OrderMapper : ObjectMappie<Order, OrderDTO>() {
  override fun map(from: Order): OrderDTO =
    mapping {
      // 自动映射基础字段（id, orderNumber, userId, productId, quantity, amount, status）
      // paymentMethod, paymentTransactionId, deliveryWorkerId, paymentTime, completedAt, createdAt, updatedAt
      // deliveryAddressId 和 totalAmount 字段名称相同，会自动映射

      // 注意：以下字段在 Order 模型中存在但在 OrderDTO 中被有意排除：
      // - addressId: 使用 deliveryAddressId 替代
      // - deliveryPhotos: 内部使用，不暴露给客户端
    }
}
