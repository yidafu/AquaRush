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
import dev.yidafu.aqua.common.domain.model.enums.UserTicketStatus
import jakarta.persistence.*
import org.hibernate.annotations.SoftDelete
import java.time.LocalDateTime

/**
 * 用户水票实例(PRD 10-marketing.md §F-MKT-007,每票一行):
 * 一票抵一桶,购票入账 N+M 张(赠送票 is_gift 与正常票同池);
 * 每张票带唯一递增票号 ticketNo(格式 SP+入账年月+8位序号,按月重置,见 v2.2.0 迁移);
 * 状态机 UNUSED → USED(核销)→ RETURNED(订单取消/退款退回,未过期可复用)→ USED ... / EXPIRED;
 * 核销幂等:原子条件 UPDATE(status IN (UNUSED,RETURNED) 且未过期),防并发重复使用。
 */
@Entity
@SoftDelete(columnName = "is_deleted")
@Table(
  name = "user_tickets",
  indexes = [
    Index(name = "idx_user_tickets_user_status", columnList = "user_id,status"),
    Index(name = "idx_user_tickets_used_order", columnList = "used_order_id"),
  ],
)
open class UserTicketModel(
  @Id
  @SnowflakeIdGenerator
  @Column(name = "id", nullable = false, updatable = false)
  var id: Long? = null,
  @Column(name = "user_id", nullable = false)
  val userId: Long = 0,
  @Column(name = "ticket_type_id", nullable = false)
  val ticketTypeId: Long = 0,
  @Column(name = "purchase_id", nullable = false)
  val purchaseId: Long = 0,
  // 用户可见唯一递增票号(SP+入账年月+8位序号,按月重置;支付入账时生成)
  @Column(name = "ticket_no", nullable = false, unique = true, length = 32)
  var ticketNo: String = "",
  @Column(name = "status", nullable = false, length = 30)
  @Enumerated(EnumType.STRING)
  var status: UserTicketStatus = UserTicketStatus.UNUSED,
  // 过期时间(快照计算:固定区间=validTo;购票后N天=购票时间+validDays)
  @Column(name = "expire_time", nullable = false)
  var expireTime: LocalDateTime = LocalDateTime.now(),
  @Column(name = "is_gift", nullable = false)
  var isGift: Boolean = false,
  @Column(name = "used_order_id")
  var usedOrderId: Long? = null,
  @Column(name = "used_at")
  var usedAt: LocalDateTime? = null,
  @Column(name = "returned_at")
  var returnedAt: LocalDateTime? = null,
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
