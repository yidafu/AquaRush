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

import dev.yidafu.aqua.api.dto.PurchaseTicketsRequest
import dev.yidafu.aqua.common.domain.model.TicketPurchaseModel

/**
 * 桶水票用户侧变更服务(购票/支付入账/取消退票,PRD 10-marketing.md §F-MKT-007)
 */
interface TicketMutationApiService {
  /**
   * 购票(校验票种 ACTIVE+限购;创建 PENDING 购票单,orderNo 作为支付 out_trade_no;
   * 购票即预收款项,一经购买不支持退款)
   */
  fun purchaseTickets(
    userId: Long,
    request: PurchaseTicketsRequest,
  ): TicketPurchaseModel

  /**
   * 支付成功入账(幂等):PENDING→PAID 原子流转后票包入账 N+M 张(赠送票 is_gift 同池);
   * 重复回调 no-op。
   */
  fun handleTicketPurchasePaid(
    ticketPurchaseId: Long,
    transactionId: String,
  ): TicketPurchaseModel

  /**
   * 订单取消/退款退回(幂等):已核销未过期票→RETURNED 可复用,过期→EXPIRED
   */
  fun returnTicketsByOrder(orderId: Long): Int
}
