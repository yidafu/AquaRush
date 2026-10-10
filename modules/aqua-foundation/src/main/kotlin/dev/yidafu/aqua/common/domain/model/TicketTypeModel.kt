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

import dev.yidafu.aqua.common.domain.model.enums.TicketTypeKind
import dev.yidafu.aqua.common.domain.model.enums.TicketTypeStatus
import dev.yidafu.aqua.common.domain.model.enums.ValidityType
import java.time.LocalDateTime

/**
 * 桶水票票种(PRD 10-marketing.md §F-MKT-007/R-MKT-08):
 * 指定商品票(仅抵指定商品)/通兑票(抵限定规格下全场商品,跨规格不通用);
 * 套餐:张数+购票价(分)+赠送张数(买N送M);有效期固定区间或购票后N天;限购(0=不限)。
 */
open class TicketTypeModel(
  var id: Long? = null,
  var name: String = "",
  var ticketType: TicketTypeKind = TicketTypeKind.UNIVERSAL,
  var productId: Long? = null,
  // 通兑票限定规格(与商品 specification 字符串匹配,如 "19L")
  var specification: String? = null,
  var packageCount: Int = 0,
  var packagePriceCents: Long = 0,
  var giftCount: Int = 0,
  var validityType: ValidityType = ValidityType.FIXED_RANGE,
  var validFrom: LocalDateTime? = null,
  var validTo: LocalDateTime? = null,
  var validDays: Int? = null,
  var perUserLimit: Int = 0,
  var status: TicketTypeStatus = TicketTypeStatus.ACTIVE,
  val createdAt: LocalDateTime = LocalDateTime.now(),
  var updatedAt: LocalDateTime = LocalDateTime.now(),
  override var deletedAt: LocalDateTime? = null,
  override var deletedBy: Long? = null,
) : SoftDeletable
