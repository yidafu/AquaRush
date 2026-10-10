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

import dev.yidafu.aqua.common.domain.model.enums.CouponIssuanceType
import dev.yidafu.aqua.common.domain.model.enums.CouponScope
import dev.yidafu.aqua.common.domain.model.enums.CouponTemplateStatus
import dev.yidafu.aqua.common.domain.model.enums.CouponType
import dev.yidafu.aqua.common.domain.model.enums.ValidityType
import java.time.LocalDateTime

/**
 * 优惠券模板(PRD 10-marketing.md §F-MKT-003/R-MKT-03):
 * 现金券(满X减Y)/折扣券(满X打N折)/无门槛现金券;门槛按商品小计判定;有效期固定区间或领取后N天;
 * 发放方式自动领取/定向发放;限领数;停用后不可再领取(已领券按快照继续有效)。
 */
open class CouponTemplateModel(
  var id: Long? = null,
  var name: String = "",
  var couponType: CouponType = CouponType.CASH,
  // 门槛(分,空=无门槛)
  var thresholdCents: Long? = null,
  // 现金券面额(分)
  var discountAmountCents: Long? = null,
  // 折扣券折扣率(千分比,8500=8.5折即支付85%)
  var discountRatePermille: Long? = null,
  var scope: CouponScope = CouponScope.ALL,
  var productId: Long? = null,
  var validityType: ValidityType = ValidityType.FIXED_RANGE,
  var validFrom: LocalDateTime? = null,
  var validTo: LocalDateTime? = null,
  var validDays: Int? = null,
  var issuanceType: CouponIssuanceType = CouponIssuanceType.AUTO_CLAIM,
  var perUserLimit: Int = 1,
  var status: CouponTemplateStatus = CouponTemplateStatus.ACTIVE,
  val createdAt: LocalDateTime = LocalDateTime.now(),
  var updatedAt: LocalDateTime = LocalDateTime.now(),
  override var deletedAt: LocalDateTime? = null,
  override var deletedBy: Long? = null,
) : SoftDeletable
