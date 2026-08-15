package dev.yidafu.aqua.marketing.engine

import dev.yidafu.aqua.common.domain.model.ProductModel
import java.time.LocalDateTime

/**
 * 促销引擎计价上下文(PRD 10-marketing.md §R-MKT-02,多商品订单 F-ORD-004):
 * items 为订单商品明细(1~20 项,同一商品不可重复);
 * orderId 为空=纯预览(无副作用);非空=下单消费(同事务原子核销,失败整体回滚)。
 */
data class PricingContext(
  val userId: Long,
  val items: List<PricingItemContext>,
  val isSelfCollect: Boolean,
  /** null=引擎自动选最优券,0=不用券 */
  val preferredCouponId: Long?,
  val ticketRedeemCount: Int,
  val now: LocalDateTime,
  val orderId: Long? = null,
) {
  val productIds: List<Long>
    get() = items.map { it.productId }

  /** 订单商品总数量(水票核销上限口径 R-MKT-08) */
  val totalQuantity: Int
    get() = items.sumOf { it.quantity }
}

/**
 * 计价商品明细上下文(单价解析/水票覆盖按商品逐项进行)
 */
data class PricingItemContext(
  val productId: Long,
  val quantity: Int,
  val product: ProductModel,
)
