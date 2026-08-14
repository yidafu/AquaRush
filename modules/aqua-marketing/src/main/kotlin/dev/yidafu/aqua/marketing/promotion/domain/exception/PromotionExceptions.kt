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

package dev.yidafu.aqua.marketing.promotion.domain.exception

/**
 * 满减/折扣活动异常(PRD 10-marketing.md §F-MKT-004)
 */
class PromotionActivityNotFoundException(
  activityId: Long,
) : RuntimeException("营销活动不存在: $activityId")

/**
 * 档位校验异常(档位金额递增且互斥 R-MKT-04)
 */
class InvalidTierException(
  message: String,
) : RuntimeException(message)
