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
import java.time.LocalDateTime

/**
 * 限时特价活动(PRD 10-marketing.md §F-MKT-005/R-MKT-05):
 * 商品级限时活动价,处于活动期的商品按活动价参与成交单价解析(R-MKT-02③);
 * 时间窗到期自动失效(查询按时间窗过滤,无需定时任务);已生成订单不受影响。
 */
open class FlashSaleActivityModel(
  var id: Long? = null,
  var name: String = "",
  var productId: Long = 0,
  var salePriceCents: Long = 0,
  var validFrom: LocalDateTime = LocalDateTime.now(),
  var validTo: LocalDateTime = LocalDateTime.now(),
  var status: PromotionActivityStatus = PromotionActivityStatus.ACTIVE,
  val createdAt: LocalDateTime = LocalDateTime.now(),
  var updatedAt: LocalDateTime = LocalDateTime.now(),
  override var deletedAt: LocalDateTime? = null,
  override var deletedBy: Long? = null,
) : SoftDeletable
