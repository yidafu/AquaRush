/**
 * AquaRush Admin OrderVo Query Resolver
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

import dev.yidafu.aqua.admin.order.mapper.AdminOrderMapper
import dev.yidafu.aqua.api.service.order.OrderQueryService
import dev.yidafu.aqua.common.graphql.generated.GiftItemVo
import dev.yidafu.aqua.common.graphql.generated.OrderListInput
import dev.yidafu.aqua.common.graphql.generated.OrderVo
import dev.yidafu.aqua.common.graphql.generated.OrderVoPage
import dev.yidafu.aqua.common.graphql.util.toPageInfo
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.order.domain.repository.OrderGiftRepository
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

@Controller
class AdminOrderQueryResolver(
  private val orderQueryService: OrderQueryService,
  private val orderGiftRepository: OrderGiftRepository,
  private val adminOrderMapper: AdminOrderMapper,
) {
  /**
   * OrderVo 字段级解析:赠品行(随单快照,配置变更不影响已生成订单 —— R-MKT-09)
   */
  @SchemaMapping(typeName = "OrderVo", field = "giftItems")
  fun giftItems(order: OrderVo): List<GiftItemVo> =
    orderGiftRepository.findByOrderId(order.id).map {
      GiftItemVo(
        productId = it.productId,
        productName = it.productName,
        quantity = it.quantity,
      )
    }

  /**
   * 分页查询订单 - 管理员权限
   * 号码保护(F-ORD-032/R-ORD-17):送水工角色按订单状态脱敏收货人电话,管理员不受限。
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_ORDER_READ')")
  fun orders(
    @Argument input: OrderListInput?,
    @AuthenticationPrincipal userPrincipal: UserPrincipal?,
  ): OrderVoPage {
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
        minAmount = input?.minAmountCents,
        maxAmount = input?.maxAmountCents,
        deliveryWorkerId = input?.deliveryWorkerId,
        page = page,
        size = size,
        sort = sort,
      )

    val (orderList, pageInfo) = ordersPage.toPageInfo { adminOrderMapper.mapForViewer(it, userPrincipal) }
    return OrderVoPage(
      content = orderList,
      totalElements = pageInfo.total,
      totalPages = pageInfo.totalPages,
      size = pageInfo.pageSize,
      number = pageInfo.pageNum,
    )
  }

  /**
   * 根据ID获取订单 - 管理员权限
   * 号码保护(F-ORD-032/R-ORD-17):送水工角色按订单状态脱敏,管理员不受限(AC-ORD-032-04)。
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_ORDER_READ')")
  fun order(
    @Argument orderId: Long,
    @AuthenticationPrincipal userPrincipal: UserPrincipal?,
  ): OrderVo? =
    adminOrderMapper.mapForViewer(
      orderQueryService.getOrderById(orderId),
      userPrincipal,
    )

  /**
   * 根据订单号获取订单(后台订单详情 + 送水工端订单详情共用)
   * 号码保护(F-ORD-032/R-ORD-17):送水工仅配送阶段(待配送/配送中)可见完整号码,
   * 其余状态服务端仅下发脱敏号码、无查看完整号码旁路(AC-ORD-032-02/03/05);管理员不受限。
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_ORDER_READ')")
  fun orderByNo(
    @Argument orderNo: String,
    @AuthenticationPrincipal userPrincipal: UserPrincipal?,
  ): OrderVo? =
    adminOrderMapper.mapForViewer(
      orderQueryService.getOrderByNo(orderNo),
      userPrincipal,
    )
}
