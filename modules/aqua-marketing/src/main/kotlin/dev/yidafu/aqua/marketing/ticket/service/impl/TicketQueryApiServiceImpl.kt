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
import dev.yidafu.aqua.api.service.order.OrderQueryApiService
import dev.yidafu.aqua.common.domain.model.TicketPurchaseModel
import dev.yidafu.aqua.common.domain.model.TicketTypeModel
import dev.yidafu.aqua.common.domain.model.UserTicketModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
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
  private val orderQueryApiService: OrderQueryApiService,
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
   * G97 复购激活待激活判定(R-MKT-08 运营策略):
   * 仅 UNUSED/RETURNED 且未过期的票可能待激活;购票单落了基线且票种当前启用激活才参与判定;
   * 口径 = 当前已完成订单数 − 基线 < activation_order_count → 待激活(条件满足即自动可用,无需手动激活)。
   * 无基线候选时短路,不查订单数。
   */
  override fun findPendingActivationIds(
    userId: Long,
    tickets: List<UserTicketModel>,
  ): Set<Long> {
    if (tickets.isEmpty()) return emptySet()
    val now = LocalDateTime.now()
    val candidates =
      tickets.filter {
        (it.status == UserTicketStatus.UNUSED || it.status == UserTicketStatus.RETURNED) && it.expireTime.isAfter(now)
      }
    if (candidates.isEmpty()) return emptySet()
    val purchaseById = ticketPurchaseRepository.findByIds(candidates.map { it.purchaseId }.distinct()).associateBy { it.id!! }
    val gated =
      candidates.filter { ticket ->
        val purchase = purchaseById[ticket.purchaseId] ?: return@filter false
        purchase.completedOrdersAtPurchase != null
      }
    if (gated.isEmpty()) return emptySet()
    val typeById = ticketTypeRepository.findAllById(gated.map { it.ticketTypeId }.distinct()).associateBy { it.id!! }
    // 类型层短路:票种未启用复购激活(配置变更不影响已购票 R-MKT-08)→ 不查订单数
    val activationEnabled =
      gated.filter { ticket ->
        val required = typeById[ticket.ticketTypeId]?.activationOrderCount
        required != null && required > 0
      }
    if (activationEnabled.isEmpty()) return emptySet()
    val completed = orderQueryApiService.countByUserIdGroupByStatus(userId)[OrderModelStatus.COMPLETED] ?: 0L
    return activationEnabled
      .filter { ticket ->
        val required = typeById[ticket.ticketTypeId]!!.activationOrderCount!!
        val baseline = purchaseById[ticket.purchaseId]!!.completedOrdersAtPurchase!!
        completed - baseline < required
      }.mapNotNull { it.id }
      .toSet()
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
