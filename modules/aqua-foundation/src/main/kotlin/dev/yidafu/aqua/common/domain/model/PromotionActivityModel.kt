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

import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityType
import dev.yidafu.aqua.common.domain.model.enums.PromotionScope
import java.time.LocalDateTime

/**
 * 满减/折扣活动(PRD 10-marketing.md §F-MKT-004/R-MKT-04):
 * 订单级自动营销活动,满足门槛即自动减免,无需领券;多档阶梯(档位递增且互斥);
 * 适用范围全场/指定商品/指定分组;时间窗到期自动失效;同一订单命中多个活动取优惠最大者不叠加。
 */
open class PromotionActivityModel(
  var id: Long? = null,
  var name: String = "",
  var activityType: PromotionActivityType = PromotionActivityType.FULL_REDUCE,
  var scope: PromotionScope = PromotionScope.ALL,
  var productId: Long? = null,
  var customerGroupId: Long? = null,
  var validFrom: LocalDateTime = LocalDateTime.now(),
  var validTo: LocalDateTime = LocalDateTime.now(),
  var status: PromotionActivityStatus = PromotionActivityStatus.ACTIVE,
  val createdAt: LocalDateTime = LocalDateTime.now(),
  var updatedAt: LocalDateTime = LocalDateTime.now(),
  override var deletedAt: LocalDateTime? = null,
  override var deletedBy: Long? = null,
) : SoftDeletable
