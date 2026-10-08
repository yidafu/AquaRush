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

package dev.yidafu.aqua.marketing.coupon.mapper

import dev.yidafu.aqua.api.dto.CouponTemplateCreateRequest
import dev.yidafu.aqua.api.dto.CouponTemplateUpdateRequest
import dev.yidafu.aqua.api.dto.IssueCouponsRequest
import dev.yidafu.aqua.common.domain.model.CouponTemplateModel
import dev.yidafu.aqua.common.graphql.generated.CouponIssuanceType
import dev.yidafu.aqua.common.graphql.generated.CouponScope
import dev.yidafu.aqua.common.graphql.generated.CouponTemplate
import dev.yidafu.aqua.common.graphql.generated.CouponTemplateStatus
import dev.yidafu.aqua.common.graphql.generated.CouponType
import dev.yidafu.aqua.common.graphql.generated.CreateCouponTemplateInput
import dev.yidafu.aqua.common.graphql.generated.IssueCouponsInput
import dev.yidafu.aqua.common.graphql.generated.IssueTargetType
import dev.yidafu.aqua.common.graphql.generated.UpdateCouponTemplateInput
import dev.yidafu.aqua.common.graphql.generated.ValidityType
import tech.mappie.api.EnumMappie
import tech.mappie.api.ObjectMappie
import dev.yidafu.aqua.api.dto.IssueTargetType as DomainIssueTargetType
import dev.yidafu.aqua.common.domain.model.enums.CouponIssuanceType as DomainCouponIssuanceType
import dev.yidafu.aqua.common.domain.model.enums.CouponScope as DomainCouponScope
import dev.yidafu.aqua.common.domain.model.enums.CouponTemplateStatus as DomainCouponTemplateStatus
import dev.yidafu.aqua.common.domain.model.enums.CouponType as DomainCouponType
import dev.yidafu.aqua.common.domain.model.enums.ValidityType as DomainValidityType

/**
 * 优惠券模板映射器:域实体 → GraphQL 类型(领取/核销数由服务统计后传入,PRD 10-marketing.md §F-MKT-003)
 */
object CouponTemplateMapper {
  fun toGraphQL(
    model: CouponTemplateModel,
    receivedCount: Int,
    usedCount: Int,
  ): CouponTemplate =
    CouponTemplate(
      id = model.id ?: 0L,
      name = model.name,
      couponType = CouponTypeMapper.map(model.couponType),
      thresholdCents = model.thresholdCents,
      discountAmountCents = model.discountAmountCents,
      discountRatePermille = model.discountRatePermille,
      scope = CouponScopeMapper.map(model.scope),
      productId = model.productId,
      validityType = ValidityTypeMapper.map(model.validityType),
      validFrom = model.validFrom,
      validTo = model.validTo,
      validDays = model.validDays,
      issuanceType = CouponIssuanceTypeMapper.map(model.issuanceType),
      perUserLimit = model.perUserLimit,
      status = CouponTemplateStatusMapper.map(model.status),
      receivedCount = receivedCount,
      usedCount = usedCount,
      createdAt = model.createdAt,
      updatedAt = model.updatedAt,
    )
}

object CouponTypeMapper : EnumMappie<DomainCouponType, CouponType>()

object CouponScopeMapper : EnumMappie<DomainCouponScope, CouponScope>()

object ValidityTypeMapper : EnumMappie<DomainValidityType, ValidityType>()

object CouponIssuanceTypeMapper : EnumMappie<DomainCouponIssuanceType, CouponIssuanceType>()

object CouponTemplateStatusMapper : EnumMappie<DomainCouponTemplateStatus, CouponTemplateStatus>()

object IssueTargetTypeMapper : EnumMappie<DomainIssueTargetType, IssueTargetType>()

object CouponTypeInputMapper : EnumMappie<CouponType, DomainCouponType>()

object CouponScopeInputMapper : EnumMappie<CouponScope, DomainCouponScope>()

object ValidityTypeInputMapper : EnumMappie<ValidityType, DomainValidityType>()

object CouponIssuanceTypeInputMapper : EnumMappie<CouponIssuanceType, DomainCouponIssuanceType>()

object CouponTemplateStatusInputMapper : EnumMappie<CouponTemplateStatus, DomainCouponTemplateStatus>()

object IssueTargetTypeInputMapper : EnumMappie<IssueTargetType, DomainIssueTargetType>()

/**
 * 新建优惠券模板输入映射器:GraphQL 输入 → 服务请求
 */
object CreateCouponTemplateInputMapper : ObjectMappie<CreateCouponTemplateInput, CouponTemplateCreateRequest>() {
  override fun map(from: CreateCouponTemplateInput): CouponTemplateCreateRequest =
    mapping {
      to::couponType fromExpression { CouponTypeInputMapper.map(from.couponType) }
      to::scope fromExpression { CouponScopeInputMapper.map(from.scope) }
      to::validityType fromExpression { ValidityTypeInputMapper.map(from.validityType) }
      to::issuanceType fromExpression { CouponIssuanceTypeInputMapper.map(from.issuanceType) }
    }
}

/**
 * 编辑优惠券模板输入映射器(发放方式不可改)
 */
object UpdateCouponTemplateInputMapper : ObjectMappie<UpdateCouponTemplateInput, CouponTemplateUpdateRequest>() {
  override fun map(from: UpdateCouponTemplateInput): CouponTemplateUpdateRequest =
    mapping {
      to::couponType fromExpression { CouponTypeInputMapper.map(from.couponType) }
      to::scope fromExpression { CouponScopeInputMapper.map(from.scope) }
      to::validityType fromExpression { ValidityTypeInputMapper.map(from.validityType) }
    }
}

/**
 * 定向发放优惠券输入映射器:GraphQL 输入 → 服务请求
 */
object IssueCouponsInputMapper : ObjectMappie<IssueCouponsInput, IssueCouponsRequest>() {
  override fun map(from: IssueCouponsInput): IssueCouponsRequest =
    mapping {
      to::targetType fromExpression { IssueTargetTypeInputMapper.map(from.targetType) }
      to::userIds fromExpression { from.userIds?.toList() ?: emptyList() }
    }
}
