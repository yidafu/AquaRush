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

package dev.yidafu.aqua.api.dto

import dev.yidafu.aqua.common.domain.model.enums.CouponScope
import dev.yidafu.aqua.common.domain.model.enums.CouponType
import dev.yidafu.aqua.common.domain.model.enums.UserCouponStatus
import java.time.LocalDateTime

/**
 * 用户优惠券展示信息(用户端我的券包/领券中心)。
 */
data class UserCouponInfo(
  val id: Long,
  val name: String,
  val couponType: CouponType,
  val thresholdCents: Long?,
  val discountAmountCents: Long?,
  val discountRatePermille: Long?,
  val scope: CouponScope,
  val productId: Long?,
  val status: UserCouponStatus,
  val expireTime: LocalDateTime,
)
