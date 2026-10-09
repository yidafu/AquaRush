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

import dev.yidafu.aqua.common.domain.model.enums.CouponScope
import dev.yidafu.aqua.common.domain.model.enums.CouponType
import dev.yidafu.aqua.common.domain.model.enums.UserCouponStatus
import java.time.LocalDateTime

/**
 * 用户优惠券实例(PRD 10-marketing.md §F-MKT-003):
 * 领取/发放时对模板参数做快照(名称/类型/门槛/面额/折扣率/范围/有效期),模板变更不影响已领券;
 * 状态机 RECEIVED → USED → RETURNED(订单取消/退款退回,未过期可复用)→ USED ... / EXPIRED;
 * 核销幂等:原子条件 UPDATE(status=RECEIVED 且未过期),防并发重复使用。
 */
open class UserCouponModel(
  var id: Long? = null,
  val userId: Long = 0,
  val couponTemplateId: Long = 0,
  var status: UserCouponStatus = UserCouponStatus.RECEIVED,
  // ===== 模板参数快照 =====
  var name: String = "",
  var couponType: CouponType = CouponType.CASH,
  var thresholdCents: Long? = null,
  var discountAmountCents: Long? = null,
  var discountRatePermille: Long? = null,
  var scope: CouponScope = CouponScope.ALL,
  var productId: Long? = null,
  // 过期时间(快照计算:固定区间=validTo;领取后N天=claimTime+validDays)
  var expireTime: LocalDateTime = LocalDateTime.now(),
  val claimTime: LocalDateTime = LocalDateTime.now(),
  var usedOrderId: Long? = null,
  var usedAt: LocalDateTime? = null,
  var returnedAt: LocalDateTime? = null,
  val createdAt: LocalDateTime = LocalDateTime.now(),
  var updatedAt: LocalDateTime = LocalDateTime.now(),
  override var deletedAt: LocalDateTime? = null,
  override var deletedBy: Long? = null,
) : SoftDeletable
