package dev.yidafu.aqua.admin.order.mapper

import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.graphql.generated.OrderVo
import dev.yidafu.aqua.order.domain.repository.OrderItemRepository
import dev.yidafu.aqua.order.mapper.toGraphQL
import dev.yidafu.aqua.order.mapper.toOrderItemVo
import org.springframework.stereotype.Component

/**
 * 后台/送水工端订单 GraphQL 映射(F-ORD-004 多商品下单):
 * 在 OrderModel.toGraphQL 之上填充商品明细 items(order_items 权威数据,随单快照映射)。
 */
@Component
class AdminOrderMapper(
  private val orderItemRepository: OrderItemRepository,
) {
  fun map(order: OrderModel): OrderVo =
    order.toGraphQL(
      items = orderItemRepository.findByOrderId(order.id!!).map { it.toOrderItemVo() },
    )
}
