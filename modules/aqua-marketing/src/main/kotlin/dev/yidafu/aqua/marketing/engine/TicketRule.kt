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

package dev.yidafu.aqua.marketing.engine

import dev.yidafu.aqua.api.service.marketing.TicketQueryApiService
import dev.yidafu.aqua.common.domain.model.enums.TicketTypeKind
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.marketing.ticket.domain.repository.TicketTypeRepository
import dev.yidafu.aqua.marketing.ticket.domain.repository.UserTicketRepository
import org.springframework.stereotype.Component

/**
 * 桶水票核销规则(R-MKT-08):
 * 一票抵一桶,被核销商品数量按单价 0 计入应付,不参与现金计价(R-MKT-02/07);
 * 候选池 UNUSED/RETURNED 未过期,按类型过滤(指定商品票 productId 匹配/通兑票 specification 匹配),先过期先用(FIFO);
 * 核销张数 ≤ 下单桶数 ∩ 可用张数;下单消费时逐张原子核销,实际核销数 < 预检 → 回滚。
 */
@Component
class TicketRule(
  private val ticketQueryApiService: TicketQueryApiService,
  private val userTicketRepository: UserTicketRepository,
  private val ticketTypeRepository: TicketTypeRepository,
) : PricingRule {
  override val order: Int = 200

  override fun evaluate(
    ctx: PricingContext,
    state: PricingState,
  ) {
    val pool =
      ticketQueryApiService
        .findUsableTickets(ctx.userId)
        .filter { isTicketUsableFor(it.ticketTypeId, ctx) }
        .sortedBy { it.expireTime } // 先过期先用

    state.availableTicketCount = pool.size
    val requested = minOf(ctx.ticketRedeemCount, ctx.quantity)
    val redeemCount = minOf(requested, pool.size)
    state.ticketRedeemCount = redeemCount
    state.ticketCoveredQuantity = redeemCount
    state.cashSubtotalCents = state.unitPriceCents * (ctx.quantity - redeemCount)

    // 下单消费:逐张原子核销,带 orderId 留痕
    val orderId = ctx.orderId
    if (orderId != null && redeemCount > 0) {
      val now = ctx.now
      val candidates = pool.take(redeemCount)
      var actuallyRedeemed = 0
      candidates.forEach { ticket ->
        val affected = userTicketRepository.redeemAtomically(ticket.id!!, ctx.userId, orderId, now)
        if (affected == 1) {
          actuallyRedeemed++
        }
      }
      if (actuallyRedeemed < redeemCount) {
        // 并发被抢:整体回滚,订单不创建
        throw BadRequestException("水票已被占用,请刷新后重试")
      }
    }
  }

  private fun isTicketUsableFor(
    ticketTypeId: Long,
    ctx: PricingContext,
  ): Boolean {
    val type = ticketTypeRepository.findById(ticketTypeId).orElse(null) ?: return false
    return when (type.ticketType) {
      TicketTypeKind.SPECIFIC_PRODUCT -> type.productId == ctx.productId
      TicketTypeKind.UNIVERSAL -> type.specification != null && type.specification == ctx.product.specification
    }
  }
}
