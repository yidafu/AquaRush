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
import dev.yidafu.aqua.common.domain.model.enums.DepositStatus
import jakarta.persistence.*
import org.hibernate.annotations.SoftDelete
import java.time.LocalDateTime

/**
 * 桶押金记录实体(G06/R-MON-03 押金模块,PRD 14-misc.md §核心实体「押金:用户、金额、状态、退还时间」)
 * 押金随下单一并收取(deposit_records 落账),订单取消自动退还,空桶退回由管理员退还。
 */
@Entity
@SoftDelete(columnName = "is_deleted")
@Table(
  name = "deposit_records",
  indexes = [
    Index(name = "idx_deposit_records_order_id", columnList = "order_id"),
    Index(name = "idx_deposit_records_user_id", columnList = "user_id"),
    Index(name = "idx_deposit_records_status", columnList = "status"),
    Index(name = "idx_deposit_records_collected_at", columnList = "collected_at"),
  ],
)
data class DepositRecordModel(
  @Id
  @SnowflakeIdGenerator
  @Column(name = "id", nullable = false, updatable = false)
  var id: Long? = null,
  @Column(name = "order_id", nullable = false)
  var orderId: Long,
  @Column(name = "user_id", nullable = false)
  var userId: Long,
  // 押金金额(分,R-MON-01)
  @Column(name = "deposit_amount_cents", nullable = false)
  var depositAmountCents: Long,
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  var status: DepositStatus = DepositStatus.COLLECTED,
  // 收取时间
  @Column(name = "collected_at")
  var collectedAt: LocalDateTime? = null,
  // 退还时间
  @Column(name = "refunded_at")
  var refundedAt: LocalDateTime? = null,
  // 退还操作人(账号 id)
  @Column(name = "refunded_by")
  var refundedBy: Long? = null,
  @Column(name = "refund_remark", length = 500)
  var refundRemark: String? = null,
  @Column(name = "created_at", nullable = false, updatable = false)
  val createdAt: LocalDateTime = LocalDateTime.now(),
  @Column(name = "updated_at", nullable = false)
  var updatedAt: LocalDateTime = LocalDateTime.now(),
  @Column(name = "deleted_at")
  override var deletedAt: LocalDateTime? = null,
  @Column(name = "deleted_by")
  override var deletedBy: Long? = null,
) : SoftDeletable
