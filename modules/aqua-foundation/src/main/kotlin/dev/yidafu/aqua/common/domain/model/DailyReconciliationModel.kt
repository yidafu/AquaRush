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

package dev.yidafu.aqua.common.domain.model

import dev.yidafu.aqua.common.domain.model.enums.DailyReconciliationStatus
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 每日对账记录实体
 * 汇总每日所有配送员的收款和订单情况
 */
data class DailyReconciliationModel(
  var id: Long? = null,
  var reconciliationDate: LocalDate = LocalDate.now(),
  var totalCashAmountCents: Long = 0L,
  var totalWaterTicketCount: Int = 0,
  var totalQrCodeAmountCents: Long = 0L,
  var totalOrderCount: Int = 0,
  var totalOrderAmountCents: Long = 0L,
  var totalCollectionAmountCents: Long = 0L,
  var discrepancyAmountCents: Long = 0L,
  var status: DailyReconciliationStatus = DailyReconciliationStatus.PENDING,
  var reportData: Map<String, Any>? = null,
  var reviewedAt: LocalDateTime? = null,
  var reviewedBy: Long? = null,
  var reviewNotes: String? = null,
  val createdAt: LocalDateTime = LocalDateTime.now(),
  var updatedAt: LocalDateTime = LocalDateTime.now(),
)
