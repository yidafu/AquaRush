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

import dev.yidafu.aqua.common.domain.model.CouponTemplateModel
import dev.yidafu.aqua.common.graphql.generated.CouponIssuanceType
import dev.yidafu.aqua.common.graphql.generated.CouponScope
import dev.yidafu.aqua.common.graphql.generated.CouponTemplateAdminVo
import dev.yidafu.aqua.common.graphql.generated.CouponTemplateStatus
import dev.yidafu.aqua.common.graphql.generated.CouponType
import dev.yidafu.aqua.common.graphql.generated.IssueTargetType
import dev.yidafu.aqua.common.graphql.generated.ValidityType
import dev.yidafu.aqua.api.dto.IssueTargetType as DomainIssueTargetType
import dev.yidafu.aqua.common.domain.model.enums.CouponIssuanceType as DomainCouponIssuanceType
import dev.yidafu.aqua.common.domain.model.enums.CouponScope as DomainCouponScope
import dev.yidafu.aqua.common.domain.model.enums.CouponTemplateStatus as DomainCouponTemplateStatus
import dev.yidafu.aqua.common.domain.model.enums.CouponType as DomainCouponType
import dev.yidafu.aqua.common.domain.model.enums.ValidityType as DomainValidityType

/**
 * 优惠券模板映射器:域实体 → GraphQL 类型(领取/核销数由服务统计后传入,PRD 10-marketing.md §F-MKT-003)
 */
fun CouponTemplateModel.toGraphQL(
  receivedCount: Int,
  usedCount: Int,
): CouponTemplateAdminVo =
  CouponTemplateAdminVo(
    id = id ?: 0L,
    name = name,
    couponType = couponType.toGraphQL(),
    thresholdCents = thresholdCents,
    discountAmountCents = discountAmountCents,
    discountRatePermille = discountRatePermille,
    scope = scope.toGraphQL(),
    productId = productId,
    validityType = validityType.toGraphQL(),
    validFrom = validFrom,
    validTo = validTo,
    validDays = validDays,
    issuanceType = issuanceType.toGraphQL(),
    perUserLimit = perUserLimit,
    status = status.toGraphQL(),
    receivedCount = receivedCount,
    usedCount = usedCount,
    createdAt = createdAt,
    updatedAt = updatedAt,
  )

/**
 * 优惠券类型:领域枚举 ↔ GraphQL 枚举同名 valueOf 互转
 */
fun DomainCouponType.toGraphQL(): CouponType = CouponType.valueOf(name)

fun CouponType.toDomain(): DomainCouponType = DomainCouponType.valueOf(name)

/**
 * 优惠券适用范围:领域枚举 ↔ GraphQL 枚举同名 valueOf 互转
 */
fun DomainCouponScope.toGraphQL(): CouponScope = CouponScope.valueOf(name)

fun CouponScope.toDomain(): DomainCouponScope = DomainCouponScope.valueOf(name)

/**
 * 有效期类型:领域枚举 ↔ GraphQL 枚举同名 valueOf 互转
 */
fun DomainValidityType.toGraphQL(): ValidityType = ValidityType.valueOf(name)

fun ValidityType.toDomain(): DomainValidityType = DomainValidityType.valueOf(name)

/**
 * 发放方式:领域枚举 ↔ GraphQL 枚举同名 valueOf 互转
 */
fun DomainCouponIssuanceType.toGraphQL(): CouponIssuanceType = CouponIssuanceType.valueOf(name)

fun CouponIssuanceType.toDomain(): DomainCouponIssuanceType = DomainCouponIssuanceType.valueOf(name)

/**
 * 模板状态:领域枚举 ↔ GraphQL 枚举同名 valueOf 互转
 */
fun DomainCouponTemplateStatus.toGraphQL(): CouponTemplateStatus = CouponTemplateStatus.valueOf(name)

fun CouponTemplateStatus.toDomain(): DomainCouponTemplateStatus = DomainCouponTemplateStatus.valueOf(name)

/**
 * 定向发放目标类型:GraphQL 枚举 → 领域枚举同名 valueOf 互转
 */
fun IssueTargetType.toDomain(): DomainIssueTargetType = DomainIssueTargetType.valueOf(name)
