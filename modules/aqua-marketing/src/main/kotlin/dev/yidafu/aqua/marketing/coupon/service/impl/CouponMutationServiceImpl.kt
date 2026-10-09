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

package dev.yidafu.aqua.marketing.coupon.service.impl

import dev.yidafu.aqua.api.dto.IssueTargetType
import dev.yidafu.aqua.api.service.marketing.CouponMutationService
import dev.yidafu.aqua.api.service.user.UserQueryApiService
import dev.yidafu.aqua.common.domain.model.CouponTemplateModel
import dev.yidafu.aqua.common.domain.model.enums.CouponScope
import dev.yidafu.aqua.common.domain.model.enums.CouponTemplateStatus
import dev.yidafu.aqua.common.domain.model.enums.CouponType
import dev.yidafu.aqua.common.domain.model.enums.ValidityType
import dev.yidafu.aqua.common.graphql.generated.CreateCouponTemplateInput
import dev.yidafu.aqua.common.graphql.generated.IssueCouponsInput
import dev.yidafu.aqua.common.graphql.generated.UpdateCouponTemplateInput
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.marketing.coupon.domain.exception.CouponNotClaimableException
import dev.yidafu.aqua.marketing.coupon.domain.exception.CouponTemplateNotFoundException
import dev.yidafu.aqua.marketing.coupon.domain.repository.CouponTemplateRepository
import dev.yidafu.aqua.marketing.coupon.domain.repository.UserCouponRepository
import dev.yidafu.aqua.marketing.coupon.mapper.toDomain
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 优惠券模板变更服务实现(管理后台,F-MKT-003):
 * 建券 → 定向发放;模板参数校验按类型(现金券/折扣券);发放目标按用户或分组全体成员。
 */
@Service
class CouponMutationServiceImpl(
  private val couponTemplateRepository: CouponTemplateRepository,
  private val userCouponRepository: UserCouponRepository,
  private val userQueryApiService: UserQueryApiService,
  private val bizLogger: BizLogger,
) : CouponMutationService {
  private val logger = LoggerFactory.getLogger(CouponMutationServiceImpl::class.java)

  @Transactional
  override fun createTemplate(
    adminId: Long,
    adminName: String,
    input: CreateCouponTemplateInput,
  ): CouponTemplateModel {
    // GraphQL 枚举 → 领域枚举
    val couponType = input.couponType.toDomain()
    val scope = input.scope.toDomain()
    val validityType = input.validityType.toDomain()
    val issuanceType = input.issuanceType.toDomain()
    validateTemplateFields(couponType, input.discountAmountCents, input.discountRatePermille, scope, input.productId)
    validateValidity(validityType, input.validFrom, input.validTo, input.validDays)
    val template =
      CouponTemplateModel(
        name = input.name,
        couponType = couponType,
        thresholdCents = input.thresholdCents,
        discountAmountCents = input.discountAmountCents,
        discountRatePermille = input.discountRatePermille,
        scope = scope,
        productId = input.productId,
        validityType = validityType,
        validFrom = input.validFrom,
        validTo = input.validTo,
        validDays = input.validDays,
        issuanceType = issuanceType,
        perUserLimit = input.perUserLimit,
        status = CouponTemplateStatus.ACTIVE,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )
    val saved = couponTemplateRepository.save(template)
    logger.info("Admin {} created coupon template {}: {}", adminId, saved.id, saved.name)

    // 业务日志(F-LOG-003 MARKETING 域):券模板创建
    bizLogger.logMarketingOperation(
      targetId = saved.id!!,
      operation = "COUPON_TEMPLATE_CREATED",
      result = "SUCCESS",
      userId = adminId,
      username = adminName,
      additionalData = mapOf("name" to saved.name, "couponType" to saved.couponType.name),
    )
    return saved
  }

  @Transactional
  override fun updateTemplate(
    adminId: Long,
    adminName: String,
    id: Long,
    input: UpdateCouponTemplateInput,
  ): CouponTemplateModel {
    val template = couponTemplateRepository.findById(id).orElseThrow { CouponTemplateNotFoundException(id) }
    // GraphQL 枚举 → 领域枚举
    val couponType = input.couponType.toDomain()
    val scope = input.scope.toDomain()
    val validityType = input.validityType.toDomain()
    validateTemplateFields(couponType, input.discountAmountCents, input.discountRatePermille, scope, input.productId)
    validateValidity(validityType, input.validFrom, input.validTo, input.validDays)
    template.name = input.name
    template.couponType = couponType
    template.thresholdCents = input.thresholdCents
    template.discountAmountCents = input.discountAmountCents
    template.discountRatePermille = input.discountRatePermille
    template.scope = scope
    template.productId = input.productId
    template.validityType = validityType
    template.validFrom = input.validFrom
    template.validTo = input.validTo
    template.validDays = input.validDays
    template.perUserLimit = input.perUserLimit
    val saved = couponTemplateRepository.save(template)
    logger.info("Admin {} updated coupon template {}", adminId, id)

    // 业务日志(F-LOG-003 MARKETING 域):券模板更新
    bizLogger.logMarketingOperation(
      targetId = saved.id!!,
      operation = "COUPON_TEMPLATE_UPDATED",
      result = "SUCCESS",
      userId = adminId,
      username = adminName,
      additionalData = mapOf("name" to saved.name),
    )
    return saved
  }

  @Transactional
  override fun disableTemplate(
    adminId: Long,
    adminName: String,
    id: Long,
  ): CouponTemplateModel {
    val template = couponTemplateRepository.findById(id).orElseThrow { CouponTemplateNotFoundException(id) }
    template.status = CouponTemplateStatus.DISABLED
    val saved = couponTemplateRepository.save(template)
    logger.info("Admin {} disabled coupon template {}", adminId, id)

    // 业务日志(F-LOG-003 MARKETING 域):券模板停用
    bizLogger.logMarketingOperation(
      targetId = saved.id!!,
      operation = "COUPON_TEMPLATE_DISABLED",
      result = "SUCCESS",
      userId = adminId,
      username = adminName,
      additionalData = mapOf("name" to saved.name),
    )
    return saved
  }

  @Transactional
  override fun issueCoupons(
    adminId: Long,
    adminName: String,
    input: IssueCouponsInput,
  ): Int {
    val template =
      couponTemplateRepository.findById(input.couponTemplateId).orElseThrow {
        CouponTemplateNotFoundException(input.couponTemplateId)
      }
    if (template.status != CouponTemplateStatus.ACTIVE) {
      throw CouponNotClaimableException("券模板已停用,不可发放")
    }
    // GraphQL 枚举 → 领域枚举;userIds 缺省回填空列表(原 Request 中转行为)
    val targetType = input.targetType.toDomain()
    val userIds = input.userIds ?: emptyList()
    val targetUserIds =
      when (targetType) {
        IssueTargetType.USER -> userIds
        IssueTargetType.GROUP -> {
          val groupId = input.customerGroupId ?: throw IllegalArgumentException("分组发放必须指定客户分组")
          userQueryApiService.findByCustomerGroupId(groupId).mapNotNull { it.id }
        }
      }
    val now = LocalDateTime.now()
    var issued = 0
    targetUserIds.forEach { userId ->
      // 达到限领数跳过(定向发放不强制突破限领)
      val claimed = userCouponRepository.countByUserIdAndCouponTemplateId(userId, template.id!!)
      if (claimed >= template.perUserLimit) {
        return@forEach
      }
      userCouponRepository.save(CouponSnapshotSupport.buildUserCoupon(userId, template, now))
      issued++
    }
    logger.info("Admin {} issued {} coupons from template {} (target={})", adminId, issued, template.id, targetType)

    // 业务日志(F-LOG-003 MARKETING 域):优惠券发放
    bizLogger.logMarketingOperation(
      targetId = template.id!!,
      operation = "COUPON_ISSUED",
      result = "SUCCESS",
      userId = adminId,
      username = adminName,
      additionalData = mapOf("issuedCount" to issued, "targetType" to targetType.name, "templateName" to template.name),
    )
    return issued
  }

  private fun validateTemplateFields(
    couponType: CouponType,
    discountAmountCents: Long?,
    discountRatePermille: Long?,
    scope: CouponScope,
    productId: Long?,
  ) {
    when (couponType) {
      CouponType.CASH, CouponType.NO_THRESHOLD_CASH ->
        require(discountAmountCents != null) { "券面额无效" }
      CouponType.DISCOUNT ->
        require(discountRatePermille != null) { "折扣率无效" }
    }
    if (scope == CouponScope.PRODUCT) {
      require(productId != null) { "指定商品券必须选择商品" }
    }
  }

  private fun validateValidity(
    validityType: ValidityType,
    validFrom: LocalDateTime?,
    validTo: LocalDateTime?,
    validDays: Int?,
  ) {
    when (validityType) {
      ValidityType.FIXED_RANGE -> {
        require(validFrom != null && validTo != null) { "有效期不合法:固定区间必须填开始与结束时间" }
        require(validFrom.isBefore(validTo)) { "有效期不合法:开始时间必须早于结束时间" }
      }
      ValidityType.DAYS_AFTER_CLAIM -> require(validDays != null && validDays > 0) { "有效期不合法:领取后N天必须为正数" }
      else -> Unit
    }
  }
}
