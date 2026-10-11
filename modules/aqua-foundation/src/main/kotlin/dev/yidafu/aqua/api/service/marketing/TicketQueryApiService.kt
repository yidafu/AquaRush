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

import dev.yidafu.aqua.common.domain.model.TicketPurchaseModel
import dev.yidafu.aqua.common.domain.model.TicketTypeModel
import dev.yidafu.aqua.common.domain.model.UserTicketModel
import dev.yidafu.aqua.common.domain.model.enums.UserTicketStatus

/**
 * 桶水票用户侧查询服务(用户端水票商城/我的水票;促销引擎核销预检用)
 */
interface TicketQueryApiService {
  /**
   * 水票商城票种列表(仅 ACTIVE)
   */
  fun findActiveTicketTypes(): List<TicketTypeModel>

  /**
   * 我的水票(按状态过滤,空=全部)
   */
  fun findUserTickets(
    userId: Long,
    status: UserTicketStatus?,
  ): List<UserTicketModel>

  /**
   * 可用水票候选池(UNUSED/RETURNED 且未过期;引擎按先过期先用 FIFO 核销)
   */
  fun findUsableTickets(userId: Long): List<UserTicketModel>

  /**
   * 复购激活待激活判定(G97,R-MKT-08 运营策略):给定票中因复购条件未满足而不可核销的票 id 集合。
   * 口径:票种启用复购激活且购票单落了基线时,当前已完成订单数 − 基线 < activation_order_count → 待激活;
   * 无待激活候选时不做订单数查询(短路)。
   */
  fun findPendingActivationIds(
    userId: Long,
    tickets: List<UserTicketModel>,
  ): Set<Long>

  /**
   * 该商品上下文下可用张数(指定商品票按 productId,通兑票按 specification 字符串匹配)
   */
  fun countUsableFor(
    userId: Long,
    productId: Long,
    specification: String?,
  ): Int

  /**
   * 用户已购某票种张数(限购判定,按已支付购票单)
   */
  fun countPurchasedByTicketType(
    userId: Long,
    ticketTypeId: Long,
  ): Int

  fun findPurchaseById(id: Long): TicketPurchaseModel?

  /**
   * 按购票单号查询(微信支付回调 out_trade_no 派发)
   */
  fun findPurchaseByOrderNo(orderNo: String): TicketPurchaseModel?
}
