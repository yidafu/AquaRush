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

package dev.yidafu.aqua.marketing.promotion.mapper

import dev.yidafu.aqua.common.domain.model.PromotionActivityModel
import dev.yidafu.aqua.common.domain.model.PromotionActivityTierModel
import dev.yidafu.aqua.common.graphql.generated.PromotionActivityDisplayStatus
import dev.yidafu.aqua.common.graphql.generated.PromotionActivityStatus
import dev.yidafu.aqua.common.graphql.generated.PromotionActivityTierVo
import dev.yidafu.aqua.common.graphql.generated.PromotionActivityType
import dev.yidafu.aqua.common.graphql.generated.PromotionActivityVo
import dev.yidafu.aqua.common.graphql.generated.PromotionScope
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus as DomainPromotionActivityStatus
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityType as DomainPromotionActivityType
import dev.yidafu.aqua.common.domain.model.enums.PromotionScope as DomainPromotionScope

/**
 * 满减/折扣活动映射器:域实体 → GraphQL 类型(档位与展示态由服务/Resolver 计算后传入,PRD 10-marketing.md §F-MKT-004)
 */
fun PromotionActivityModel.toGraphQL(
  tiers: List<PromotionActivityTierModel>,
  currentStatus: PromotionActivityDisplayStatus,
): PromotionActivityVo =
  PromotionActivityVo(
    id = id ?: 0L,
    name = name,
    activityType = activityType.toGraphQL(),
    scope = scope.toGraphQL(),
    productId = productId,
    customerGroupId = customerGroupId,
    validFrom = validFrom,
    validTo = validTo,
    status = status.toGraphQL(),
    currentStatus = currentStatus,
    tiers = tiers.map { it.toGraphQL() },
    createdAt = createdAt,
    updatedAt = updatedAt,
  )

/**
 * 活动档位映射器:域实体 → GraphQL 类型
 */
fun PromotionActivityTierModel.toGraphQL(): PromotionActivityTierVo =
  PromotionActivityTierVo(
    id = id ?: 0L,
    thresholdCents = thresholdCents,
    discountAmountCents = discountAmountCents,
    discountRatePermille = discountRatePermille,
  )

/**
 * 活动类型:领域枚举 ↔ GraphQL 枚举同名枚举 when 穷举互转
 */
fun DomainPromotionActivityType.toGraphQL(): PromotionActivityType =
  when (this) {
    DomainPromotionActivityType.FULL_REDUCE -> PromotionActivityType.FULL_REDUCE
    DomainPromotionActivityType.FULL_DISCOUNT -> PromotionActivityType.FULL_DISCOUNT
  }

fun PromotionActivityType.toDomain(): DomainPromotionActivityType =
  when (this) {
    PromotionActivityType.FULL_DISCOUNT -> DomainPromotionActivityType.FULL_DISCOUNT
    PromotionActivityType.FULL_REDUCE -> DomainPromotionActivityType.FULL_REDUCE
  }

/**
 * 活动适用范围:领域枚举 ↔ GraphQL 枚举同名枚举 when 穷举互转
 */
fun DomainPromotionScope.toGraphQL(): PromotionScope =
  when (this) {
    DomainPromotionScope.ALL -> PromotionScope.ALL
    DomainPromotionScope.PRODUCT -> PromotionScope.PRODUCT
    DomainPromotionScope.GROUP -> PromotionScope.GROUP
  }

fun PromotionScope.toDomain(): DomainPromotionScope =
  when (this) {
    PromotionScope.ALL -> DomainPromotionScope.ALL
    PromotionScope.GROUP -> DomainPromotionScope.GROUP
    PromotionScope.PRODUCT -> DomainPromotionScope.PRODUCT
  }

/**
 * 活动状态:领域枚举 ↔ GraphQL 枚举同名枚举 when 穷举互转
 */
fun DomainPromotionActivityStatus.toGraphQL(): PromotionActivityStatus =
  when (this) {
    DomainPromotionActivityStatus.ACTIVE -> PromotionActivityStatus.ACTIVE
    DomainPromotionActivityStatus.DISABLED -> PromotionActivityStatus.DISABLED
  }
