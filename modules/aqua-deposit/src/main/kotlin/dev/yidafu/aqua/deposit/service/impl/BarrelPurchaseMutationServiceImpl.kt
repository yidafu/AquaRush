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

package dev.yidafu.aqua.deposit.service.impl

import dev.yidafu.aqua.api.dto.PurchaseBarrelRequest
import dev.yidafu.aqua.api.service.barrel.BarrelPurchaseMutationService
import dev.yidafu.aqua.common.domain.model.BarrelPurchaseItemModel
import dev.yidafu.aqua.common.domain.model.BarrelPurchaseModel
import dev.yidafu.aqua.common.domain.model.enums.BarrelFeeStatus
import dev.yidafu.aqua.common.domain.model.enums.BarrelPaymentStatus
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.deposit.domain.repository.BarrelFeeRepository
import dev.yidafu.aqua.deposit.domain.repository.BarrelPurchaseItemRepository
import dev.yidafu.aqua.deposit.domain.repository.BarrelPurchaseRepository
import dev.yidafu.aqua.logging.util.BizLogger
import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 水桶购买单变更服务实现(G06:一次性塑料水桶费,独立支付):
 * 购桶(校验价目 ENABLED + 每用户总限购一次,建 PENDING 购桶单与明细快照)→ 支付回调入账(PENDING→PAID 原子,幂等)→ 管理员手动退还(REFUNDED)。
 */
@Service
class BarrelPurchaseMutationServiceImpl(
  private val barrelFeeRepository: BarrelFeeRepository,
  private val barrelPurchaseRepository: BarrelPurchaseRepository,
  private val barrelPurchaseItemRepository: BarrelPurchaseItemRepository,
  private val entityManager: EntityManager,
  private val bizLogger: BizLogger,
) : BarrelPurchaseMutationService {
  private val logger = LoggerFactory.getLogger(BarrelPurchaseMutationServiceImpl::class.java)

  @Transactional
  override fun purchaseBarrel(
    userId: Long,
    request: PurchaseBarrelRequest,
  ): BarrelPurchaseModel {
    // 每用户总限购一次:存在已支付(PAID)购桶单即拦截
    if (barrelPurchaseRepository.existsByUserIdAndPaymentStatus(userId, BarrelPaymentStatus.PAID)) {
      throw BadRequestException("您已购买过水桶,无需重复购买")
    }
    // 校验价目(存在且 ENABLED),组装明细快照
    val items =
      request.items.map { item ->
        val fee =
          barrelFeeRepository.findById(item.barrelFeeId).orElseThrow { BadRequestException("水桶费价目不存在: ${item.barrelFeeId}") }
        if (fee.status != BarrelFeeStatus.ENABLED) {
          throw BadRequestException("水桶费价目已停用: ${fee.specification}")
        }
        BarrelPurchaseItemModel(
          barrelFeeId = fee.id!!,
          specification = fee.specification,
          unitPriceCents = fee.priceCents,
          quantity = item.quantity,
          amountCents = fee.priceCents * item.quantity,
        )
      }
    val now = LocalDateTime.now()
    // 重复发起:先取消该用户未支付(PENDING)旧购桶单,保证单用户在途购桶单唯一(幂等化)
    barrelPurchaseRepository.cancelPendingByUser(userId, now)
    val totalAmountCents = items.sumOf { it.amountCents }
    val purchase =
      barrelPurchaseRepository.save(
        BarrelPurchaseModel(
          userId = userId,
          orderNo = buildBarrelOrderNo(userId),
          totalAmountCents = totalAmountCents,
          paymentStatus = BarrelPaymentStatus.PENDING,
          createdAt = now,
          updatedAt = now,
        ),
      )
    items.forEach { it.purchaseId = purchase.id!! }
    barrelPurchaseItemRepository.saveAll(items)
    logger.info("购桶单创建: userId={}, purchaseId={}, totalAmountCents={}", userId, purchase.id, totalAmountCents)
    bizLogger.logOperation(
      operation = "BARREL_PURCHASED",
      module = "PAYMENT",
      result = "SUCCESS",
      target = purchase.id?.toString() ?: "barrel",
      userId = userId,
      additionalData =
        mapOf(
          "orderNo" to purchase.orderNo,
          "totalAmountCents" to totalAmountCents,
        ),
    )
    return purchase
  }

  @Transactional
  override fun handleBarrelPurchasePaid(
    purchaseId: Long,
    transactionId: String,
  ): BarrelPurchaseModel {
    val purchase =
      barrelPurchaseRepository
        .findById(purchaseId)
        .orElseThrow { BadRequestException("购桶单不存在: $purchaseId") }
    // 幂等:已入账直接返回(重复回调)
    if (purchase.paymentStatus == BarrelPaymentStatus.PAID) {
      return purchase
    }
    val now = LocalDateTime.now()
    // 原子 PENDING→PAID;受影响行数=0 表示并发已入账
    val affected = barrelPurchaseRepository.markPaidAtomically(purchaseId, transactionId, now)
    if (affected == 0) {
      entityManager.refresh(purchase)
      return purchase
    }
    entityManager.refresh(purchase)
    logger.info("购桶单支付入账: purchaseId={}, transactionId={}", purchaseId, transactionId)
    bizLogger.logOperation(
      operation = "BARREL_PURCHASE_PAID",
      module = "PAYMENT",
      result = "SUCCESS",
      target = purchaseId.toString(),
      userId = purchase.userId,
      additionalData = mapOf("transactionId" to transactionId),
    )
    return purchase
  }

  @Transactional
  override fun markRefunded(
    purchaseId: Long,
    refundedBy: Long,
    remark: String?,
  ): BarrelPurchaseModel {
    val purchase =
      barrelPurchaseRepository
        .findById(purchaseId)
        .orElseThrow { BadRequestException("购桶单不存在: $purchaseId") }
    // 幂等:已退还直接返回
    if (purchase.paymentStatus == BarrelPaymentStatus.REFUNDED) {
      logger.info("购桶单 {} 已退还,跳过", purchaseId)
      return purchase
    }
    if (purchase.paymentStatus != BarrelPaymentStatus.PAID) {
      throw BadRequestException("购桶单状态为 ${purchase.paymentStatus},不可退还")
    }
    val now = LocalDateTime.now()
    purchase.paymentStatus = BarrelPaymentStatus.REFUNDED
    purchase.refundedAt = now
    purchase.refundedBy = refundedBy
    purchase.refundRemark = remark
    purchase.updatedAt = now
    val saved = barrelPurchaseRepository.save(purchase)
    logger.info("购桶单退还: purchaseId={}, totalAmountCents={}", purchaseId, saved.totalAmountCents)
    return saved
  }

  private fun buildBarrelOrderNo(userId: Long): String = "BK$userId${System.currentTimeMillis()}"
}
