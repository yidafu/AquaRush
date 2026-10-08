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

import dev.yidafu.aqua.common.domain.model.CouponTemplateModel
import dev.yidafu.aqua.common.domain.model.UserCouponModel
import dev.yidafu.aqua.common.domain.model.enums.UserCouponStatus

/**
 * 优惠券用户侧查询服务(用户端领券中心/我的券包;促销引擎评估用)
 */
interface CouponQueryApiService {
  /**
   * 领券中心模板列表(仅 ACTIVE 且 AUTO_CLAIM,含当前用户已领数量)
   */
  fun findClaimableTemplates(userId: Long): List<CouponTemplateModel>

  /**
   * 我的券包(按状态过滤,空=全部)
   */
  fun findUserCoupons(
    userId: Long,
    status: UserCouponStatus?,
  ): List<UserCouponModel>

  /**
   * 用户可用券(未过期且状态 RECEIVED/RETURNED,供引擎「最优优惠组合」评估)
   */
  fun findUsableCoupons(userId: Long): List<UserCouponModel>

  fun findUserCouponByIdAndUser(
    userId: Long,
    couponId: Long,
  ): UserCouponModel?

  /**
   * 用户已领某模板数量(限领数判定)
   */
  fun countUserCouponsByTemplate(
    userId: Long,
    templateId: Long,
  ): Long
}
