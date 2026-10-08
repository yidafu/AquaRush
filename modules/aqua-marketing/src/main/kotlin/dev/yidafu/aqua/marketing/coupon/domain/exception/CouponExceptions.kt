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

package dev.yidafu.aqua.marketing.coupon.domain.exception

/**
 * 优惠券异常(PRD 10-marketing.md §F-MKT-003/R-MKT-03)
 */
class CouponTemplateNotFoundException(
  templateId: Long,
) : RuntimeException("优惠券模板不存在: $templateId")

/**
 * 券不可领取(模板停用/非自动领取/已过期)
 */
class CouponNotClaimableException(
  message: String,
) : RuntimeException(message)

/**
 * 超过限领数
 */
class CouponLimitExceededException(
  message: String,
) : RuntimeException(message)

/**
 * 券不可用(不存在/已核销/已过期/门槛不足/范围不匹配)
 */
class CouponNotUsableException(
  message: String,
) : RuntimeException(message)
