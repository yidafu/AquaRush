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

package dev.yidafu.aqua.api.service.marketing

import dev.yidafu.aqua.api.dto.ClaimCouponRequest
import dev.yidafu.aqua.common.domain.model.UserCouponModel

/**
 * 优惠券用户侧变更服务(用户端领券/取消退券)
 */
interface CouponMutationApiService {
  /**
   * 领取优惠券(仅 ACTIVE+AUTO_CLAIM 模板;限领数校验;参数快照)
   */
  fun claimCoupon(
    userId: Long,
    request: ClaimCouponRequest,
  ): UserCouponModel

  /**
   * 订单取消/退款退回(幂等):已核销券未过期→RETURNED 可复用,过期→EXPIRED
   */
  fun returnCouponsByOrder(orderId: Long): Int
}
