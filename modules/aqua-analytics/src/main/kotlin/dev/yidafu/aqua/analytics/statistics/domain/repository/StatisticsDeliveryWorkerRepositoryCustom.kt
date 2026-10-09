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

package dev.yidafu.aqua.analytics.statistics.domain.repository

import java.time.LocalDateTime

/**
 * 已完成订单履约时间要素(F-STA-004,G36 平均履约时长):completedAt − 派单时间
 */
data class CompletedOrderTiming(
  val orderId: Long,
  val completedAt: LocalDateTime,
  val deliveryStartedAt: LocalDateTime?,
  val createdAt: LocalDateTime,
)

/**
 * 送水员统计 Repository 自定义接口
 */
interface StatisticsDeliveryWorkerRepositoryCustom {
  /**
   * 统计今日有活跃订单的送水员数
   */
  fun countTodayActiveWorkers(sinceDateTime: LocalDateTime): Long

  /**
   * 统计指定时间范围内的配送中订单数
   */
  fun countDeliveringOrders(): Long

  /**
   * 统计指定时间范围内已完成的订单数
   */
  fun countCompletedOrdersSince(dateTime: LocalDateTime): Long

  /**
   * 查询指定时间范围内已完成订单的履约时间要素(F-STA-004,G36)
   */
  fun findCompletedOrderTimingsSince(dateTime: LocalDateTime): List<CompletedOrderTiming>

  /**
   * 查询各订单最早派单(DELIVERY_ASSIGNED)操作时间(F-STA-004,G36):
   * 派单时间 = order_operations 中 operation_type=DELIVERY_ASSIGNED 的最早 createdAt;
   * 未命中(历史订单/事件未落)的订单不在返回 map 中,由服务层回退 deliveryStartedAt/createdAt。
   */
  fun findEarliestAssignedAtByOrderIds(orderIds: List<Long>): Map<Long, LocalDateTime>

  /**
   * 今日各送水员完成单数(R-ORD-11:待确认收货+已完成,按送达时间归属,送水员非空)。
   * 原实现位于 DeliveryWorkerStatisticsServiceImpl 内的 QueryDSL 查询(P3 迁移时搬入仓储)。
   */
  fun countTodayCompletedOrdersByWorker(sinceDateTime: LocalDateTime): Map<Long, Int>
}
