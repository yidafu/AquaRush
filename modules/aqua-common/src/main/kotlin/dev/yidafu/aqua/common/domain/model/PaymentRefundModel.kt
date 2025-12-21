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

import dev.yidafu.aqua.common.graphql.generated.RefundStatus
import jakarta.persistence.*
import java.time.LocalDateTime

import org.hibernate.annotations.SoftDelete

@Entity
@SoftDelete(columnName = "is_deleted")
@Table(name = "payment_refunds")
data class PaymentRefundModel(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,
  @Column(name = "payment_id", nullable = false)
  val paymentId: Long,
  @Column(name = "refund_id", unique = true)
  var refundId: String? = null,
  @Column(name = "out_refund_no", unique = true)
  var outRefundNo: String? = null,
  @Column(name = "refund_amount_cents", nullable = false)
  var refundAmount: Long,
  @Column(name = "refund_reason")
  var refundReason: String? = null,
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  var status: RefundStatus = RefundStatus.PENDING,
  @Column(name = "refund_account")
  var refundAccount: String? = null,
  @Column(name = "refunded_at")
  var refundedAt: LocalDateTime? = null,
  @Column(name = "created_at", nullable = false)
  val createdAt: LocalDateTime = LocalDateTime.now(),
  @Column(name = "updated_at", nullable = false)
  var updatedAt: LocalDateTime = LocalDateTime.now(),
@Column(name = "deleted_at")
  override var deletedAt: LocalDateTime? = null,

  @Column(name = "deleted_by")
  override var deletedBy: Long? = null
) : SoftDeletable
