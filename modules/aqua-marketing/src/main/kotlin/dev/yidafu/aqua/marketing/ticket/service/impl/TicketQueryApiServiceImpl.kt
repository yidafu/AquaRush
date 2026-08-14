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

package dev.yidafu.aqua.marketing.ticket.service.impl

import dev.yidafu.aqua.api.service.marketing.TicketQueryApiService
import dev.yidafu.aqua.common.domain.model.TicketPurchaseModel
import dev.yidafu.aqua.common.domain.model.TicketTypeModel
import dev.yidafu.aqua.common.domain.model.UserTicketModel
import dev.yidafu.aqua.common.domain.model.enums.TicketPurchaseStatus
import dev.yidafu.aqua.common.domain.model.enums.TicketTypeKind
import dev.yidafu.aqua.common.domain.model.enums.TicketTypeStatus
import dev.yidafu.aqua.common.domain.model.enums.UserTicketStatus
import dev.yidafu.aqua.marketing.ticket.domain.repository.TicketPurchaseRepository
import dev.yidafu.aqua.marketing.ticket.domain.repository.TicketTypeRepository
import dev.yidafu.aqua.marketing.ticket.domain.repository.UserTicketRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * 桶水票用户侧查询服务实现(水票商城/我的水票;促销引擎核销预检数据源)
 */
@Service
class TicketQueryApiServiceImpl(
  private val ticketTypeRepository: TicketTypeRepository,
  private val userTicketRepository: UserTicketRepository,
  private val ticketPurchaseRepository: TicketPurchaseRepository,
) : TicketQueryApiService {
  override fun findActiveTicketTypes(): List<TicketTypeModel> = ticketTypeRepository.findByStatus(TicketTypeStatus.ACTIVE)

  override fun findUserTickets(
    userId: Long,
    status: UserTicketStatus?,
  ): List<UserTicketModel> =
    if (status == null) {
      userTicketRepository.findByUserId(userId)
    } else {
      userTicketRepository.findByUserIdAndStatus(userId, status)
    }

  override fun findUsableTickets(userId: Long): List<UserTicketModel> {
    val now = LocalDateTime.now()
    return userTicketRepository
      .findByUserIdAndStatusIn(userId, listOf(UserTicketStatus.UNUSED, UserTicketStatus.RETURNED))
      .filter { it.expireTime.isAfter(now) }
  }

  /**
   * 可用张数预检:指定商品票按 productId 匹配;通兑票按 specification 字符串匹配(跨规格不通用,防跨容量套利 R-MKT-08)。
   */
  override fun countUsableFor(
    userId: Long,
    productId: Long,
    specification: String?,
  ): Int {
    val usable = findUsableTickets(userId)
    val types = usable.mapNotNull { ticketTypeRepository.findById(it.ticketTypeId).orElse(null) }
    val byTypeId = types.associateBy { it.id!! }
    return usable.count { ticket ->
      val type = byTypeId[ticket.ticketTypeId] ?: return@count false
      when (type.ticketType) {
        TicketTypeKind.SPECIFIC_PRODUCT -> type.productId == productId
        TicketTypeKind.UNIVERSAL -> type.specification != null && type.specification == specification
      }
    }
  }

  override fun countPurchasedByTicketType(
    userId: Long,
    ticketTypeId: Long,
  ): Int = ticketPurchaseRepository.countByUserIdAndTicketTypeIdAndPaymentStatus(userId, ticketTypeId, TicketPurchaseStatus.PAID).toInt()

  override fun findPurchaseById(id: Long): TicketPurchaseModel? = ticketPurchaseRepository.findById(id).orElse(null)

  override fun findPurchaseByOrderNo(orderNo: String): TicketPurchaseModel? = ticketPurchaseRepository.findByOrderNo(orderNo)
}
