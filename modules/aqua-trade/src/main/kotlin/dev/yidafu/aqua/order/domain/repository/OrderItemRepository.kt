package dev.yidafu.aqua.order.domain.repository

import dev.yidafu.aqua.common.domain.model.OrderItemModel
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * 订单商品明细仓库(F-ORD-004 多商品下单):
 * order_items 为订单商品权威明细(orders 表仅存聚合金额/营销快照);
 * 查询按软删过滤(与既有仓库一致,见 OrderRepository 等)。
 */
@Repository
interface OrderItemRepository : JpaRepository<OrderItemModel, Long> {
  fun findByOrderId(orderId: Long): List<OrderItemModel>

  fun findByProductId(productId: Long): List<OrderItemModel>
}
