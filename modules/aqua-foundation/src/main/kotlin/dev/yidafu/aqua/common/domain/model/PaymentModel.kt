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

package dev.yidafu.aqua.common.domain.model

import java.time.LocalDateTime

open class PaymentModel(
  var id: Long? = null,
  val orderId: Long,
  val userId: Long,
  var transactionId: String? = null,
  var prepayId: String? = null,
  var amount: Long,
  var currency: String = "CNY",
  var status: PaymentStatus = PaymentStatus.PENDING,
  var paymentMethod: PaymentMethod = PaymentMethod.WECHAT_PAY,
  var description: String? = null,
  var failureReason: String? = null,
  var paidAt: LocalDateTime? = null,
  var expiredAt: LocalDateTime? = null,
  var version: Long = 0L,
  val createdAt: LocalDateTime = LocalDateTime.now(),
  var updatedAt: LocalDateTime = LocalDateTime.now(),
  override var deletedAt: LocalDateTime? = null,
  override var deletedBy: Long? = null,
) : SoftDeletable

enum class PaymentStatus {
  PENDING, // 待支付
  PROCESSING, // 支付处理中
  SUCCESS, // 支付成功
  FAILED, // 支付失败
  CANCELLED, // 支付取消
  EXPIRED, // 支付过期
  REFUNDING, // 退款中
  REFUNDED, // 已退款
}

enum class PaymentMethod {
  WECHAT_PAY, // 微信支付
  CASH, // 线下现金支付
}
