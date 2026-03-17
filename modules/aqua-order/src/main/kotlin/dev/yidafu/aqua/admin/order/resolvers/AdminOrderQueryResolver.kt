/*
 * AquaRush Admin Order Query Resolver
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

package dev.yidafu.aqua.admin.order.resolvers

import dev.yidafu.aqua.api.service.order.OrderQueryService
import dev.yidafu.aqua.common.graphql.generated.Order
import dev.yidafu.aqua.common.graphql.generated.OrderListInput
import dev.yidafu.aqua.common.graphql.generated.OrderPage
import dev.yidafu.aqua.common.graphql.util.toPageInfo
import dev.yidafu.aqua.order.mapper.OrderMapper
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
class AdminOrderQueryResolver(
  private val orderQueryService: OrderQueryService,
) {
  /**
   * 分页查询订单 - 管理员权限
   */
  @QueryMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun orders(
    @Argument input: OrderListInput?,
  ): OrderPage {
    val page = input?.page ?: 0
    val size = input?.size ?: 20
    val sort = input?.sort ?: "createdAt,desc"

    val ordersPage =
      orderQueryService.searchOrders(
        keyword = input?.search,
        status = input?.status?.name,
        userId = input?.userId,
        dateFrom = input?.dateFrom?.toString(),
        dateTo = input?.dateTo?.toString(),
        minAmount = input?.minAmount,
        maxAmount = input?.maxAmount,
        deliveryWorkerId = input?.deliveryWorkerId,
        page = page,
        size = size,
        sort = sort,
      )

    val (orderList, pageInfo) = ordersPage.toPageInfo { OrderMapper.map(it) }
    return OrderPage(
      content = orderList,
      totalElements = pageInfo.total,
      totalPages = pageInfo.totalPages,
      size = pageInfo.pageSize,
      number = pageInfo.pageNum,
    )
  }

  /**
   * 根据ID获取订单 - 管理员权限
   */
  @QueryMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun order(
    @Argument orderId: Long,
  ): Order? = OrderMapper.map(orderQueryService.getOrderById(orderId))

  /**
   * 根据订单号获取订单 - 管理员权限
   */
  @QueryMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun orderByNumber(
    @Argument orderNumber: String,
  ): Order? = OrderMapper.map(orderQueryService.getOrderByNumber(orderNumber))
}
