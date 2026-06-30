/**
 * AquaRush Client Order Operation Query Resolver
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

import dev.yidafu.aqua.api.service.order.OrderOperationService
import dev.yidafu.aqua.common.annotation.ClientService
import dev.yidafu.aqua.common.domain.model.OrderOperationModel
import dev.yidafu.aqua.common.security.UserPrincipal
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

@ClientService
@Controller("clientOrderOperationQueryResolver")
class OrderOperationQueryResolver(
  private val orderOperationService: OrderOperationService,
) {
  /**
   * 获取订单的操作历史 - 客户端
   */
  @QueryMapping
  @PreAuthorize("isAuthenticated()")
  fun orderOperations(
    @Argument orderId: Long,
    @AuthenticationPrincipal userPrincipal: UserPrincipal,
  ): List<OrderOperationModel> = orderOperationService.getOrderOperations(orderId)
}
