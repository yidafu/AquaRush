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

package dev.yidafu.aqua.payment.mapper

import dev.yidafu.aqua.api.dto.PaymentDTO
import dev.yidafu.aqua.common.domain.model.Payment
import org.springframework.stereotype.Component
import tech.mappie.api.ObjectMappie

@Component
object PaymentMapper : ObjectMappie<Payment, PaymentDTO>() {
  override fun map(from: Payment): PaymentDTO =
    mapping {
      // 自动映射基础字段（id, orderId, amount, transactionId, status, paidAt）
      // paymentMethod 字段名称相同，会自动映射

      // 注意：以下字段在 Payment 模型中存在但在 PaymentDTO 中被有意排除：
      // - userId: 敏感信息，不暴露给客户端
      // - prepayId: 支付预交易ID，内部使用
      // - currency: 固定为 CNY，不需要暴露
      // - description: 支付描述，内部使用
      // - failureReason: 支付失败原因，包含敏感信息
      // - expiredAt: 支付过期时间，内部使用
      // - createdAt, updatedAt: 审计字段，客户端不需要
    }
}
