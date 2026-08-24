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

package dev.yidafu.aqua.marketing.ticket.domain.repository

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 用户水票实例仓库 QueryDSL 自定义查询(UserTicketRepository 中手写 @Query 迁移至此)。
 */
interface UserTicketRepositoryCustom {
  /**
   * 使用统计投影(按日):已核销/已退回/已过期张数。
   * 时间取各状态对应时间戳:USED=usedAt,RETURNED=returnedAt,EXPIRED=returnedAt 回退 updatedAt。
   */
  interface TicketUsageDay {
    fun getDay(): LocalDate

    fun getRedeemedCount(): Long

    fun getReturnedCount(): Long

    fun getExpiredCount(): Long
  }

  /**
   * 按日汇总使用情况(后台水票统计):[from, to) 左闭右开,空数据日由服务层补零。
   */
  fun findUsageGroupedByDay(
    ticketTypeId: Long,
    from: LocalDateTime,
    to: LocalDateTime,
  ): List<TicketUsageDay>

  /**
   * 原子核销:UNUSED/RETURNED 且未过期可核销;受影响行数=1 成功,=0 已被并发核销/过期。
   */
  fun redeemAtomically(
    id: Long,
    userId: Long,
    orderId: Long,
    now: LocalDateTime,
  ): Int

  /**
   * 订单取消/退款退回(幂等):该订单已核销票,未过期→RETURNED 可复用,过期→EXPIRED。
   */
  fun returnByOrder(
    orderId: Long,
    now: LocalDateTime,
  ): Int
}
