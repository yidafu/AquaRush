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

package dev.yidafu.aqua.marketing.coupon.service.impl

import dev.yidafu.aqua.common.domain.model.CouponTemplateModel
import dev.yidafu.aqua.common.domain.model.UserCouponModel
import dev.yidafu.aqua.common.domain.model.enums.UserCouponStatus
import dev.yidafu.aqua.common.domain.model.enums.ValidityType
import java.time.LocalDateTime

/**
 * 用户优惠券快照构造(R-MKT-03:领取/发放时对模板参数快照,模板变更不影响已领券)。
 */
internal object CouponSnapshotSupport {
  /**
   * 快照过期时间:固定区间=validTo;领取后 N 天=claimTime + validDays。
   */
  fun computeExpireTime(
    template: CouponTemplateModel,
    claimTime: LocalDateTime,
  ): LocalDateTime =
    when (template.validityType) {
      ValidityType.FIXED_RANGE -> template.validTo ?: claimTime
      ValidityType.DAYS_AFTER_CLAIM -> claimTime.plusDays((template.validDays ?: 0).toLong())
      else -> claimTime
    }

  /**
   * 由模板构造用户券实例(参数快照,状态 RECEIVED)。
   */
  fun buildUserCoupon(
    userId: Long,
    template: CouponTemplateModel,
    claimTime: LocalDateTime,
  ): UserCouponModel =
    UserCouponModel(
      userId = userId,
      couponTemplateId = template.id!!,
      status = UserCouponStatus.RECEIVED,
      name = template.name,
      couponType = template.couponType,
      thresholdCents = template.thresholdCents,
      discountAmountCents = template.discountAmountCents,
      discountRatePermille = template.discountRatePermille,
      scope = template.scope,
      productId = template.productId,
      expireTime = computeExpireTime(template, claimTime),
      claimTime = claimTime,
      createdAt = claimTime,
      updatedAt = claimTime,
    )
}
