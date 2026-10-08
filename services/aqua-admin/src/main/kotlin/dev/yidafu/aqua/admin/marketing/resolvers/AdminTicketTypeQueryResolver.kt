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

import dev.yidafu.aqua.api.service.marketing.TicketTypeQueryService
import dev.yidafu.aqua.common.graphql.generated.TicketType
import dev.yidafu.aqua.common.graphql.generated.TicketTypeListInput
import dev.yidafu.aqua.common.graphql.generated.TicketTypeVoPage
import dev.yidafu.aqua.marketing.ticket.mapper.TicketTypeMapper
import dev.yidafu.aqua.marketing.ticket.mapper.TicketTypeStatusInputMapper
import org.springframework.data.domain.PageRequest
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

/**
 * 桶水票票种查询 Resolver(管理后台,PERMISSION_MKT_READ)
 * 与 PRD 10-marketing.md §F-MKT-007 保持一致。
 */
@Controller
class AdminTicketTypeQueryResolver(
  private val ticketTypeQueryService: TicketTypeQueryService,
) {
  /**
   * 分页查询票种列表
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_MKT_READ')")
  fun ticketTypes(
    @Argument input: TicketTypeListInput?,
  ): TicketTypeVoPage {
    val status =
      input?.status?.let { TicketTypeStatusInputMapper.map(it) }
    val page =
      ticketTypeQueryService.listTicketTypes(
        keyword = input?.keyword,
        status = status,
        pageable = PageRequest.of(input?.page ?: 0, input?.size ?: 20),
      )
    return TicketTypeVoPage(
      content = page.content.map { TicketTypeMapper.map(it) },
      number = page.number,
      size = page.size,
      totalElements = page.totalElements.toInt(),
      totalPages = page.totalPages,
    )
  }

  /**
   * 根据 ID 查询票种
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_MKT_READ')")
  fun ticketType(
    @Argument id: Long,
  ): TicketType? = ticketTypeQueryService.getTicketType(id)?.let { TicketTypeMapper.map(it) }
}
