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

package dev.yidafu.aqua.analytics.reconciliation.dto

import java.time.LocalDate

/**
 * 订单统计结果DTO
 */
data class OrderStatsDTO(
  val orderCount: Int,
  val orderAmountCents: Long,
)

/**
 * 支付类型统计结果DTO
 */
data class PaymentStatsDTO(
  val cashAmountCents: Long,
  val waterTicketCount: Int,
  val qrCodeAmountCents: Long,
)

/**
 * 指定日期收银汇总(后台收银对账顶部汇总卡数据源,按日报记录聚合):
 * 现金/扫码/水票为送水工填报合计,订单金额/订单数为系统计算合计。
 */
data class DailyCollectionSummaryDTO(
  val collectionDate: LocalDate,
  val totalCashAmountCents: Long,
  val totalQrCodeAmountCents: Long,
  val totalWaterTicketCount: Int,
  val totalOrderCount: Int,
  val totalOrderAmountCents: Long,
  /** 已提交日报的送水工人数(草稿不计入) */
  val totalSubmittedCount: Int,
)
