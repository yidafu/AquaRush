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

import dev.yidafu.aqua.api.dto.UpsertDepositPriceRequest
import dev.yidafu.aqua.api.service.deposit.DepositPriceMutationService
import dev.yidafu.aqua.common.domain.model.DepositPriceModel
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.marketing.barrel.domain.repository.DepositPriceRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 水桶押金价目变更服务实现(R-MON-03:押金按规格独立配置,与商品解耦;后台押金管理维护)。
 * upsert 合一:Id 空=新增,规格唯一冲突拦截;押金 0..9999900 分(GraphQL @min/@max 双保险)。
 * 表沿用 barrel_fees(B2 语义回退,列名 price_cents 即押金价——01 §3.4 已注明)。
 */
@Service
class DepositPriceMutationServiceImpl(
  private val depositPriceRepository: DepositPriceRepository,
  private val bizLogger: BizLogger,
) : DepositPriceMutationService {
  private val logger = LoggerFactory.getLogger(DepositPriceMutationServiceImpl::class.java)

  @Transactional
  override fun upsert(request: UpsertDepositPriceRequest): DepositPriceModel {
    if (request.specification.isBlank()) {
      throw BadRequestException("规格不能为空")
    }
    if (request.depositCents < 0 || request.depositCents > 9_999_900L) {
      throw BadRequestException("押金须在 0~99999 元之间")
    }
    val existing = depositPriceRepository.findBySpecification(request.specification)
    if (existing != null && existing.id != request.id) {
      throw BadRequestException("规格已存在: ${request.specification}")
    }
    val now = LocalDateTime.now()
    val target =
      request.id
        ?.let { depositPriceRepository.findById(it).orElseThrow { BadRequestException("押金价目不存在: $it") } }
        ?: DepositPriceModel(createdAt = now, updatedAt = now)
    target.specification = request.specification
    target.priceCents = request.depositCents
    target.status = request.status
    target.sortOrder = request.sortOrder
    target.updatedAt = now
    val saved = depositPriceRepository.save(target)
    val created = request.id == null
    logger.info(
      if (created) "押金价目新增: id={}, specification={}, depositCents={}" else "押金价目更新: id={}, specification={}, depositCents={}",
      saved.id,
      saved.specification,
      saved.priceCents,
    )
    bizLogger.logOperation(
      operation = if (created) "DEPOSIT_PRICE_CREATED" else "DEPOSIT_PRICE_UPDATED",
      module = "PRODUCT",
      result = "SUCCESS",
      target = saved.id?.toString() ?: "deposit-price",
      additionalData =
        mapOf(
          "specification" to saved.specification,
          "depositCents" to saved.priceCents,
          "status" to saved.status.name,
        ),
    )
    return saved
  }
}
