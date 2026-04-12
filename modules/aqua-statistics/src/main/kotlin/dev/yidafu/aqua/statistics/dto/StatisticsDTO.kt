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

package dev.yidafu.aqua.statistics.dto

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
) {
  val totalAmount: BigDecimal
    get() =
      MoneyUtils
        .fromCents(totalAmountCents)
  val completedAmount: BigDecimal
    get() =
      MoneyUtils
        .fromCents(completedAmountCents)
}

/**
 * 每日统计DTO
 */
data class DailyStatisticsDTO(
  val date: LocalDate,
  val orderCount: Long,
  val orderProductCount: Long,
  val totalAmountCents: Long,
) {
  val totalAmount: BigDecimal
    get() =
      MoneyUtils
        .fromCents(totalAmountCents)

  companion object {
    fun default(date: LocalDate) = DailyStatisticsDTO(date, 0L, 0L, 0L)
  }
}