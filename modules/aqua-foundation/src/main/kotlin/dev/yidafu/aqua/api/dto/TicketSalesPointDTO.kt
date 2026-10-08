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

package dev.yidafu.aqua.api.dto

import java.time.LocalDate

/**
 * 水票销售按日统计点(后台票种详情图表,F-MKT-007):
 * 金额一律为分(R-MON-01)。
 */
data class TicketSalesPointDTO(
  val date: LocalDate,
  val purchaseCount: Int,
  val ticketCount: Int,
  val revenueCents: Long,
)
