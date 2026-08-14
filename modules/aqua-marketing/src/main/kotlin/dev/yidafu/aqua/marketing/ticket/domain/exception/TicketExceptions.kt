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

package dev.yidafu.aqua.marketing.ticket.domain.exception

/**
 * 桶水票异常(PRD 10-marketing.md §F-MKT-007/R-MKT-08)
 */
class TicketTypeNotFoundException(
  ticketTypeId: Long,
) : RuntimeException("票种不存在: $ticketTypeId")

/**
 * 购票单不存在
 */
class TicketPurchaseNotFoundException(
  purchaseId: Long,
) : RuntimeException("购票单不存在: $purchaseId")

/**
 * 票不可核销(类型不匹配/规格不通用/过期/数量不足)
 */
class TicketNotRedeemableException(
  message: String,
) : RuntimeException(message)
