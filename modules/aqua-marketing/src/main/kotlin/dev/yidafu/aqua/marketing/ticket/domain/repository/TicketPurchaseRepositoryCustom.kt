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
 * 购票单仓库 自定义查询(TicketPurchaseRepository 中手写 @Query 迁移至此)。
 */
interface TicketPurchaseRepositoryCustom {
  /**
   * 销售统计投影(按日):购票单数/入账张数(totalCount = N+M)/销售金额(分)。
   */
  interface TicketPaidSalesDay {
    fun getDay(): LocalDate

    fun getPurchaseCount(): Long

    fun getTicketCount(): Long

    fun getRevenueCents(): Long
  }

  /**
   * 按日汇总已支付购票(后台水票统计):[from, to) 左闭右开,paidAt 为空回退 createdAt;空数据日由服务层补零。
   */
  fun findPaidSalesGroupedByDay(
    ticketTypeId: Long,
    from: LocalDateTime,
    to: LocalDateTime,
  ): List<TicketPaidSalesDay>

  /**
   * 支付入账原子流转(PENDING→PAID,幂等):返回受影响行数(1=成功,0=已被处理/不存在)。
   */
  fun markPaidAtomically(
    id: Long,
    transactionId: String,
    now: LocalDateTime,
  ): Int
}
