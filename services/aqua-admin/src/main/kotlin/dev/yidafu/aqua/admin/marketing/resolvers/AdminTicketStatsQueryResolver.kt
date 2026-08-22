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

import dev.yidafu.aqua.api.dto.TicketSalesPointDTO
import dev.yidafu.aqua.api.dto.TicketStatsSummaryDTO
import dev.yidafu.aqua.api.dto.TicketTypeStatsDTO
import dev.yidafu.aqua.api.dto.TicketUsagePointDTO
import dev.yidafu.aqua.api.service.marketing.TicketStatsQueryService
import dev.yidafu.aqua.common.graphql.generated.DateRangeInput
import dev.yidafu.aqua.common.graphql.generated.TicketSalesPointVo
import dev.yidafu.aqua.common.graphql.generated.TicketStatsSummaryVo
import dev.yidafu.aqua.common.graphql.generated.TicketTypeStatsVo
import dev.yidafu.aqua.common.graphql.generated.TicketUsagePointVo
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

/**
 * 桶水票票种销售/使用统计 Resolver(管理后台,PERMISSION_MKT_READ):
 * 供桶水票票种管理页「详情」图表展示(F-MKT-007):区间汇总 + 销售/使用按日序列。
 */
@Controller
class AdminTicketStatsQueryResolver(
  private val ticketStatsQueryService: TicketStatsQueryService,
) {
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_MKT_READ')")
  fun ticketTypeStats(
    @Argument ticketTypeId: Long,
    @Argument input: DateRangeInput,
  ): TicketTypeStatsVo {
    val stats =
      ticketStatsQueryService.getTicketTypeStats(
        ticketTypeId = ticketTypeId,
        startDate = input.startDate,
        endDate = input.endDate,
      )
    return stats.toGraphQL()
  }

  private fun TicketTypeStatsDTO.toGraphQL(): TicketTypeStatsVo =
    TicketTypeStatsVo(
      ticketTypeId = ticketTypeId,
      ticketTypeName = ticketTypeName,
      summary = summary.toGraphQL(),
      sales = sales.map { it.toGraphQL() },
      usage = usage.map { it.toGraphQL() },
    )

  private fun TicketStatsSummaryDTO.toGraphQL(): TicketStatsSummaryVo =
    TicketStatsSummaryVo(
      soldCount = soldCount,
      revenueCents = revenueCents,
      redeemedCount = redeemedCount,
      returnedCount = returnedCount,
      expiredCount = expiredCount,
      availableCount = availableCount,
    )

  private fun TicketSalesPointDTO.toGraphQL(): TicketSalesPointVo =
    TicketSalesPointVo(
      date = date.toString(),
      purchaseCount = purchaseCount,
      ticketCount = ticketCount,
      revenueCents = revenueCents,
    )

  private fun TicketUsagePointDTO.toGraphQL(): TicketUsagePointVo =
    TicketUsagePointVo(
      date = date.toString(),
      redeemedCount = redeemedCount,
      returnedCount = returnedCount,
      expiredCount = expiredCount,
    )
}
