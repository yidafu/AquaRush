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

import dev.yidafu.aqua.api.service.deposit.DepositMutationApiService
import dev.yidafu.aqua.common.domain.model.DepositRecordModel
import dev.yidafu.aqua.common.domain.model.enums.DepositStatus
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.marketing.barrel.domain.repository.DepositRecordRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 历史桶押金变更服务实现(G06 押金模块,2026-08 水桶费改造后语义废弃):
 * 仅保留「按订单退还全部有效押金」供历史订单(orders.deposit_amount_cents>0)取消退款兼容;
 * 新购桶(水桶费)走 barrel_purchases(BarrelPurchaseMutationServiceImpl)。
 */
@Service
class DepositMutationApiServiceImpl(
  private val depositRecordRepository: DepositRecordRepository,
  private val bizLogger: BizLogger,
) : DepositMutationApiService {
  private val logger = LoggerFactory.getLogger(DepositMutationApiServiceImpl::class.java)

  @Transactional
  override fun markRefundedByOrder(
    orderId: Long,
    refundedBy: Long?,
    remark: String?,
  ): List<DepositRecordModel> {
    val records = depositRecordRepository.findByOrderIdAndStatus(orderId, DepositStatus.COLLECTED)
    if (records.isEmpty()) {
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
    val saved = depositRecordRepository.saveAll(records)
    logger.info("押金随订单退还(历史兼容): orderId={}, records={}", orderId, saved.size)
    bizLogger.logOperation(
      operation = "DEPOSIT_REFUNDED",
      module = "PAYMENT",
      result = "SUCCESS",
      target = orderId.toString(),
      userId = refundedBy,
      additionalData = mapOf("orderId" to orderId, "remark" to (remark ?: "")),
    )
    return saved
  }
}
