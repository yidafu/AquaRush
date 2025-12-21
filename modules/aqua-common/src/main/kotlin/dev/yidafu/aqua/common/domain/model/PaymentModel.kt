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

package dev.yidafu.aqua.common.domain.model

import jakarta.persistence.*
import java.time.LocalDateTime

import org.hibernate.annotations.SoftDelete

@Entity
@SoftDelete(columnName = "is_deleted")
@Table(name = "payments")
open class  PaymentModel(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,
  @Column(name = "order_id", nullable = false)
  val orderId: Long,
  @Column(name = "user_id", nullable = false)
  val userId: Long,
  @Column(name = "transaction_id", unique = true)
  var transactionId: String? = null,
  @Column(name = "prepay_id", unique = true)
  var prepayId: String? = null,
  @Column(name = "amount_cents", nullable = false)
  var amount: Long,
  @Column(name = "currency", nullable = false)
  var currency: String = "CNY",
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  var status: PaymentStatus = PaymentStatus.PENDING,
  @Enumerated(EnumType.STRING)
  @Column(name = "payment_method", nullable = false)
  var paymentMethod: PaymentMethod = PaymentMethod.WECHAT_PAY,
  @Column(name = "description")
  var description: String? = null,
  @Column(name = "failure_reason")
  var failureReason: String? = null,
  @Column(name = "paid_at")
  var paidAt: LocalDateTime? = null,
  @Column(name = "expired_at")
  var expiredAt: LocalDateTime? = null,
  @Column(name = "created_at", nullable = false)
  val createdAt: LocalDateTime = LocalDateTime.now(),
  @Column(name = "updated_at", nullable = false)
  var updatedAt: LocalDateTime = LocalDateTime.now(),

  @Column(name = "deleted_at")
  override var deletedAt: LocalDateTime? = null,

  @Column(name = "deleted_by")
  override var deletedBy: Long? = null
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
}
