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

package dev.yidafu.aqua.api.service.marketing

import dev.yidafu.aqua.api.dto.TicketTypeStatsDTO
import java.time.LocalDateTime

/**
 * 桶水票票种销售/使用统计查询服务(管理后台详情图表,PRD 10-marketing.md §F-MKT-007)
 */
interface TicketStatsQueryService {
  /**
   * 指定票种在 [startDate, endDate] 区间的销售/使用统计:
   * summary 与序列同区间;availableCount 为当前存量;空数据日补零。
   * 日期非法(startDate > endDate)抛 BadRequestException。
   */
  fun getTicketTypeStats(
    ticketTypeId: Long,
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): TicketTypeStatsDTO
}
