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

import dev.yidafu.aqua.common.utils.MoneyUtils
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 押桶记录实体
 */
@Entity
@Table(name = "bucket_deposits")
data class BucketDepositModel(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long = -1L,

  @Column(name = "user_id", nullable = false)
  val userId: Long = -1L,

  @Column(name = "quantity", nullable = false)
  val quantity: Int = 1,

  @Column(name = "amount_cents", nullable = false)
  val amountCents: Long = 0L,

  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  var status: BucketDepositStatus = BucketDepositStatus.DEPOSITED,

  @Column(name = "payment_transaction_id")
  var paymentTransactionId: String? = null,

  @Column(name = "payment_time")
  var paymentTime: LocalDateTime? = null,

  @Column(name = "refunded_at")
  var refundedAt: LocalDateTime? = null,

  @Column(name = "refunded_by")
  var refundedBy: Long? = null,

  @Column(name = "remark")
  var remark: String? = null,

  @Column(name = "created_at", nullable = false, updatable = false)
  val createdAt: LocalDateTime = LocalDateTime.now(),

  @Column(name = "updated_at", nullable = false)
  var updatedAt: LocalDateTime = LocalDateTime.now()
) {
  @PreUpdate
  fun preUpdate() {
    updatedAt = LocalDateTime.now()
  }

  // 金额转换为元
  val amount: BigDecimal
    get() = MoneyUtils.fromCents(amountCents)

  // 总金额（数量 × 单价）
  val totalAmountCents: Long
    get() = amountCents * quantity

  val totalAmount: BigDecimal
    get() = MoneyUtils.fromCents(totalAmountCents)
}

/**
 * 押桶状态枚举
 */
enum class BucketDepositStatus {
  DEPOSITED,   // 已押桶（待退还）
  REFUNDED     // 已退还
}
