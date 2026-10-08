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

package dev.yidafu.aqua.marketing.coupon.domain.repository

import dev.yidafu.aqua.common.domain.model.enums.UserCouponStatus
import java.time.LocalDateTime

/**
 * 用户优惠券实例仓库 QueryDSL 自定义查询(UserCouponRepository 中手写 @Query 迁移至此)。
 */
interface UserCouponRepositoryCustom {
  /**
   * 原子核销:仅 RECEIVED 且未过期可核销;受影响行数=1 成功,=0 表示已被并发核销/过期(核销幂等)。
   */
  fun redeemAtomically(
    id: Long,
    userId: Long,
    orderId: Long,
    from: UserCouponStatus,
    to: UserCouponStatus,
    now: LocalDateTime,
  ): Int

  /**
   * 订单取消/退款退回(幂等):该订单已核销券,未过期→RETURNED 可复用,过期→EXPIRED。
   */
  fun returnByOrder(
    orderId: Long,
    now: LocalDateTime,
  ): Int
}
