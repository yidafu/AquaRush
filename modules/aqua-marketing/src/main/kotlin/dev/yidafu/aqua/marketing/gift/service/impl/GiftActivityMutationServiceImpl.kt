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

package dev.yidafu.aqua.marketing.gift.service.impl

import dev.yidafu.aqua.api.dto.GiftActivityCreateRequest
import dev.yidafu.aqua.api.dto.GiftActivityUpdateRequest
import dev.yidafu.aqua.api.service.marketing.GiftActivityMutationService
import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.common.domain.model.GiftActivityModel
import dev.yidafu.aqua.common.domain.model.enums.GiftActivityType
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus
import dev.yidafu.aqua.common.domain.model.enums.PromotionScope
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.marketing.gift.domain.exception.GiftActivityNotFoundException
import dev.yidafu.aqua.marketing.gift.domain.exception.InvalidGiftActivityException
import dev.yidafu.aqua.marketing.gift.domain.repository.GiftActivityRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 买赠/满赠活动变更服务实现(管理后台,F-MKT-008):
 * 满赠=现金部分商品小计满 X 元;买赠=指定商品满 X 桶;赠品必选且数量 > 0;
 * 时间窗到期自动失效;配置变更不影响已生成订单(R-MKT-09)。
 */
@Service
class GiftActivityMutationServiceImpl(
  private val giftActivityRepository: GiftActivityRepository,
  private val productQueryApiService: ProductQueryApiService,
  private val bizLogger: BizLogger,
) : GiftActivityMutationService {
  private val logger = LoggerFactory.getLogger(GiftActivityMutationServiceImpl::class.java)

  @Transactional
  override fun createActivity(
    adminId: Long,
    adminName: String,
    request: GiftActivityCreateRequest,
  ): GiftActivityModel {
    validateActivity(
      request.giftType,
      request.thresholdCents,
      request.thresholdQuantity,
      request.scope,
      request.productId,
      request.customerGroupId,
      request.giftProductId,
      request.giftQuantity,
      request.validFrom,
      request.validTo,
    )
    val activity =
      GiftActivityModel(
        name = request.name,
        giftType = request.giftType,
        thresholdCents = request.thresholdCents,
        thresholdQuantity = request.thresholdQuantity,
        scope = request.scope,
        productId = request.productId,
        customerGroupId = request.customerGroupId,
        giftProductId = request.giftProductId,
        giftQuantity = request.giftQuantity,
        validFrom = request.validFrom,
        validTo = request.validTo,
        status = PromotionActivityStatus.ACTIVE,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )
    val saved = giftActivityRepository.save(activity)
    logger.info("Admin {} created gift activity {}: {}", adminId, saved.id, saved.name)

    // 业务日志(F-LOG-003 MARKETING 域):买赠活动创建
    bizLogger.logMarketingOperation(
      targetId = saved.id!!,
      operation = "GIFT_ACTIVITY_CREATED",
      result = "SUCCESS",
      userId = adminId,
      username = adminName,
      additionalData = mapOf("name" to saved.name, "giftType" to saved.giftType.name),
    )
    return saved
  }

  @Transactional
  override fun updateActivity(
    adminId: Long,
    adminName: String,
    id: Long,
    request: GiftActivityUpdateRequest,
  ): GiftActivityModel {
    val activity =
      giftActivityRepository
        .findById(id)
        .orElseThrow { GiftActivityNotFoundException(id) }
    validateActivity(
      request.giftType,
      request.thresholdCents,
      request.thresholdQuantity,
      request.scope,
      request.productId,
      request.customerGroupId,
      request.giftProductId,
      request.giftQuantity,
      request.validFrom,
      request.validTo,
    )
    activity.name = request.name
    activity.giftType = request.giftType
    activity.thresholdCents = request.thresholdCents
    activity.thresholdQuantity = request.thresholdQuantity
    activity.scope = request.scope
    activity.productId = request.productId
    activity.customerGroupId = request.customerGroupId
    activity.giftProductId = request.giftProductId
    activity.giftQuantity = request.giftQuantity
    activity.validFrom = request.validFrom
    activity.validTo = request.validTo
    val saved = giftActivityRepository.save(activity)
    logger.info("Admin {} updated gift activity {}", adminId, id)

    // 业务日志(F-LOG-003 MARKETING 域):买赠活动更新
    bizLogger.logMarketingOperation(
      targetId = saved.id!!,
      operation = "GIFT_ACTIVITY_UPDATED",
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
  ): GiftActivityModel {
    val activity =
      giftActivityRepository
        .findById(id)
        .orElseThrow { GiftActivityNotFoundException(id) }
    activity.status = PromotionActivityStatus.DISABLED
    val saved = giftActivityRepository.save(activity)
    logger.info("Admin {} disabled gift activity {}", adminId, id)

    // 业务日志(F-LOG-003 MARKETING 域):买赠活动停用
    bizLogger.logMarketingOperation(
      targetId = saved.id!!,
      operation = "GIFT_ACTIVITY_DISABLED",
      result = "SUCCESS",
      userId = adminId,
      username = adminName,
      additionalData = mapOf("name" to saved.name),
    )
    return saved
  }

  private fun validateActivity(
    giftType: GiftActivityType,
    thresholdCents: Long?,
    thresholdQuantity: Int?,
    scope: PromotionScope,
    productId: Long?,
    customerGroupId: Long?,
    giftProductId: Long,
    giftQuantity: Int,
    validFrom: LocalDateTime,
    validTo: LocalDateTime,
  ) {
    if (!validFrom.isBefore(validTo)) {
      throw InvalidGiftActivityException("时间窗不合法:开始时间必须早于结束时间")
    }
    when (giftType) {
      GiftActivityType.FULL_GIFT ->
        require(thresholdCents != null && thresholdCents > 0) { "满赠门槛金额无效" }
      GiftActivityType.BUY_GIFT ->
        require(thresholdQuantity != null && thresholdQuantity > 0) { "买赠门槛数量无效" }
    }
    if (scope == PromotionScope.PRODUCT) {
      require(productId != null) { "指定商品范围必须选择商品" }
    }
    if (scope == PromotionScope.GROUP) {
      require(customerGroupId != null) { "指定分组范围必须选择客户分组" }
    }
    productQueryApiService.findById(giftProductId)
      ?: throw InvalidGiftActivityException("赠品商品不存在: $giftProductId")
    require(giftQuantity > 0) { "赠品数量无效" }
  }
}
