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

package dev.yidafu.aqua.marketing.ticket.service.impl

import dev.yidafu.aqua.api.dto.TicketSalesPointDTO
import dev.yidafu.aqua.api.dto.TicketStatsSummaryDTO
import dev.yidafu.aqua.api.dto.TicketTypeStatsDTO
import dev.yidafu.aqua.api.dto.TicketUsagePointDTO
import dev.yidafu.aqua.api.service.marketing.TicketStatsQueryService
import dev.yidafu.aqua.common.domain.model.enums.UserTicketStatus
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.marketing.ticket.domain.repository.TicketPurchaseRepository
import dev.yidafu.aqua.marketing.ticket.domain.repository.TicketTypeRepository
import dev.yidafu.aqua.marketing.ticket.domain.repository.UserTicketRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * 桶水票票种销售/使用统计服务实现(后台详情图表,F-MKT-007):
 * 销售按已支付购票单(paidAt 回退 createdAt)按日聚合;使用按核销/退回/过期状态时间戳按日聚合;
 * summary 与序列同区间,availableCount 为当前存量;空数据日补零,保证图表 x 轴连续。
 */
@Service
class TicketStatsQueryServiceImpl(
  private val ticketTypeRepository: TicketTypeRepository,
  private val ticketPurchaseRepository: TicketPurchaseRepository,
  private val userTicketRepository: UserTicketRepository,
) : TicketStatsQueryService {
  override fun getTicketTypeStats(
    ticketTypeId: Long,
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): TicketTypeStatsDTO {
    if (startDate.isAfter(endDate)) {
      throw BadRequestException("日期范围无效:开始日期不能晚于结束日期")
    }
    val ticketTypeName = ticketTypeRepository.findById(ticketTypeId).orElse(null)?.name ?: "水票"
    // 左闭右开 [from, to),覆盖 endDate 全天
    val from = startDate
    val to = endDate.plusDays(1)

    val salesRows = ticketPurchaseRepository.findPaidSalesGroupedByDay(ticketTypeId, from, to)
    val usageRows = userTicketRepository.findUsageGroupedByDay(ticketTypeId, from, to)
    val availableCount =
      userTicketRepository
        .countByTicketTypeIdAndStatusInAndExpireTimeGreaterThan(
          ticketTypeId,
          listOf(UserTicketStatus.UNUSED, UserTicketStatus.RETURNED),
          LocalDateTime.now(),
        ).toInt()

    val salesByDay = salesRows.associateBy { it.getDay() }
    val usageByDay = usageRows.associateBy { it.getDay() }

    val sales = mutableListOf<TicketSalesPointDTO>()
    val usage = mutableListOf<TicketUsagePointDTO>()
    var soldCount = 0L
    var revenueCents = 0L
    var redeemedCount = 0L
    var returnedCount = 0L
    var expiredCount = 0L

    var day = startDate.toLocalDate()
    val end = endDate.toLocalDate()
    while (!day.isAfter(end)) {
      val salesRow = salesByDay[day]
      val purchaseCount = salesRow?.getPurchaseCount() ?: 0L
      val ticketCount = salesRow?.getTicketCount() ?: 0L
      val revenue = salesRow?.getRevenueCents() ?: 0L
      soldCount += ticketCount
      revenueCents += revenue
      sales += TicketSalesPointDTO(day, purchaseCount.toInt(), ticketCount.toInt(), revenue)

      val usageRow = usageByDay[day]
      val redeemed = usageRow?.getRedeemedCount() ?: 0L
      val returned = usageRow?.getReturnedCount() ?: 0L
      val expired = usageRow?.getExpiredCount() ?: 0L
      redeemedCount += redeemed
      returnedCount += returned
      expiredCount += expired
      usage += TicketUsagePointDTO(day, redeemed.toInt(), returned.toInt(), expired.toInt())

      day = day.plusDays(1)
    }

    val summary =
      TicketStatsSummaryDTO(
        soldCount = soldCount.toInt(),
        revenueCents = revenueCents,
        redeemedCount = redeemedCount.toInt(),
        returnedCount = returnedCount.toInt(),
        expiredCount = expiredCount.toInt(),
        availableCount = availableCount,
      )
    return TicketTypeStatsDTO(
      ticketTypeId = ticketTypeId,
      ticketTypeName = ticketTypeName,
      summary = summary,
      sales = sales,
      usage = usage,
    )
  }
}
