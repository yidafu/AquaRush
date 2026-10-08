package dev.yidafu.aqua.marketing.engine

import dev.yidafu.aqua.api.service.marketing.TicketQueryApiService
import dev.yidafu.aqua.common.domain.model.enums.TicketTypeKind
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.marketing.ticket.domain.repository.TicketTypeRepository
import dev.yidafu.aqua.marketing.ticket.domain.repository.UserTicketRepository
import org.springframework.stereotype.Component

/**
 * 桶水票核销规则(R-MKT-08,多商品订单 F-ORD-004):
 * 一票抵一桶,被核销商品数量按单价 0 计入应付,不参与现金计价(R-MKT-02/07);
 * 候选池 UNUSED/RETURNED 未过期,按类型过滤(指定商品票 productId 匹配任一商品/通兑票 specification 匹配任一商品),
 * 先过期先用(FIFO);核销张数 ≤ 订单总桶数 ∩ 可用张数,按 items 顺序逐商品覆盖;
 * 下单消费时逐张原子核销,实际核销数 < 预检 → 回滚。
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
    val requested = minOf(ctx.ticketRedeemCount, ctx.totalQuantity)
    val redeemCount = minOf(requested, pool.size)

    // 按 items 顺序逐商品覆盖(记录每商品被覆盖桶数)
    var remaining = redeemCount
    val updatedItems =
      state.items.map { item ->
        val covered = minOf(item.quantity, remaining)
        remaining -= covered
        item.copy(
          ticketCoveredQuantity = covered,
          cashSubtotalCents = item.unitPriceCents * (item.quantity - covered),
        )
      }
    state.items = updatedItems.toMutableList()
    state.ticketRedeemCount = redeemCount
    state.ticketCoveredQuantity = redeemCount
    state.cashSubtotalCents = updatedItems.sumOf { it.cashSubtotalCents }

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
      TicketTypeKind.SPECIFIC_PRODUCT -> type.productId in ctx.productIds
      TicketTypeKind.UNIVERSAL ->
        type.specification != null && ctx.items.any { type.specification == it.product.specification }
    }
  }
}
