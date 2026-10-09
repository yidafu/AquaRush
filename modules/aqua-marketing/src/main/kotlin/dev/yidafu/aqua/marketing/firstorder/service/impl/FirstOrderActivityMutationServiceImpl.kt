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

package dev.yidafu.aqua.marketing.firstorder.service.impl

import dev.yidafu.aqua.api.service.marketing.FirstOrderActivityMutationService
import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.common.domain.model.FirstOrderActivityModel
import dev.yidafu.aqua.common.domain.model.enums.FirstOrderActivityType
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus
import dev.yidafu.aqua.common.domain.model.enums.PromotionScope
import dev.yidafu.aqua.common.graphql.generated.CreateFirstOrderActivityInput
import dev.yidafu.aqua.common.graphql.generated.UpdateFirstOrderActivityInput
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.marketing.firstorder.domain.exception.FirstOrderActivityNotFoundException
import dev.yidafu.aqua.marketing.firstorder.domain.exception.InvalidFirstOrderActivityException
import dev.yidafu.aqua.marketing.firstorder.domain.repository.FirstOrderActivityRepository
import dev.yidafu.aqua.marketing.firstorder.mapper.toDomain
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 首单立减/新人专享价活动变更服务实现(管理后台,F-MKT-009):
 * FIRST_ORDER_REDUCE=满 X 减 Y 或打 N 折(互斥);NEWCOMER_PRICE=指定商品专享单价(低于目录价);
 * 新用户判定=无已完成订单(下单时动态判定,首单完成即失效 R-MKT-10)。
 */
@Service
class FirstOrderActivityMutationServiceImpl(
  private val firstOrderActivityRepository: FirstOrderActivityRepository,
  private val productQueryApiService: ProductQueryApiService,
  private val bizLogger: BizLogger,
) : FirstOrderActivityMutationService {
  private val logger = LoggerFactory.getLogger(FirstOrderActivityMutationServiceImpl::class.java)

  @Transactional
  override fun createActivity(
    adminId: Long,
    adminName: String,
    input: CreateFirstOrderActivityInput,
  ): FirstOrderActivityModel {
    // GraphQL 枚举 → 领域枚举
    val activityType = input.activityType.toDomain()
    val scope = input.scope.toDomain()
    validateActivity(
      activityType = activityType,
      thresholdCents = input.thresholdCents,
      discountAmountCents = input.discountAmountCents,
      discountRatePermille = input.discountRatePermille,
      scope = scope,
      productId = input.productId,
      newcomerPriceCents = input.newcomerPriceCents,
      validFrom = input.validFrom,
      validTo = input.validTo,
      excludeId = null,
    )
    val activity =
      FirstOrderActivityModel(
        name = input.name,
        activityType = activityType,
        thresholdCents = input.thresholdCents,
        discountAmountCents = input.discountAmountCents,
        discountRatePermille = input.discountRatePermille,
        scope = scope,
        productId = input.productId,
        newcomerPriceCents = input.newcomerPriceCents,
        validFrom = input.validFrom,
        validTo = input.validTo,
        status = PromotionActivityStatus.ACTIVE,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )
    val saved = firstOrderActivityRepository.save(activity)
    logger.info("Admin {} created first order activity {}: {}", adminId, saved.id, saved.name)

    // 业务日志(F-LOG-003 MARKETING 域):首单活动创建
    bizLogger.logMarketingOperation(
      targetId = saved.id!!,
      operation = "FIRST_ORDER_ACTIVITY_CREATED",
      result = "SUCCESS",
      userId = adminId,
      username = adminName,
      additionalData = mapOf("name" to saved.name, "activityType" to saved.activityType.name),
    )
    return saved
  }

  @Transactional
  override fun updateActivity(
    adminId: Long,
    adminName: String,
    id: Long,
    input: UpdateFirstOrderActivityInput,
  ): FirstOrderActivityModel {
    val activity =
      firstOrderActivityRepository
        .findById(id)
        .orElseThrow { FirstOrderActivityNotFoundException(id) }
    // GraphQL 枚举 → 领域枚举
    val activityType = input.activityType.toDomain()
    val scope = input.scope.toDomain()
    validateActivity(
      activityType = activityType,
      thresholdCents = input.thresholdCents,
      discountAmountCents = input.discountAmountCents,
      discountRatePermille = input.discountRatePermille,
      scope = scope,
      productId = input.productId,
      newcomerPriceCents = input.newcomerPriceCents,
      validFrom = input.validFrom,
      validTo = input.validTo,
      excludeId = id,
    )
    activity.name = input.name
    activity.activityType = activityType
    activity.thresholdCents = input.thresholdCents
    activity.discountAmountCents = input.discountAmountCents
    activity.discountRatePermille = input.discountRatePermille
    activity.scope = scope
    activity.productId = input.productId
    activity.newcomerPriceCents = input.newcomerPriceCents
    activity.validFrom = input.validFrom
    activity.validTo = input.validTo
    val saved = firstOrderActivityRepository.save(activity)
    logger.info("Admin {} updated first order activity {}", adminId, id)

    // 业务日志(F-LOG-003 MARKETING 域):首单活动更新
    bizLogger.logMarketingOperation(
      targetId = saved.id!!,
      operation = "FIRST_ORDER_ACTIVITY_UPDATED",
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
  ): FirstOrderActivityModel {
    val activity =
      firstOrderActivityRepository
        .findById(id)
        .orElseThrow { FirstOrderActivityNotFoundException(id) }
    activity.status = PromotionActivityStatus.DISABLED
    val saved = firstOrderActivityRepository.save(activity)
    logger.info("Admin {} disabled first order activity {}", adminId, id)

    // 业务日志(F-LOG-003 MARKETING 域):首单活动停用
    bizLogger.logMarketingOperation(
      targetId = saved.id!!,
      operation = "FIRST_ORDER_ACTIVITY_DISABLED",
      result = "SUCCESS",
      userId = adminId,
      username = adminName,
      additionalData = mapOf("name" to saved.name),
    )
    return saved
  }

  private fun validateActivity(
    activityType: FirstOrderActivityType,
    thresholdCents: Long?,
    discountAmountCents: Long?,
    discountRatePermille: Long?,
    scope: PromotionScope,
    productId: Long?,
    newcomerPriceCents: Long?,
    validFrom: LocalDateTime,
    validTo: LocalDateTime,
    excludeId: Long?,
  ) {
    if (!validFrom.isBefore(validTo)) {
      throw InvalidFirstOrderActivityException("时间窗不合法:开始时间必须早于结束时间")
    }
    when (activityType) {
      FirstOrderActivityType.FIRST_ORDER_REDUCE -> {
        require(thresholdCents != null && thresholdCents > 0) { "首单立减门槛金额无效" }
        val hasAmount = discountAmountCents != null && discountAmountCents > 0
        val hasRate = discountRatePermille != null
        require(hasAmount xor hasRate) { "首单立减需填写减金额或折扣率(二选一)" }
        // 首单立减适用范围仅全场/指定商品(R-MKT-10)
        require(scope == PromotionScope.ALL || scope == PromotionScope.PRODUCT) { "首单立减适用范围仅支持全场或指定商品" }
        if (scope == PromotionScope.PRODUCT) {
          require(productId != null) { "指定商品范围必须选择商品" }
        }
      }
      FirstOrderActivityType.NEWCOMER_PRICE -> {
        require(scope == PromotionScope.PRODUCT) { "新人专享价仅支持指定商品范围" }
        require(productId != null) { "新人专享价必须选择商品" }
        require(newcomerPriceCents != null && newcomerPriceCents > 0) { "新人专享价无效" }
        val product =
          productQueryApiService.findById(productId)
            ?: throw InvalidFirstOrderActivityException("商品不存在: $productId")
        // 新人价低于目录价
        if (newcomerPriceCents >= product.price) {
          throw InvalidFirstOrderActivityException("新人专享价必须低于目录价")
        }
        // 同商品新人价重叠拦截(避免配置歧义)
        if (firstOrderActivityRepository.existsOverlap(
            productId = productId,
            activityType = FirstOrderActivityType.NEWCOMER_PRICE,
            status = PromotionActivityStatus.ACTIVE,
            newValidFrom = validFrom,
            newValidTo = validTo,
            excludeId = excludeId,
          )
        ) {
          throw InvalidFirstOrderActivityException("该商品在所选时间窗内已有进行中的新人专享价")
        }
      }
    }
  }
}
