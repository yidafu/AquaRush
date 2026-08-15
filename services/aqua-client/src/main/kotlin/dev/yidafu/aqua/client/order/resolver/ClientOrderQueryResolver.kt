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

package dev.yidafu.aqua.client.order.resolver

import dev.yidafu.aqua.api.service.order.OrderQueryApiService
import dev.yidafu.aqua.client.order.mapper.toGraphQL
import dev.yidafu.aqua.common.annotation.ClientService
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.exception.NotFoundException
import dev.yidafu.aqua.common.graphql.generated.Order
import dev.yidafu.aqua.common.security.UserPrincipal
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

/**
 * 用户端订单查询 Resolver(我的订单详情,仅本人可见):
 * 订单详情/支付结果回显使用真实订单数据(F-ORD-003)。
 */
@ClientService
@Controller
class ClientOrderQueryResolver(
  private val orderQueryApiService: OrderQueryApiService,
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
    return order.toGraphQL()
  }
}
