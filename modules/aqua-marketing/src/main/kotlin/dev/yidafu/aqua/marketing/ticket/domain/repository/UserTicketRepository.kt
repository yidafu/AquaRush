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

import dev.yidafu.aqua.common.domain.model.UserTicketModel
import dev.yidafu.aqua.common.domain.model.enums.UserTicketStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 用户水票实例仓库(F-MKT-007,每票一行):
 * 核销/退回用原子条件 UPDATE(防并发重复核销,核销幂等 R-MKT-08)。
 */
@Repository
interface UserTicketRepository :
  JpaRepository<UserTicketModel, Long>,
  JpaSpecificationExecutor<UserTicketModel>,
  UserTicketSequenceRepository {
  fun findByUserId(userId: Long): List<UserTicketModel>

  fun findByUserIdAndStatus(
    userId: Long,
    status: UserTicketStatus,
  ): List<UserTicketModel>

  fun findByUserIdAndStatusIn(
    userId: Long,
    statuses: List<UserTicketStatus>,
  ): List<UserTicketModel>

  fun findByUsedOrderId(orderId: Long): List<UserTicketModel>

  fun findByPurchaseId(purchaseId: Long): List<UserTicketModel>

  /**
   * 当前可用张数(UNUSED + 未过期 RETURNED),后台水票统计「当前可用」口径。
   */
  fun countByTicketTypeIdAndStatusInAndExpireTimeGreaterThan(
    ticketTypeId: Long,
    statuses: List<UserTicketStatus>,
    expireTime: LocalDateTime,
  ): Long

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
  @Query(
    """
    select cast(coalesce(t.usedAt, t.returnedAt, t.updatedAt) as date) as day,
           sum(case when t.status = dev.yidafu.aqua.common.domain.model.enums.UserTicketStatus.USED then 1 else 0 end) as redeemedCount,
           sum(case when t.status = dev.yidafu.aqua.common.domain.model.enums.UserTicketStatus.RETURNED then 1 else 0 end) as returnedCount,
           sum(case when t.status = dev.yidafu.aqua.common.domain.model.enums.UserTicketStatus.EXPIRED then 1 else 0 end) as expiredCount
    from UserTicketModel t
    where t.ticketTypeId = :ticketTypeId
      and t.status in (
        dev.yidafu.aqua.common.domain.model.enums.UserTicketStatus.USED,
        dev.yidafu.aqua.common.domain.model.enums.UserTicketStatus.RETURNED,
        dev.yidafu.aqua.common.domain.model.enums.UserTicketStatus.EXPIRED
      )
      and coalesce(t.usedAt, t.returnedAt, t.updatedAt) >= :from
      and coalesce(t.usedAt, t.returnedAt, t.updatedAt) < :to
    group by cast(coalesce(t.usedAt, t.returnedAt, t.updatedAt) as date)
    order by day
    """,
  )
  fun findUsageGroupedByDay(
    @Param("ticketTypeId") ticketTypeId: Long,
    @Param("from") from: LocalDateTime,
    @Param("to") to: LocalDateTime,
  ): List<TicketUsageDay>

  /**
   * 原子核销:UNUSED/RETURNED 且未过期可核销;受影响行数=1 成功,=0 已被并发核销/过期。
   */
  @Modifying
  @Query(
    """
    update UserTicketModel t
    set t.status = USED, t.usedOrderId = :orderId, t.usedAt = :now, t.updatedAt = :now
    where t.id = :id and t.userId = :userId
      and t.status in (
        dev.yidafu.aqua.common.domain.model.enums.UserTicketStatus.UNUSED,
        dev.yidafu.aqua.common.domain.model.enums.UserTicketStatus.RETURNED
      ) and t.expireTime > :now
    """,
  )
  fun redeemAtomically(
    @Param("id") id: Long,
    @Param("userId") userId: Long,
    @Param("orderId") orderId: Long,
    @Param("now") now: LocalDateTime,
  ): Int

  /**
   * 订单取消/退款退回(幂等):该订单已核销票,未过期→RETURNED 可复用,过期→EXPIRED。
   */
  @Modifying
  @Query(
    """
    update UserTicketModel t
    set t.status = case when t.expireTime > :now
        then dev.yidafu.aqua.common.domain.model.enums.UserTicketStatus.RETURNED
        else dev.yidafu.aqua.common.domain.model.enums.UserTicketStatus.EXPIRED end,
        t.usedOrderId = null, t.usedAt = null, t.returnedAt = :now, t.updatedAt = :now
    where t.usedOrderId = :orderId and t.status = dev.yidafu.aqua.common.domain.model.enums.UserTicketStatus.USED
    """,
  )
  fun returnByOrder(
    @Param("orderId") orderId: Long,
    @Param("now") now: LocalDateTime,
  ): Int
}
