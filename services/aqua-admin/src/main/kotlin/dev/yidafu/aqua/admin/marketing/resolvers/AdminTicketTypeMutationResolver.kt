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

package dev.yidafu.aqua.admin.marketing.resolvers

import dev.yidafu.aqua.api.service.marketing.TicketTypeMutationService
import dev.yidafu.aqua.common.graphql.generated.CreateTicketTypeInput
import dev.yidafu.aqua.common.graphql.generated.TicketType
import dev.yidafu.aqua.common.graphql.generated.UpdateTicketTypeInput
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.marketing.ticket.mapper.toGraphQL
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

/**
 * 桶水票票种变更 Resolver(管理后台,PERMISSION_MKT_WRITE)
 * 票种/套餐配置变更不影响已购票(快照);停用后用户端不再展示(PRD 10-marketing.md §F-MKT-007)。
 */
@Controller
class AdminTicketTypeMutationResolver(
  private val ticketTypeMutationService: TicketTypeMutationService,
) {
  private val logger = LoggerFactory.getLogger(AdminTicketTypeMutationResolver::class.java)

  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_MKT_WRITE')")
  fun createTicketType(
    @Argument @Valid input: CreateTicketTypeInput,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): TicketType {
    logger.info("Admin {} creating ticket type: {}", principal.username, input.name)
    val ticketType =
      ticketTypeMutationService.createTicketType(
        adminId = principal.id,
        adminName = principal.username,
        input = input,
      )
    return ticketType.toGraphQL()
  }

  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_MKT_WRITE')")
  fun updateTicketType(
    @Argument id: Long,
    @Argument @Valid input: UpdateTicketTypeInput,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): TicketType {
    logger.info("Admin {} updating ticket type {}", principal.username, id)
    val ticketType =
      ticketTypeMutationService.updateTicketType(
        adminId = principal.id,
        adminName = principal.username,
        id = id,
        input = input,
      )
    return ticketType.toGraphQL()
  }

  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_MKT_WRITE')")
  fun disableTicketType(
    @Argument id: Long,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): TicketType {
    logger.info("Admin {} disabling ticket type {}", principal.username, id)
    val ticketType =
      ticketTypeMutationService.disableTicketType(principal.id, principal.username, id)
    return ticketType.toGraphQL()
  }
}
