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
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.marketing.ticket.domain.exception.TicketPurchaseNotFoundException
import dev.yidafu.aqua.marketing.ticket.domain.exception.TicketTypeNotFoundException
import dev.yidafu.aqua.marketing.ticket.domain.repository.TicketPurchaseRepository
import dev.yidafu.aqua.marketing.ticket.domain.repository.TicketTypeRepository
import dev.yidafu.aqua.marketing.ticket.domain.repository.UserTicketRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
  private val bizLogger: BizLogger,
) : TicketMutationApiService {
  @Transactional
  override fun purchaseTickets(
    userId: Long,
    request: PurchaseTicketsRequest,
  ): TicketPurchaseModel {
    // 防御校验:单次购票张数 1~10(GraphQL input 已约束,服务层兜底防直连调用越界)
    require(request.packageCount in 1..10) { "单次购票张数必须为1-10张" }
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
    val saved = ticketPurchaseRepository.save(purchase)

    // 业务日志(F-LOG-003 MARKETING 域):购票发起
    bizLogger.logMarketingOperation(
      targetId = saved.id ?: 0L,
      operation = "TICKET_PURCHASED",
      result = "SUCCESS",
      userId = userId,
      additionalData =
        mapOf(
          "ticketTypeId" to ticketTypeId,
          "ticketTypeName" to ticketType.name,
          "packageCount" to request.packageCount,
          "totalCount" to totalCount,
          "amountCents" to saved.packagePriceCents,
        ),
    )
    return saved
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
      // 并发已入账:重读最新状态返回(实体已脱离 JPA 持久化上下文,原 JPA refresh 的等价语义)
      return ticketPurchaseRepository.findById(ticketPurchaseId).orElse(purchase)
    }
    val ticketType = ticketTypeRepository.findById(purchase.ticketTypeId).orElseThrow { TicketTypeNotFoundException(purchase.ticketTypeId) }
    // 票包入账 totalCount 张(N 张正常 + M 张赠送,同池核销/过期口径一致 R-MKT-08);
    // 每张票生成唯一递增票号:SP + 入账年月(yyyyMM) + 8 位序号,按月重置(v2.2.0)
    val expireTime =
      when (ticketType.validityType) {
        ValidityType.FIXED_RANGE -> ticketType.validTo ?: now
        ValidityType.DAYS_AFTER_PURCHASE -> now.plusDays((ticketType.validDays ?: 0).toLong())
        else -> now
      }
    val monthKey = DateTimeFormatter.ofPattern("yyyyMM").format(now)
    repeat(purchase.totalCount) { index ->
      val seq = userTicketRepository.nextTicketSeq(monthKey)
      userTicketRepository.save(
        UserTicketModel(
          userId = purchase.userId,
          ticketTypeId = purchase.ticketTypeId,
          purchaseId = ticketPurchaseId,
          ticketNo = "SP$monthKey${seq.toString().padStart(8, '0')}",
          status = dev.yidafu.aqua.common.domain.model.enums.UserTicketStatus.UNUSED,
          expireTime = expireTime,
          isGift = index >= purchase.packageCount,
          createdAt = now,
          updatedAt = now,
        ),
      )
    }
    // 重读入账后的最新状态(PAID)返回
    return ticketPurchaseRepository.findById(ticketPurchaseId).orElse(purchase)
  }

  @Transactional
  override fun returnTicketsByOrder(orderId: Long): Int {
    val now = LocalDateTime.now()
    return userTicketRepository.returnByOrder(orderId, now)
  }

  private fun buildTicketOrderNo(ticketTypeId: Long): String = "TP$ticketTypeId${System.currentTimeMillis()}"
}
