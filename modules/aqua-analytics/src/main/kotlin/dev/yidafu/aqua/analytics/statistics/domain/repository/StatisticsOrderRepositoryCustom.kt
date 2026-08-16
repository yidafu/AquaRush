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

import dev.yidafu.aqua.analytics.statistics.dto.DailyStatisticsDTO
import dev.yidafu.aqua.analytics.statistics.dto.OrderValueBucketDTO
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import java.time.LocalDateTime

/**
 * 订单统计 Repository 自定义接口
 * 提供按日期、周、月聚合的订单统计查询
 */
interface StatisticsOrderRepositoryCustom {
  /**
   * 按日期分组统计订单数量和金额
   */
  fun getDailyOrderStatistics(
    startDateTime: LocalDateTime,
    endDateTime: LocalDateTime,
    deliveryWorkerId: Long? = null,
    statuses: List<OrderModelStatus>? = null,
  ): List<DailyStatisticsDTO>

  /**
   * 按周分组统计订单数量和金额（周一作为周开始）
   */
  fun getWeeklyOrderStatistics(
    startDateTime: LocalDateTime,
    endDateTime: LocalDateTime,
    deliveryWorkerId: Long? = null,
    statuses: List<OrderModelStatus>? = null,
  ): List<DailyStatisticsDTO>

  /**
   * 按月分组统计订单数量和金额
   */
  fun getMonthlyOrderStatistics(
    startDateTime: LocalDateTime,
    endDateTime: LocalDateTime,
    deliveryWorkerId: Long? = null,
    statuses: List<OrderModelStatus>? = null,
  ): List<DailyStatisticsDTO>

  /**
   * 按当前状态快照计数（无日期过滤，存量统计）
   */
  fun countByStatusIn(statuses: List<OrderModelStatus>): Map<OrderModelStatus, Long>

  /**
   * 按状态分组统计日期窗口内订单数（G37 状态分布，全状态聚合）
   */
  fun countOrdersGroupByStatus(
    startDateTime: LocalDateTime,
    endDateTime: LocalDateTime,
  ): Map<OrderModelStatus, Long>

  /**
   * 按订单创建小时(0-23)分组统计日期窗口内订单数（G37 高峰时段分析，全状态聚合）
   */
  fun countOrdersGroupByHour(
    startDateTime: LocalDateTime,
    endDateTime: LocalDateTime,
  ): Map<Int, Long>

  /**
   * 按金额区间分桶统计日期窗口内订单数（G37 客单价分布，全状态聚合）
   * 金额按订单 amountCents 只计一次；buckets 由服务层定义（默认 0-20/20-50/50-100/100-200/200+ 元）。
   * @return label → 订单数
   */
  fun countOrdersByAmountBucket(
    startDateTime: LocalDateTime,
    endDateTime: LocalDateTime,
    buckets: List<OrderValueBucketDTO>,
  ): Map<String, Long>
}
