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

import dev.yidafu.aqua.api.dto.PurchaseTicketsRequest
import dev.yidafu.aqua.api.service.marketing.TicketMutationApiService
import dev.yidafu.aqua.common.domain.model.TicketPurchaseModel
import dev.yidafu.aqua.common.domain.model.UserTicketModel
import dev.yidafu.aqua.common.domain.model.enums.TicketPurchaseStatus
import dev.yidafu.aqua.common.domain.model.enums.TicketTypeStatus
import dev.yidafu.aqua.common.domain.model.enums.ValidityType
import dev.yidafu.aqua.marketing.ticket.domain.exception.TicketPurchaseNotFoundException
import dev.yidafu.aqua.marketing.ticket.domain.exception.TicketTypeNotFoundException
import dev.yidafu.aqua.marketing.ticket.domain.repository.TicketPurchaseRepository
import dev.yidafu.aqua.marketing.ticket.domain.repository.TicketTypeRepository
import dev.yidafu.aqua.marketing.ticket.domain.repository.UserTicketRepository
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 桶水票用户侧变更服务实现(F-MKT-007):
 * 购票(校验 ACTIVE+限购,建 PENDING 购票单)→ 支付回调入账(原子 PENDING→PAID+票包入账 N+M,幂等)→ 取消退票。
 * 购票即预收款项,一经购买不支持退款(R-MKT-08)。
 */
@Service
class TicketMutationApiServiceImpl(
  private val ticketTypeRepository: TicketTypeRepository,
  private val ticketPurchaseRepository: TicketPurchaseRepository,
  private val userTicketRepository: UserTicketRepository,
  private val entityManager: EntityManager,
) : TicketMutationApiService {
  @Transactional
  override fun purchaseTickets(
    userId: Long,
    request: PurchaseTicketsRequest,
  ): TicketPurchaseModel {
    val ticketType = ticketTypeRepository.findById(request.ticketTypeId).orElseThrow { TicketTypeNotFoundException(request.ticketTypeId) }
    if (ticketType.status != TicketTypeStatus.ACTIVE) {
      throw TicketTypeNotFoundException(request.ticketTypeId)
    }
    val ticketTypeId = ticketType.id ?: throw TicketTypeNotFoundException(request.ticketTypeId)
    // 限购校验(按已支付购票张数)
    if (ticketType.perUserLimit > 0) {
      val purchased = ticketPurchaseRepository.countByUserIdAndTicketTypeIdAndPaymentStatus(userId, ticketTypeId, TicketPurchaseStatus.PAID)
      if (purchased + request.packageCount > ticketType.perUserLimit) {
        throw IllegalArgumentException("超过限购张数(限购 ${ticketType.perUserLimit} 张)")
      }
    }
    val giftCount = ticketType.giftCount * request.packageCount
    val totalCount = request.packageCount + giftCount
    val now = LocalDateTime.now()
    val purchase =
      TicketPurchaseModel(
        userId = userId,
        ticketTypeId = ticketTypeId,
        orderNo = buildTicketOrderNo(ticketTypeId),
        packageCount = request.packageCount,
        giftCount = giftCount,
        totalCount = totalCount,
        packagePriceCents = ticketType.packagePriceCents * request.packageCount,
        paymentStatus = TicketPurchaseStatus.PENDING,
        createdAt = now,
        updatedAt = now,
      )
    return ticketPurchaseRepository.save(purchase)
  }

  @Transactional
  override fun handleTicketPurchasePaid(
    ticketPurchaseId: Long,
    transactionId: String,
  ): TicketPurchaseModel {
    val purchase = ticketPurchaseRepository.findById(ticketPurchaseId).orElseThrow { TicketPurchaseNotFoundException(ticketPurchaseId) }
    // 幂等:已入账直接返回(重复回调)
    if (purchase.paymentStatus == TicketPurchaseStatus.PAID) {
      return purchase
    }
    val now = LocalDateTime.now()
    // 原子 PENDING→PAID;受影响行数=0 表示并发已入账
    val affected = ticketPurchaseRepository.markPaidAtomically(ticketPurchaseId, transactionId, now)
    if (affected == 0) {
      // 并发已入账:刷新为最新状态后返回
      entityManager.refresh(purchase)
      return purchase
    }
    val ticketType = ticketTypeRepository.findById(purchase.ticketTypeId).orElseThrow { TicketTypeNotFoundException(purchase.ticketTypeId) }
    // 票包入账 totalCount 张(N 张正常 + M 张赠送,同池核销/过期口径一致 R-MKT-08)
    val expireTime =
      when (ticketType.validityType) {
        ValidityType.FIXED_RANGE -> ticketType.validTo ?: now
        ValidityType.DAYS_AFTER_PURCHASE -> now.plusDays((ticketType.validDays ?: 0).toLong())
        else -> now
      }
    repeat(purchase.totalCount) { index ->
      userTicketRepository.save(
        UserTicketModel(
          userId = purchase.userId,
          ticketTypeId = purchase.ticketTypeId,
          purchaseId = ticketPurchaseId,
          status = dev.yidafu.aqua.common.domain.model.enums.UserTicketStatus.UNUSED,
          expireTime = expireTime,
          isGift = index >= purchase.packageCount,
          createdAt = now,
          updatedAt = now,
        ),
      )
    }
    // 批量更新后刷新持久化上下文中的实体为最新状态(PAID)
    entityManager.refresh(purchase)
    return purchase
  }

  @Transactional
  override fun returnTicketsByOrder(orderId: Long): Int {
    val now = LocalDateTime.now()
    return userTicketRepository.returnByOrder(orderId, now)
  }

  private fun buildTicketOrderNo(ticketTypeId: Long): String = "TP$ticketTypeId${System.currentTimeMillis()}"
}
