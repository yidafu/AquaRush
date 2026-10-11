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

import dev.yidafu.aqua.common.domain.model.enums.TicketPurchaseStatus
import java.time.LocalDateTime

/**
 * 购票单(PRD 10-marketing.md §F-MKT-007/R-MKT-08):
 * 购票即预收款项,一经购买不支持退款(不退现、不支持退票退款);
 * orderNo 作为微信支付 out_trade_no;支付回调成功后按 PENDING→PAID 原子流转并票包入账(totalCount = N+M),幂等。
 */
open class TicketPurchaseModel(
  var id: Long? = null,
  val userId: Long = 0,
  val ticketTypeId: Long = 0,
  var orderNo: String = "",
  var packageCount: Int = 0,
  var giftCount: Int = 0,
  var totalCount: Int = 0,
  var packagePriceCents: Long = 0,
  var paymentStatus: TicketPurchaseStatus = TicketPurchaseStatus.PENDING,
  var transactionId: String? = null,
  var paidAt: LocalDateTime? = null,
  // G97 复购激活基线:购票时刻用户已完成订单数;仅票种启用复购激活时落值,NULL=该购票单不参与激活判定
  var completedOrdersAtPurchase: Int? = null,
  val createdAt: LocalDateTime = LocalDateTime.now(),
  var updatedAt: LocalDateTime = LocalDateTime.now(),
  override var deletedAt: LocalDateTime? = null,
  override var deletedBy: Long? = null,
) : SoftDeletable
