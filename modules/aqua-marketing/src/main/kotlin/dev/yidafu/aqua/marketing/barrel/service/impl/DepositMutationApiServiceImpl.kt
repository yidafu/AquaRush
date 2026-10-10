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

package dev.yidafu.aqua.marketing.barrel.service.impl

import dev.yidafu.aqua.api.dto.DepositCalculation
import dev.yidafu.aqua.api.dto.DepositLine
import dev.yidafu.aqua.api.dto.EmptyBarrelRecoveryItem
import dev.yidafu.aqua.api.dto.OrderDepositItem
import dev.yidafu.aqua.api.service.deposit.DepositMutationApiService
import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.common.domain.model.BarrelRecoveryRecordModel
import dev.yidafu.aqua.common.domain.model.DepositRecordModel
import dev.yidafu.aqua.common.domain.model.enums.DepositPriceStatus
import dev.yidafu.aqua.common.domain.model.enums.DepositStatus
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.marketing.barrel.domain.repository.BarrelRecoveryRecordRepository
import dev.yidafu.aqua.marketing.barrel.domain.repository.DepositPriceRepository
import dev.yidafu.aqua.marketing.barrel.domain.repository.DepositRecordRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 水桶押金变更服务实现(R-MON-03 押金账本):
 * 试算 [calcOrderDeposit] 与下单建账 [createPendingDeposits] 同口径——按规格启用押金价目 ×
 * 本单该规格桶数,同规格已有 HELD/PENDING 记录不收(在租只收一次,同时防两笔未支付订单重复收);
 * 支付成功/代客下单确认 [markHeldByOrder] 置 HELD,取消/超时 [cancelPendingByOrder] 清 PENDING,
 * 退款联动 [markRefundedByOrder](HELD→REFUNDED 状态谓词条件更新幂等)。
 */
@Service
class DepositMutationApiServiceImpl(
  private val depositRecordRepository: DepositRecordRepository,
  private val barrelFeeRepository: DepositPriceRepository,
  private val barrelRecoveryRecordRepository: BarrelRecoveryRecordRepository,
  private val productQueryApiService: ProductQueryApiService,
  private val bizLogger: BizLogger,
) : DepositMutationApiService {
  private val logger = LoggerFactory.getLogger(DepositMutationApiServiceImpl::class.java)

  @Transactional
  override fun calcOrderDeposit(
    userId: Long,
    items: List<OrderDepositItem>,
  ): DepositCalculation {
    if (items.isEmpty()) return DepositCalculation(0L, emptyList())
    // 按规格聚合桶数(同一规格多商品合并计桶)
    val quantityBySpec = mutableMapOf<String, Int>()
    items.forEach { item ->
      val product = productQueryApiService.findById(item.productId) ?: return@forEach
      val spec = product.specification
      if (spec.isNotBlank()) {
        quantityBySpec[spec] = (quantityBySpec[spec] ?: 0) + item.quantity
      }
    }
    if (quantityBySpec.isEmpty()) return DepositCalculation(0L, emptyList())

    val lines =
      quantityBySpec.mapNotNull { (spec, quantity) ->
        val price =
          barrelFeeRepository.findBySpecification(spec)?.takeIf { it.status == DepositPriceStatus.ENABLED }
            ?: return@mapNotNull null
        if (price.priceCents <= 0) return@mapNotNull null
        // 在租只收一次(R-MON-03):已有 HELD(在租)或 PENDING(未支付单在途)不重复收取
        val active =
          depositRecordRepository
            .findByUserIdAndSpecification(userId, spec)
            .any { it.status == DepositStatus.HELD || it.status == DepositStatus.PENDING }
        if (active) return@mapNotNull null
        DepositLine(
          specification = spec,
          unitDepositCents = price.priceCents,
          bucketCount = quantity,
          depositCents = price.priceCents * quantity,
        )
      }
    return DepositCalculation(totalCents = lines.sumOf { it.depositCents }, lines = lines)
  }

  @Transactional
  override fun createPendingDeposits(
    orderId: Long,
    userId: Long,
    calculation: DepositCalculation,
  ): List<DepositRecordModel> {
    if (calculation.lines.isEmpty()) return emptyList()
    val records =
      calculation.lines.map { line ->
        DepositRecordModel(
          orderId = orderId,
          userId = userId,
          depositAmountCents = line.depositCents,
          specification = line.specification,
          unitDepositCents = line.unitDepositCents,
          bucketCount = line.bucketCount,
          status = DepositStatus.PENDING,
        )
      }
    val saved = depositRecordRepository.saveAll(records)
    logger.info("随单建押金账(PENDING): orderId={}, lines={}, totalCents={}", orderId, saved.size, calculation.totalCents)
    return saved
  }

  @Transactional
  override fun markHeldByOrder(orderId: Long): Int {
    val pending = depositRecordRepository.findByOrderIdAndStatus(orderId, DepositStatus.PENDING)
    if (pending.isEmpty()) return 0
    val affected = depositRecordRepository.markHeldIfPending(pending.mapNotNull { it.id })
    if (affected > 0) {
      logger.info("押金实收确认(PENDING→HELD): orderId={}, records={}", orderId, affected)
    }
    return affected
  }

  @Transactional
  override fun cancelPendingByOrder(orderId: Long): Int {
    val affected = depositRecordRepository.softDeletePendingByOrderId(orderId)
    if (affected > 0) {
      logger.info("取消清押金账(软删 PENDING): orderId={}, records={}", orderId, affected)
    }
    return affected
  }

  @Transactional
  override fun registerBarrelRecovery(
    workerId: Long?,
    orderId: Long,
    items: List<EmptyBarrelRecoveryItem>,
  ): Int {
    if (items.isEmpty()) return 0
    if (items.any { it.specification.isBlank() || it.count <= 0 }) {
      throw BadRequestException("空桶回收规格不能为空且数量必须大于 0")
    }
    val normalized =
      items
        .groupBy({ it.specification }, { it.count })
        .map { (spec, counts) -> spec to counts.sum() }
    if (normalized.isEmpty()) return 0
    normalized.forEach { (spec, count) ->
      barrelRecoveryRecordRepository.save(
        BarrelRecoveryRecordModel(
          workerId = workerId ?: throw BadRequestException("缺少送水工信息,无法登记空桶回收"),
          orderId = orderId,
          specification = spec,
          bucketCount = count,
        ),
      )
    }
    logger.info("完成配送登记空桶回收: orderId={}, rows={}", orderId, normalized.size)
    return normalized.size
  }

  @Transactional
  override fun markRefundedByOrder(
    orderId: Long,
    refundedBy: Long?,
    remark: String?,
  ): List<DepositRecordModel> {
    val records = depositRecordRepository.findByOrderIdAndStatus(orderId, DepositStatus.HELD)
    if (records.isEmpty()) {
      return emptyList()
    }
    val ids = records.mapNotNull { it.id }
    val affected = depositRecordRepository.markRefundedIfHeld(ids, refundedBy, remark)
    if (affected == 0) {
      logger.info("押金退还跳过(已无 HELD 记录,幂等): orderId={}", orderId)
      return emptyList()
    }
    val now = LocalDateTime.now()
    records.forEach { record ->
      record.status = DepositStatus.REFUNDED
      record.refundedAt = now
      record.refundedBy = refundedBy
      record.refundRemark = remark
      record.updatedAt = now
    }
    logger.info("押金随订单退还: orderId={}, records={}", orderId, affected)
    bizLogger.logOperation(
      operation = "DEPOSIT_REFUNDED",
      module = "PAYMENT",
      result = "SUCCESS",
      target = orderId.toString(),
      userId = refundedBy,
      additionalData = mapOf("orderId" to orderId, "remark" to (remark ?: "")),
    )
    return records
  }
}
