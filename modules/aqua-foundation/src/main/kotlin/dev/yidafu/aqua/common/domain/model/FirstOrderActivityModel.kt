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

import dev.yidafu.aqua.common.domain.model.enums.FirstOrderActivityType
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus
import dev.yidafu.aqua.common.domain.model.enums.PromotionScope
import java.time.LocalDateTime

/**
 * 首单立减/新人专享价活动(PRD 10-marketing.md §F-MKT-009/R-MKT-10):
 * 新用户首次下单自动享受的专属优惠,一单一享(新用户=无已完成订单,首单完成即失效);
 * FIRST_ORDER_REDUCE 进 R-MKT-07 叠加①(与满减互斥取最大),NEWCOMER_PRICE 进 R-MKT-02 单价解析④。
 */
open class FirstOrderActivityModel(
  var id: Long? = null,
  var name: String = "",
  var activityType: FirstOrderActivityType = FirstOrderActivityType.FIRST_ORDER_REDUCE,
  // 首单立减:满 X
  var thresholdCents: Long? = null,
  // 首单立减:减 Y(与折扣率二选一)
  var discountAmountCents: Long? = null,
  // 首单立减:打 N 折(千分比,9000=9折,与减金额二选一)
  var discountRatePermille: Long? = null,
  var scope: PromotionScope = PromotionScope.ALL,
  // 新人专享价:指定商品
  var productId: Long? = null,
  // 新人专享价:专享单价(分)
  var newcomerPriceCents: Long? = null,
  var validFrom: LocalDateTime = LocalDateTime.now(),
  var validTo: LocalDateTime = LocalDateTime.now(),
  var status: PromotionActivityStatus = PromotionActivityStatus.ACTIVE,
  val createdAt: LocalDateTime = LocalDateTime.now(),
  var updatedAt: LocalDateTime = LocalDateTime.now(),
  override var deletedAt: LocalDateTime? = null,
  override var deletedBy: Long? = null,
) : SoftDeletable
