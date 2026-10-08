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

package dev.yidafu.aqua.analytics.statistics.dto

import dev.yidafu.aqua.common.utils.MoneyUtils
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 订单统计DTO
 */
data class OrderStatisticsDTO(
  val totalOrders: Long,
  val totalAmountCents: Long,
  val completedOrders: Long,
  val completedAmountCents: Long,
  // 已退款金额(分,payment_refunds status=COMPLETED 按退款成功时间归属窗口,PRD F-STA-002)
  val refundedAmountCents: Long = 0L,
) {
  val totalAmount: BigDecimal
    get() =
      MoneyUtils
        .fromCents(totalAmountCents)
  val completedAmount: BigDecimal
    get() =
      MoneyUtils
        .fromCents(completedAmountCents)

  /** 净营收(分)= 已完成营收 − 已退款金额;跨窗口退款时可为负(如实展示) */
  val netRevenueCents: Long
    get() = completedAmountCents - refundedAmountCents
}

/**
 * 每日统计DTO
 */
data class DailyStatisticsDTO(
  val date: LocalDate,
  val orderCount: Long,
  val orderProductCount: Long,
  val totalAmountCents: Long,
  // 该周期已退款金额(分,退款记录 status=COMPLETED 按退款成功时间归属)
  val refundedAmountCents: Long = 0L,
) {
  val totalAmount: BigDecimal
    get() =
      MoneyUtils
        .fromCents(totalAmountCents)

  companion object {
    fun default(date: LocalDate) = DailyStatisticsDTO(date, 0L, 0L, 0L)
  }
}

/**
 * 待处理订单存量快照 DTO（待支付/待派单/待配送，当前状态，与日期窗口无关）
 */
data class PendingOrderBreakdownDTO(
  val pendingPayment: Long,
  val pendingDispatch: Long,
  val pendingDelivery: Long,
) {
  val total: Long
    get() = pendingPayment + pendingDispatch + pendingDelivery
}
