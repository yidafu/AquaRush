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
import dev.yidafu.aqua.common.domain.model.enums.TicketPurchaseStatus
import jakarta.persistence.*
import org.hibernate.annotations.SoftDelete
import java.time.LocalDateTime

/**
 * 购票单(PRD 10-marketing.md §F-MKT-007/R-MKT-08):
 * 购票即预收款项,一经购买不支持退款(不退现、不支持退票退款);
 * orderNo 作为微信支付 out_trade_no;支付回调成功后按 PENDING→PAID 原子流转并票包入账(totalCount = N+M),幂等。
 */
@Entity
@SoftDelete(columnName = "is_deleted")
@Table(
  name = "ticket_purchases",
  indexes = [
    Index(name = "idx_ticket_purchases_user_status", columnList = "user_id,payment_status"),
  ],
)
open class TicketPurchaseModel(
  @Id
  @SnowflakeIdGenerator
  @Column(name = "id", nullable = false, updatable = false)
  var id: Long? = null,
  @Column(name = "user_id", nullable = false)
  val userId: Long = 0,
  @Column(name = "ticket_type_id", nullable = false)
  val ticketTypeId: Long = 0,
  @Column(name = "order_no", nullable = false, unique = true, length = 64)
  var orderNo: String = "",
  @Column(name = "package_count", nullable = false)
  var packageCount: Int = 0,
  @Column(name = "gift_count", nullable = false)
  var giftCount: Int = 0,
  @Column(name = "total_count", nullable = false)
  var totalCount: Int = 0,
  @Column(name = "package_price_cents", nullable = false)
  var packagePriceCents: Long = 0,
  @Column(name = "payment_status", nullable = false, length = 30)
  @Enumerated(EnumType.STRING)
  var paymentStatus: TicketPurchaseStatus = TicketPurchaseStatus.PENDING,
  @Column(name = "transaction_id", length = 64)
  var transactionId: String? = null,
  @Column(name = "paid_at")
  var paidAt: LocalDateTime? = null,
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
