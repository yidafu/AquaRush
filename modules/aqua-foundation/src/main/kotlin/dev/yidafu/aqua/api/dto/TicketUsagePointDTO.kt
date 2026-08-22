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
 * 水票使用按日统计点(后台票种详情图表,F-MKT-007):
 * 核销(USED)/退回(RETURNED)/过期(EXPIRED)张数。
 */
data class TicketUsagePointDTO(
  val date: LocalDate,
  val redeemedCount: Int,
  val returnedCount: Int,
  val expiredCount: Int,
)
