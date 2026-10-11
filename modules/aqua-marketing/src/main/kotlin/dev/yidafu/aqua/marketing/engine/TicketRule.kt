package dev.yidafu.aqua.marketing.engine

import dev.yidafu.aqua.api.service.marketing.TicketQueryApiService
import dev.yidafu.aqua.common.domain.model.UserTicketModel
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
 *
 * G97 运营策略门禁(R-MKT-08 修订,优先级 待激活 > 单次上限 > 普通不足截断):
 * ① 复购激活:待激活票不入池;显式请求张数超出已激活池且存在待激活票 → 拒;
 * ② 单次核销上限:按「单笔订单 × 票种」截断(N=票种 max_redeem_per_order,NULL/0=不限,FIFO 组内先过期先用);
 *    请求张数超出截断后上限且上限确为瓶颈 → 拒;普通存量不足保持既有静默截断语义。
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
    val matched =
      ticketQueryApiService
        .findUsableTickets(ctx.userId)
        .filter { isTicketUsableFor(it.ticketTypeId, ctx) }
        .sortedBy { it.expireTime } // 先过期先用

    // ① 复购激活门禁:待激活票排除出候选池
    val pendingIds = ticketQueryApiService.findPendingActivationIds(ctx.userId, matched)
    val activated = matched.filter { it.id !in pendingIds }
    val requested = minOf(ctx.ticketRedeemCount, ctx.totalQuantity)

    if (requested > activated.size && pendingIds.isNotEmpty()) {
      throw BadRequestException("水票待激活:需完成复购后方可核销")
    }

    // ② 单次核销上限:按票种截断(票种 maxRedeemPerOrder,NULL/0=不限)
    val cappedPool = applyPerTypeCaps(activated)
    if (requested > cappedPool.size && activated.size > cappedPool.size) {
      throw BadRequestException("超出单次核销上限:单笔订单最多可核销 ${cappedPool.size} 张水票")
    }

    state.availableTicketCount = cappedPool.size
    val redeemCount = minOf(requested, cappedPool.size)

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
      val candidates = cappedPool.take(redeemCount)
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

  /**
   * 单次核销上限(G97):同一订单每票种最多 maxRedeemPerOrder 张;组内保持先过期先用,组合后重排 FIFO。
   */
  private fun applyPerTypeCaps(tickets: List<UserTicketModel>): List<UserTicketModel> {
    if (tickets.isEmpty()) return tickets
    val capCache = mutableMapOf<Long, Int>()
    return tickets
      .groupBy { it.ticketTypeId }
      .flatMap { (ticketTypeId, group) ->
        val cap =
          capCache.getOrPut(ticketTypeId) {
            ticketTypeRepository
              .findById(ticketTypeId)
              .orElse(null)
              ?.maxRedeemPerOrder
              ?.takeIf { it > 0 } ?: 0
          }
        if (cap > 0) group.take(cap) else group
      }.sortedBy { it.expireTime }
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
