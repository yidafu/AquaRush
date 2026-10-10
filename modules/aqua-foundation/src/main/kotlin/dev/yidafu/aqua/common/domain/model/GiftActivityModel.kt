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

import dev.yidafu.aqua.common.domain.model.enums.GiftActivityType
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus
import dev.yidafu.aqua.common.domain.model.enums.PromotionScope
import java.time.LocalDateTime

/**
 * 买赠/满赠活动(PRD 10-marketing.md §F-MKT-008/R-MKT-09):
 * 订单达标自动赠送指定商品(赠品),赠品为附加 0 价行随单配送,不参与现金计价与叠加;
 * 满赠按现金部分商品小计判定,买赠按指定商品现金购买数量判定;v1 单档单赠品。
 */
open class GiftActivityModel(
  var id: Long? = null,
  var name: String = "",
  var giftType: GiftActivityType = GiftActivityType.FULL_GIFT,
  var thresholdCents: Long? = null,
  var thresholdQuantity: Int? = null,
  var scope: PromotionScope = PromotionScope.ALL,
  var productId: Long? = null,
  var customerGroupId: Long? = null,
  var giftProductId: Long = 0,
  var giftQuantity: Int = 0,
  var validFrom: LocalDateTime = LocalDateTime.now(),
  var validTo: LocalDateTime = LocalDateTime.now(),
  var status: PromotionActivityStatus = PromotionActivityStatus.ACTIVE,
  val createdAt: LocalDateTime = LocalDateTime.now(),
  var updatedAt: LocalDateTime = LocalDateTime.now(),
  override var deletedAt: LocalDateTime? = null,
  override var deletedBy: Long? = null,
) : SoftDeletable
