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

import java.time.LocalDateTime

/**
 * 满减/折扣活动档位(PRD 10-marketing.md §R-MKT-04):
 * 档位金额递增且互斥,活动内取最高已达标档;满减用 discountAmountCents,满折用 discountRatePermille(千分比,9000=9折)。
 */
open class PromotionActivityTierModel(
  var id: Long? = null,
  val activityId: Long = 0,
  var thresholdCents: Long = 0,
  var discountAmountCents: Long? = null,
  var discountRatePermille: Long? = null,
  var sort: Int = 0,
  val createdAt: LocalDateTime = LocalDateTime.now(),
  var updatedAt: LocalDateTime = LocalDateTime.now(),
  override var deletedAt: LocalDateTime? = null,
  override var deletedBy: Long? = null,
) : SoftDeletable
