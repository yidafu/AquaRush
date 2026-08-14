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
import dev.yidafu.aqua.common.domain.model.enums.CouponScope
import dev.yidafu.aqua.common.domain.model.enums.CouponType
import dev.yidafu.aqua.common.domain.model.enums.UserCouponStatus
import jakarta.persistence.*
import org.hibernate.annotations.SoftDelete
import java.time.LocalDateTime

/**
 * 用户优惠券实例(PRD 10-marketing.md §F-MKT-003):
 * 领取/发放时对模板参数做快照(名称/类型/门槛/面额/折扣率/范围/有效期),模板变更不影响已领券;
 * 状态机 RECEIVED → USED → RETURNED(订单取消/退款退回,未过期可复用)→ USED ... / EXPIRED;
 * 核销幂等:原子条件 UPDATE(status=RECEIVED 且未过期),防并发重复使用。
 */
@Entity
@SoftDelete(columnName = "is_deleted")
@Table(
  name = "user_coupons",
  indexes = [
    Index(name = "idx_user_coupons_user_status", columnList = "user_id,status"),
    Index(name = "idx_user_coupons_used_order", columnList = "used_order_id"),
  ],
)
open class UserCouponModel(
  @Id
  @SnowflakeIdGenerator
  @Column(name = "id", nullable = false, updatable = false)
  var id: Long? = null,
  @Column(name = "user_id", nullable = false)
  val userId: Long = 0,
  @Column(name = "coupon_template_id", nullable = false)
  val couponTemplateId: Long = 0,
  @Column(name = "status", nullable = false, length = 30)
  @Enumerated(EnumType.STRING)
  var status: UserCouponStatus = UserCouponStatus.RECEIVED,
  // ===== 模板参数快照 =====
  @Column(name = "name", nullable = false, length = 100)
  var name: String = "",
  @Column(name = "coupon_type", nullable = false, length = 30)
  @Enumerated(EnumType.STRING)
  var couponType: CouponType = CouponType.CASH,
  @Column(name = "threshold_cents")
  var thresholdCents: Long? = null,
  @Column(name = "discount_amount_cents")
  var discountAmountCents: Long? = null,
  @Column(name = "discount_rate_permille")
  var discountRatePermille: Long? = null,
  @Column(name = "scope", nullable = false, length = 30)
  @Enumerated(EnumType.STRING)
  var scope: CouponScope = CouponScope.ALL,
  @Column(name = "product_id")
  var productId: Long? = null,
  // 过期时间(快照计算:固定区间=validTo;领取后N天=claimTime+validDays)
  @Column(name = "expire_time", nullable = false)
  var expireTime: LocalDateTime = LocalDateTime.now(),
  @Column(name = "claim_time", nullable = false)
  val claimTime: LocalDateTime = LocalDateTime.now(),
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
