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

import dev.yidafu.aqua.api.dto.CreateBarrelFeeRequest
import dev.yidafu.aqua.api.dto.UpdateBarrelFeeRequest
import dev.yidafu.aqua.api.service.barrel.BarrelFeeMutationService
import dev.yidafu.aqua.common.domain.model.BarrelFeeModel
import dev.yidafu.aqua.common.domain.model.enums.BarrelFeeStatus
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.marketing.barrel.domain.repository.BarrelFeeRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 水桶费价目变更服务实现(G06:独立价目表,按规格,与商品解耦;后台维护)
 */
@Service
class BarrelFeeMutationServiceImpl(
  private val barrelFeeRepository: BarrelFeeRepository,
  private val bizLogger: BizLogger,
) : BarrelFeeMutationService {
  private val logger = LoggerFactory.getLogger(BarrelFeeMutationServiceImpl::class.java)

  @Transactional
  override fun create(request: CreateBarrelFeeRequest): BarrelFeeModel {
    if (barrelFeeRepository.findBySpecification(request.specification) != null) {
      throw BadRequestException("规格已存在: ${request.specification}")
    }
    val now = LocalDateTime.now()
    val fee =
      barrelFeeRepository.save(
        BarrelFeeModel(
          specification = request.specification,
          priceCents = request.priceCents,
          status = BarrelFeeStatus.ENABLED,
          sortOrder = request.sortOrder,
          createdAt = now,
          updatedAt = now,
        ),
      )
    logger.info("水桶费价目新增: id={}, specification={}, priceCents={}", fee.id, fee.specification, fee.priceCents)
    bizLogger.logOperation(
      operation = "BARREL_FEE_CREATED",
      module = "PAYMENT",
      result = "SUCCESS",
      target = fee.id?.toString() ?: "barrel-fee",
      additionalData = mapOf("specification" to fee.specification, "priceCents" to fee.priceCents),
    )
    return fee
  }

  @Transactional
  override fun update(
    id: Long,
    request: UpdateBarrelFeeRequest,
  ): BarrelFeeModel {
    val fee =
      barrelFeeRepository.findById(id).orElseThrow { BadRequestException("水桶费价目不存在: $id") }
    request.specification?.let {
      barrelFeeRepository
        .findBySpecification(it)
        ?.takeIf { existing -> existing.id != id }
        ?.let { throw BadRequestException("规格已存在: $it") }
      fee.specification = it
    }
    request.priceCents?.let { fee.priceCents = it }
    request.sortOrder?.let { fee.sortOrder = it }
    val saved = barrelFeeRepository.save(fee)
    logger.info("水桶费价目更新: id={}, specification={}, priceCents={}", id, saved.specification, saved.priceCents)
    return saved
  }

  @Transactional
  override fun disable(id: Long): BarrelFeeModel {
    val fee =
      barrelFeeRepository.findById(id).orElseThrow { BadRequestException("水桶费价目不存在: $id") }
    if (fee.status == BarrelFeeStatus.DISABLED) {
      return fee // 幂等
    }
    fee.status = BarrelFeeStatus.DISABLED
    val saved = barrelFeeRepository.save(fee)
    logger.info("水桶费价目停用: id={}, specification={}", id, saved.specification)
    bizLogger.logOperation(
      operation = "BARREL_FEE_DISABLED",
      module = "PAYMENT",
      result = "SUCCESS",
      target = id.toString(),
      additionalData = mapOf("specification" to saved.specification),
    )
    return saved
  }
}
