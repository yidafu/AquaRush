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

package dev.yidafu.aqua.admin.marketing.resolvers

import dev.yidafu.aqua.api.service.marketing.CouponMutationService
import dev.yidafu.aqua.api.service.marketing.CouponQueryService
import dev.yidafu.aqua.common.graphql.generated.CouponTemplate
import dev.yidafu.aqua.common.graphql.generated.CreateCouponTemplateInput
import dev.yidafu.aqua.common.graphql.generated.IssueCouponsInput
import dev.yidafu.aqua.common.graphql.generated.UpdateCouponTemplateInput
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.marketing.coupon.mapper.CouponTemplateMapper
import dev.yidafu.aqua.marketing.coupon.mapper.CreateCouponTemplateInputMapper
import dev.yidafu.aqua.marketing.coupon.mapper.IssueCouponsInputMapper
import dev.yidafu.aqua.marketing.coupon.mapper.UpdateCouponTemplateInputMapper
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

/**
 * 优惠券模板变更 Resolver(管理后台,PERMISSION_MKT_WRITE)
 * 「建券 → 发放/领取 → 核销 → 退回/过期」闭环的管理侧(PRD 10-marketing.md §F-MKT-003)。
 */
@Controller
class AdminCouponMutationResolver(
  private val couponMutationService: CouponMutationService,
  private val couponQueryService: CouponQueryService,
) {
  private val logger = LoggerFactory.getLogger(AdminCouponMutationResolver::class.java)

  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_MKT_WRITE')")
  fun createCouponTemplate(
    @Argument @Valid input: CreateCouponTemplateInput,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): CouponTemplate {
    logger.info("Admin {} creating coupon template: {}", principal.username, input.name)
    val template =
      couponMutationService.createTemplate(
        adminId = principal.id,
        adminName = principal.username,
        request = CreateCouponTemplateInputMapper.map(input),
      )
    return CouponTemplateMapper.toGraphQL(template, receivedCount = 0, usedCount = 0)
  }

  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_MKT_WRITE')")
  fun updateCouponTemplate(
    @Argument id: Long,
    @Argument @Valid input: UpdateCouponTemplateInput,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): CouponTemplate {
    logger.info("Admin {} updating coupon template {}", principal.username, id)
    val template =
      couponMutationService.updateTemplate(
        adminId = principal.id,
        adminName = principal.username,
        id = id,
        request = UpdateCouponTemplateInputMapper.map(input),
      )
    return toCouponTemplate(template)
  }

  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_MKT_WRITE')")
  fun disableCouponTemplate(
    @Argument id: Long,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): CouponTemplate {
    logger.info("Admin {} disabling coupon template {}", principal.username, id)
    val template =
      couponMutationService.disableTemplate(principal.id, principal.username, id)
    return toCouponTemplate(template)
  }

  /**
   * 定向发放优惠券,返回实际发放张数
   */
  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_MKT_WRITE')")
  fun issueCoupons(
    @Argument @Valid input: IssueCouponsInput,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): Int {
    logger.info("Admin {} issuing coupons for template {}", principal.username, input.couponTemplateId)
    return couponMutationService.issueCoupons(
      adminId = principal.id,
      adminName = principal.username,
      request = IssueCouponsInputMapper.map(input),
    )
  }

  private fun toCouponTemplate(template: dev.yidafu.aqua.common.domain.model.CouponTemplateModel): CouponTemplate {
    val templateId = template.id ?: 0L
    return CouponTemplateMapper.toGraphQL(
      model = template,
      receivedCount = couponQueryService.receivedCount(templateId).toInt(),
      usedCount = couponQueryService.usedCount(templateId).toInt(),
    )
  }
}
