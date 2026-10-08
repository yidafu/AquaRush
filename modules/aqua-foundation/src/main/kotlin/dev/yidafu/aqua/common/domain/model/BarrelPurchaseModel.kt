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

import dev.yidafu.aqua.common.annotation.SnowflakeIdGenerator
import dev.yidafu.aqua.common.domain.model.enums.BarrelPaymentStatus
import jakarta.persistence.*
import org.hibernate.annotations.SoftDelete
import java.time.LocalDateTime

/**
 * 水桶购买单(G06:一次性塑料水桶费,独立支付,与商品订单不捆绑):
 * 每用户总限购一次(存在 PAID 即拦截,PENDING 重复发起先取消旧单,REFUNDED 后可重新购买);
 * orderNo 作为微信支付 out_trade_no(BK 前缀);支付回调成功后按 PENDING→PAID 原子流转并记录交易号,幂等;
 * 退还由管理员手动执行(PENDING/PAID→REFUNDED)。
 */
@Entity
@SoftDelete(columnName = "is_deleted")
@Table(
  name = "barrel_purchases",
  indexes = [
    Index(name = "idx_barrel_purchases_user_status", columnList = "user_id,payment_status"),
    Index(name = "idx_barrel_purchases_paid_at", columnList = "paid_at"),
  ],
)
open class BarrelPurchaseModel(
  @Id
  @SnowflakeIdGenerator
  @Column(name = "id", nullable = false, updatable = false)
  var id: Long? = null,
  @Column(name = "user_id", nullable = false)
  val userId: Long = 0,
  @Column(name = "order_no", nullable = false, unique = true, length = 64)
  var orderNo: String = "",
  @Column(name = "total_amount_cents", nullable = false)
  var totalAmountCents: Long = 0,
  @Column(name = "payment_status", nullable = false, length = 30)
  @Enumerated(EnumType.STRING)
  var paymentStatus: BarrelPaymentStatus = BarrelPaymentStatus.PENDING,
  @Column(name = "transaction_id", length = 64)
  var transactionId: String? = null,
  @Column(name = "paid_at")
  var paidAt: LocalDateTime? = null,
  @Column(name = "refunded_at")
  var refundedAt: LocalDateTime? = null,
  @Column(name = "refunded_by")
  var refundedBy: Long? = null,
  @Column(name = "refund_remark", length = 500)
  var refundRemark: String? = null,
  @Column(name = "remark", length = 500)
  var remark: String? = null,
  @Column(name = "created_at", nullable = false, updatable = false)
  val createdAt: LocalDateTime = LocalDateTime.now(),
  @Column(name = "updated_at", nullable = false)
  var updatedAt: LocalDateTime = LocalDateTime.now(),
  @Column(name = "deleted_at")
  override var deletedAt: LocalDateTime? = null,
  @Column(name = "deleted_by")
  override var deletedBy: Long? = null,
) : SoftDeletable {
  @PreUpdate
  fun preUpdate() {
    updatedAt = LocalDateTime.now()
  }
}
