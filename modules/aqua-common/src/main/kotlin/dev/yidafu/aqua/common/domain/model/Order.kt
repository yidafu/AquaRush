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
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "orders")
data class Order(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,
  @Column(name = "order_number", unique = true, nullable = false)
  val orderNumber: String,
  @Column(name = "user_id", nullable = false)
  val userId: Long,
  @Column(name = "product_id", nullable = false)
  val productId: Long,
  @Column(name = "quantity", nullable = false)
  val quantity: Int,
  @Column(name = "amount", nullable = false, precision = 10, scale = 2)
  val amount: BigDecimal,
  @Column(name = "address_id", nullable = false)
  val addressId: Long,
  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  var status: OrderStatus = OrderStatus.PENDING_PAYMENT,
  @Column(name = "payment_method")
  @Enumerated(EnumType.STRING)
  var paymentMethod: PaymentMethod? = null,
  @Column(name = "payment_transaction_id")
  var paymentTransactionId: String? = null,
  @Column(name = "payment_time")
  var paymentTime: LocalDateTime? = null,
  @Column(name = "delivery_worker_id")
  var deliveryWorkerId: Long? = null,
  @Column(name = "delivery_photos", columnDefinition = "jsonb")
  var deliveryPhotos: String? = null,
  @Column("delivery_address_id")
  val deliveryAddressId: Long,
  @Column(name = "completed_at")
  var completedAt: LocalDateTime? = null,
  @Column(name = "total_amount")
  var totalAmount: BigDecimal,
  @Column(name = "created_at", nullable = false, updatable = false)
  val createdAt: LocalDateTime = LocalDateTime.now(),
  @Column(name = "updated_at", nullable = false)
  var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
  @PreUpdate
  fun preUpdate() {
    updatedAt = LocalDateTime.now()
  }
}

enum class OrderStatus {
  PENDING_PAYMENT, // 待支付
  PENDING_DELIVERY, // 待配送
  DELIVERING, // 配送中
  COMPLETED, // 已完成
  CANCELLED, // 已取消
}
