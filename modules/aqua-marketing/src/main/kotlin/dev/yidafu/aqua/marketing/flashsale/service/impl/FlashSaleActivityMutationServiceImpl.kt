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

package dev.yidafu.aqua.marketing.flashsale.service.impl

import dev.yidafu.aqua.api.dto.FlashSaleActivityCreateRequest
import dev.yidafu.aqua.api.dto.FlashSaleActivityUpdateRequest
import dev.yidafu.aqua.api.service.marketing.FlashSaleActivityMutationService
import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.common.domain.model.FlashSaleActivityModel
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.marketing.flashsale.domain.exception.FlashSaleActivityNotFoundException
import dev.yidafu.aqua.marketing.flashsale.domain.exception.InvalidFlashSaleActivityException
import dev.yidafu.aqua.marketing.flashsale.domain.repository.FlashSaleActivityRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 限时特价活动变更服务实现(管理后台,F-MKT-005):
 * 特价 0.01-99999 元、低于目录价(上浮默认不允许);同商品时间窗重叠拦截;
 * 时间窗到期自动失效(引擎按时间窗过滤);配置变更不影响已生成订单(R-MKT-05)。
 */
@Service
class FlashSaleActivityMutationServiceImpl(
  private val flashSaleActivityRepository: FlashSaleActivityRepository,
  private val productQueryApiService: ProductQueryApiService,
  private val bizLogger: BizLogger,
) : FlashSaleActivityMutationService {
  private val logger = LoggerFactory.getLogger(FlashSaleActivityMutationServiceImpl::class.java)

  @Transactional
  override fun createActivity(
    adminId: Long,
    adminName: String,
    request: FlashSaleActivityCreateRequest,
  ): FlashSaleActivityModel {
    validateActivity(
      request.productId,
      request.salePriceCents,
      request.validFrom,
      request.validTo,
      excludeId = null,
    )
    val activity =
      FlashSaleActivityModel(
        name = request.name,
        productId = request.productId,
        salePriceCents = request.salePriceCents,
        validFrom = request.validFrom,
        validTo = request.validTo,
        status = PromotionActivityStatus.ACTIVE,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )
    val saved = flashSaleActivityRepository.save(activity)
    logger.info("Admin {} created flash sale activity {}: {}", adminId, saved.id, saved.name)

    // 业务日志(F-LOG-003 MARKETING 域):闪购活动创建
    bizLogger.logMarketingOperation(
      targetId = saved.id!!,
      operation = "FLASH_SALE_CREATED",
      result = "SUCCESS",
      userId = adminId,
      username = adminName,
      additionalData = mapOf("name" to saved.name, "productId" to saved.productId, "salePriceCents" to saved.salePriceCents),
    )
    return saved
  }

  @Transactional
  override fun updateActivity(
    adminId: Long,
    adminName: String,
    id: Long,
    request: FlashSaleActivityUpdateRequest,
  ): FlashSaleActivityModel {
    val activity =
      flashSaleActivityRepository
        .findById(id)
        .orElseThrow { FlashSaleActivityNotFoundException(id) }
    validateActivity(
      request.productId,
      request.salePriceCents,
      request.validFrom,
      request.validTo,
      excludeId = id,
    )
    activity.name = request.name
    activity.productId = request.productId
    activity.salePriceCents = request.salePriceCents
    activity.validFrom = request.validFrom
    activity.validTo = request.validTo
    val saved = flashSaleActivityRepository.save(activity)
    logger.info("Admin {} updated flash sale activity {}", adminId, id)

    // 业务日志(F-LOG-003 MARKETING 域):闪购活动更新
    bizLogger.logMarketingOperation(
      targetId = saved.id!!,
      operation = "FLASH_SALE_UPDATED",
      result = "SUCCESS",
      userId = adminId,
      username = adminName,
      additionalData = mapOf("name" to saved.name),
    )
    return saved
  }

  @Transactional
  override fun disableActivity(
    adminId: Long,
    adminName: String,
    id: Long,
  ): FlashSaleActivityModel {
    val activity =
      flashSaleActivityRepository
        .findById(id)
        .orElseThrow { FlashSaleActivityNotFoundException(id) }
    activity.status = PromotionActivityStatus.DISABLED
    val saved = flashSaleActivityRepository.save(activity)
    logger.info("Admin {} disabled flash sale activity {}", adminId, id)

    // 业务日志(F-LOG-003 MARKETING 域):闪购活动停用
    bizLogger.logMarketingOperation(
      targetId = saved.id!!,
      operation = "FLASH_SALE_DISABLED",
      result = "SUCCESS",
      userId = adminId,
      username = adminName,
      additionalData = mapOf("name" to saved.name),
    )
    return saved
  }

  private fun validateActivity(
    productId: Long,
    salePriceCents: Long,
    validFrom: LocalDateTime,
    validTo: LocalDateTime,
    excludeId: Long?,
  ) {
    if (!validFrom.isBefore(validTo)) {
      throw InvalidFlashSaleActivityException("时间窗不合法:开始时间必须早于结束时间")
    }
    val product =
      productQueryApiService.findById(productId)
        ?: throw InvalidFlashSaleActivityException("商品不存在: $productId")
    // 特价低于目录价(上浮为开放问题,默认不允许)
    if (salePriceCents >= product.price) {
      throw InvalidFlashSaleActivityException("特价必须低于目录价")
    }
    // 同商品时间窗重叠拦截(仅限启用活动)
    if (flashSaleActivityRepository.existsOverlap(productId, PromotionActivityStatus.ACTIVE, validFrom, validTo, excludeId)) {
      throw InvalidFlashSaleActivityException("该商品在所选时间窗内已有进行中的特价活动")
    }
  }
}
