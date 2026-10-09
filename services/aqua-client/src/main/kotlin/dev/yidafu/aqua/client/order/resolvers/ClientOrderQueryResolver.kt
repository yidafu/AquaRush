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

package dev.yidafu.aqua.client.order.resolvers

import dev.yidafu.aqua.api.service.order.OrderQueryApiService
import dev.yidafu.aqua.api.service.review.ReviewQueryService
import dev.yidafu.aqua.client.order.mapper.toGraphQL
import dev.yidafu.aqua.common.annotation.ClientService
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.exception.NotFoundException
import dev.yidafu.aqua.common.graphql.generated.MyOrderStatistics
import dev.yidafu.aqua.common.graphql.generated.Order
import dev.yidafu.aqua.common.graphql.generated.OrderReviewCheckResponse
import dev.yidafu.aqua.common.graphql.generated.OrderStatus
import dev.yidafu.aqua.common.graphql.generated.OrderVoPage
import dev.yidafu.aqua.common.security.UserPrincipal
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

/**
 * 用户端订单查询 Resolver(我的订单,仅本人可见):
 * 订单详情/支付结果回显使用真实订单数据(F-ORD-003);
 * 订单列表分页查询(F-ORD-002);订单评价检查(F-ORD-007)。
 */
@ClientService
@Controller
class ClientOrderQueryResolver(
  private val orderQueryApiService: OrderQueryApiService,
  private val reviewQueryService: ReviewQueryService,
) {
  @QueryMapping
  @PreAuthorize("isAuthenticated()")
  fun order(
    @Argument id: Long,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): Order {
    val order =
      orderQueryApiService.findById(id)
        ?: throw NotFoundException("订单不存在: $id")
    if (order.userId != principal.id) {
      throw BadRequestException("无权查看此订单")
    }
    return order.toGraphQL(items = orderQueryApiService.findItemsByOrderId(order.id!!))
  }

  /**
   * 我的订单列表(F-ORD-002,G02 真实化):userId 强制取登录用户(防越权);
   * statuses 为空=全部;「待发货」口径=待派单+待配送;按下单时间倒序分页。
   */
  @QueryMapping
  @PreAuthorize("isAuthenticated()")
  fun myOrders(
    @Argument statuses: List<OrderStatus>?,
    @Argument page: Int,
    @Argument size: Int,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): OrderVoPage {
    val safePage = page.coerceAtLeast(0)
    val safeSize = size.coerceIn(1, 50)
    val result =
      orderQueryApiService.findByUserIdPaginated(
        userId = principal.id,
        statuses = statuses?.map { OrderModelStatus.valueOf(it.name) },
        page = safePage,
        size = safeSize,
      )
    return OrderVoPage(
      content =
        result.content.map { order ->
          order.toGraphQL(items = orderQueryApiService.findItemsByOrderId(order.id!!))
        },
      totalElements = result.totalElements.toInt(),
      totalPages = result.totalPages,
      size = result.size,
      number = result.number,
    )
  }

  /**
   * 我的订单状态计数(F-USR-002,G31 我的页订单角标真实化):
   * userId 强制取登录用户(防越权);按状态分组计数,缺失状态补 0;
   * total = 各状态合计(软删除自动过滤)。
   */
  @QueryMapping
  @PreAuthorize("isAuthenticated()")
  fun myOrderStatistics(
    @AuthenticationPrincipal principal: UserPrincipal,
  ): MyOrderStatistics {
    val counts = orderQueryApiService.countByUserIdGroupByStatus(principal.id)

    fun countOf(status: OrderModelStatus): Int = counts[status]?.toInt() ?: 0
    val pendingPayment = countOf(OrderModelStatus.PENDING_PAYMENT)
    val pendingDispatch = countOf(OrderModelStatus.PENDING_DISPATCH)
    val pendingDelivery = countOf(OrderModelStatus.PENDING_DELIVERY)
    val delivering = countOf(OrderModelStatus.DELIVERING)
    // 待确认收货(R-ORD-11:已送达未确认,我的页角标)
    val awaitingConfirmation = countOf(OrderModelStatus.AWAITING_CONFIRMATION)
    val completed = countOf(OrderModelStatus.COMPLETED)
    val cancelled = countOf(OrderModelStatus.CANCELLED)
    val refunded = countOf(OrderModelStatus.REFUNDED)
    return MyOrderStatistics(
      total = pendingPayment + pendingDispatch + pendingDelivery + delivering + awaitingConfirmation + completed + cancelled + refunded,
      pendingPayment = pendingPayment,
      pendingDispatch = pendingDispatch,
      pendingDelivery = pendingDelivery,
      delivering = delivering,
      awaitingConfirmation = awaitingConfirmation,
      completed = completed,
      cancelled = cancelled,
      refunded = refunded,
    )
  }

  /**
   * 订单评价检查(F-ORD-007):返回已评价内容与是否可评(仅已完成且未评价)。
   */
  @QueryMapping
  @PreAuthorize("isAuthenticated()")
  fun orderReviewCheck(
    @Argument orderId: Long,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): OrderReviewCheckResponse {
    val order =
      orderQueryApiService.findById(orderId)
        ?: throw NotFoundException("订单不存在: $orderId")
    if (order.userId != principal.id) {
      throw BadRequestException("无权查看此订单")
    }
    val review = reviewQueryService.findByOrderId(orderId)
    return OrderReviewCheckResponse(
      hasReviewed = review != null,
      canReview = order.status == OrderModelStatus.COMPLETED && review == null,
      review = review?.toGraphQL(),
    )
  }
}
