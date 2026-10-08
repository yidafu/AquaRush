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
import dev.yidafu.aqua.common.domain.model.enums.CouponIssuanceType
import dev.yidafu.aqua.common.domain.model.enums.CouponScope
import dev.yidafu.aqua.common.domain.model.enums.CouponTemplateStatus
import dev.yidafu.aqua.common.domain.model.enums.CouponType
import dev.yidafu.aqua.common.domain.model.enums.ValidityType
import jakarta.persistence.*
import org.hibernate.annotations.SoftDelete
import java.time.LocalDateTime

/**
 * 优惠券模板(PRD 10-marketing.md §F-MKT-003/R-MKT-03):
 * 现金券(满X减Y)/折扣券(满X打N折)/无门槛现金券;门槛按商品小计判定;有效期固定区间或领取后N天;
 * 发放方式自动领取/定向发放;限领数;停用后不可再领取(已领券按快照继续有效)。
 */
@Entity
@SoftDelete(columnName = "is_deleted")
@Table(name = "coupon_templates")
open class CouponTemplateModel(
  @Id
  @SnowflakeIdGenerator
  @Column(name = "id", nullable = false, updatable = false)
  var id: Long? = null,
  @Column(name = "name", nullable = false, length = 100)
  var name: String = "",
  @Column(name = "coupon_type", nullable = false, length = 30)
  @Enumerated(EnumType.STRING)
  var couponType: CouponType = CouponType.CASH,
  // 门槛(分,空=无门槛)
  @Column(name = "threshold_cents")
  var thresholdCents: Long? = null,
  // 现金券面额(分)
  @Column(name = "discount_amount_cents")
  var discountAmountCents: Long? = null,
  // 折扣券折扣率(千分比,8500=8.5折即支付85%)
  @Column(name = "discount_rate_permille")
  var discountRatePermille: Long? = null,
  @Column(name = "scope", nullable = false, length = 30)
  @Enumerated(EnumType.STRING)
  var scope: CouponScope = CouponScope.ALL,
  @Column(name = "product_id")
  var productId: Long? = null,
  @Column(name = "validity_type", nullable = false, length = 30)
  @Enumerated(EnumType.STRING)
  var validityType: ValidityType = ValidityType.FIXED_RANGE,
  @Column(name = "valid_from")
  var validFrom: LocalDateTime? = null,
  @Column(name = "valid_to")
  var validTo: LocalDateTime? = null,
  @Column(name = "valid_days")
  var validDays: Int? = null,
  @Column(name = "issuance_type", nullable = false, length = 30)
  @Enumerated(EnumType.STRING)
  var issuanceType: CouponIssuanceType = CouponIssuanceType.AUTO_CLAIM,
  @Column(name = "per_user_limit", nullable = false)
  var perUserLimit: Int = 1,
  @Column(name = "status", nullable = false, length = 30)
  @Enumerated(EnumType.STRING)
  var status: CouponTemplateStatus = CouponTemplateStatus.ACTIVE,
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
