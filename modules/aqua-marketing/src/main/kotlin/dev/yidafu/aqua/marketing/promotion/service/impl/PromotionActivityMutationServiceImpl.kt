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

package dev.yidafu.aqua.marketing.promotion.service.impl

import dev.yidafu.aqua.api.dto.PromotionActivityCreateRequest
import dev.yidafu.aqua.api.dto.PromotionActivityUpdateRequest
import dev.yidafu.aqua.api.dto.PromotionTierItem
import dev.yidafu.aqua.api.service.marketing.PromotionActivityMutationService
import dev.yidafu.aqua.common.domain.model.PromotionActivityModel
import dev.yidafu.aqua.common.domain.model.PromotionActivityTierModel
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityType
import dev.yidafu.aqua.common.domain.model.enums.PromotionScope
import dev.yidafu.aqua.marketing.promotion.domain.exception.InvalidTierException
import dev.yidafu.aqua.marketing.promotion.domain.exception.PromotionActivityNotFoundException
import dev.yidafu.aqua.marketing.promotion.domain.repository.PromotionActivityRepository
import dev.yidafu.aqua.marketing.promotion.domain.repository.PromotionActivityTierRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 满减/折扣活动变更服务实现(管理后台,F-MKT-004):
 * 档位校验(递增且互斥,按类型字段);活动内取最高已达标档,跨活动取优惠最大者不叠加(R-MKT-04)。
 */
@Service
class PromotionActivityMutationServiceImpl(
  private val promotionActivityRepository: PromotionActivityRepository,
  private val promotionActivityTierRepository: PromotionActivityTierRepository,
) : PromotionActivityMutationService {
  private val logger = LoggerFactory.getLogger(PromotionActivityMutationServiceImpl::class.java)

  @Transactional
  override fun createActivity(
    adminId: Long,
    adminName: String,
    request: PromotionActivityCreateRequest,
  ): PromotionActivityModel {
    validateActivity(
      request.activityType,
      request.scope,
      request.productId,
      request.customerGroupId,
      request.validFrom,
      request.validTo,
      request.tiers,
    )
    val activity =
      PromotionActivityModel(
        name = request.name,
        activityType = request.activityType,
        scope = request.scope,
        productId = request.productId,
        customerGroupId = request.customerGroupId,
        validFrom = request.validFrom,
        validTo = request.validTo,
        status = dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus.ACTIVE,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )
    val saved = promotionActivityRepository.save(activity)
    saveTiers(saved.id!!, request.tiers)
    logger.info("Admin {} created promotion activity {}: {}", adminId, saved.id, saved.name)
    return saved
  }

  @Transactional
  override fun updateActivity(
    adminId: Long,
    adminName: String,
    id: Long,
    request: PromotionActivityUpdateRequest,
  ): PromotionActivityModel {
    val activity = promotionActivityRepository.findById(id).orElseThrow { PromotionActivityNotFoundException(id) }
    validateActivity(
      request.activityType,
      request.scope,
      request.productId,
      request.customerGroupId,
      request.validFrom,
      request.validTo,
      request.tiers,
    )
    activity.name = request.name
    activity.activityType = request.activityType
    activity.scope = request.scope
    activity.productId = request.productId
    activity.customerGroupId = request.customerGroupId
    activity.validFrom = request.validFrom
    activity.validTo = request.validTo
    val saved = promotionActivityRepository.save(activity)
    // 全量替换档位(旧档位软删)
    promotionActivityTierRepository.findByActivityId(id).forEach { promotionActivityTierRepository.delete(it) }
    saveTiers(id, request.tiers)
    logger.info("Admin {} updated promotion activity {}", adminId, id)
    return saved
  }

  @Transactional
  override fun disableActivity(
    adminId: Long,
    adminName: String,
    id: Long,
  ): PromotionActivityModel {
    val activity = promotionActivityRepository.findById(id).orElseThrow { PromotionActivityNotFoundException(id) }
    activity.status = dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus.DISABLED
    val saved = promotionActivityRepository.save(activity)
    logger.info("Admin {} disabled promotion activity {}", adminId, id)
    return saved
  }

  private fun saveTiers(
    activityId: Long,
    tiers: List<PromotionTierItem>,
  ) {
    tiers.forEachIndexed { index, tier ->
      promotionActivityTierRepository.save(
        PromotionActivityTierModel(
          activityId = activityId,
          thresholdCents = tier.thresholdCents,
          discountAmountCents = tier.discountAmountCents,
          discountRatePermille = tier.discountRatePermille,
          sort = index,
          createdAt = LocalDateTime.now(),
          updatedAt = LocalDateTime.now(),
        ),
      )
    }
  }

  private fun validateActivity(
    activityType: PromotionActivityType,
    scope: PromotionScope,
    productId: Long?,
    customerGroupId: Long?,
    validFrom: LocalDateTime,
    validTo: LocalDateTime,
    tiers: List<PromotionTierItem>,
  ) {
    if (!validFrom.isBefore(validTo)) {
      throw InvalidTierException("时间窗不合法:开始时间必须早于结束时间")
    }
    if (scope == PromotionScope.PRODUCT) {
      require(productId != null) { "指定商品范围必须选择商品" }
    }
    if (scope == PromotionScope.GROUP) {
      require(customerGroupId != null) { "指定分组范围必须选择客户分组" }
    }
    // 档位金额递增且互斥(R-MKT-04)
    var previousThreshold = -1L
    tiers.forEach { tier ->
      require(tier.thresholdCents > previousThreshold) { "档位金额需递增" }
      previousThreshold = tier.thresholdCents
      when (activityType) {
        PromotionActivityType.FULL_REDUCE -> require(tier.discountAmountCents != null) { "满减活动档位必须填写满减金额" }
        PromotionActivityType.FULL_DISCOUNT -> require(tier.discountRatePermille != null) { "满折活动档位必须填写折扣率" }
      }
    }
  }
}
