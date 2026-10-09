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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package dev.yidafu.aqua.client.marketing.resolvers

import dev.yidafu.aqua.api.service.marketing.TicketQueryApiService
import dev.yidafu.aqua.api.service.marketing.TicketTypeQueryService
import dev.yidafu.aqua.client.marketing.mapper.toDomain
import dev.yidafu.aqua.client.marketing.mapper.toGraphQL
import dev.yidafu.aqua.common.annotation.ClientService
import dev.yidafu.aqua.common.graphql.generated.TicketTypeVo
import dev.yidafu.aqua.common.graphql.generated.UserTicketStatus
import dev.yidafu.aqua.common.graphql.generated.UserTicketVo
import dev.yidafu.aqua.common.security.UserPrincipal
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

/**
 * 用户端桶水票查询 Resolver(F-MKT-007:水票商城/我的水票)
 */
@ClientService
@Controller
class ClientTicketQueryResolver(
  private val ticketQueryApiService: TicketQueryApiService,
  private val ticketTypeQueryService: TicketTypeQueryService,
) {
  /**
   * 水票商城(仅上架票种,含当前用户已购张数用于限购展示)
   */
  @QueryMapping
  @PreAuthorize("isAuthenticated()")
  fun ticketShop(
    @AuthenticationPrincipal principal: UserPrincipal,
  ): List<TicketTypeVo> =
    ticketQueryApiService.findActiveTicketTypes().map { type ->
      type.toGraphQL(
        myPurchasedCount = ticketQueryApiService.countPurchasedByTicketType(principal.id, type.id ?: 0L),
      )
    }

  /**
   * 我的水票(按状态过滤,空=全部;票种名称/通兑票限定规格取自票种)
   */
  @QueryMapping
  @PreAuthorize("isAuthenticated()")
  fun myTickets(
    @Argument status: UserTicketStatus?,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): List<UserTicketVo> =
    ticketQueryApiService.findUserTickets(principal.id, status?.toDomain()).map { ticket ->
      val ticketType = ticketTypeQueryService.getTicketType(ticket.ticketTypeId)
      ticket.toGraphQL(
        ticketTypeName = ticketType?.name ?: "水票",
        specification = ticketType?.specification,
      )
    }
}
