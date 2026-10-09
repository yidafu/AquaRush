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

package dev.yidafu.aqua.marketing.firstorder.mapper

import dev.yidafu.aqua.common.domain.model.FirstOrderActivityModel
import dev.yidafu.aqua.common.graphql.generated.FirstOrderActivity
import dev.yidafu.aqua.common.graphql.generated.FirstOrderActivityType
import dev.yidafu.aqua.common.graphql.generated.PromotionActivityDisplayStatus
import dev.yidafu.aqua.common.graphql.generated.PromotionActivityStatus
import dev.yidafu.aqua.common.graphql.generated.PromotionScope
import dev.yidafu.aqua.common.domain.model.enums.FirstOrderActivityType as DomainFirstOrderActivityType
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus as DomainPromotionActivityStatus
import dev.yidafu.aqua.common.domain.model.enums.PromotionScope as DomainPromotionScope

/**
 * 首单立减/新人专享价活动映射器:域实体 → GraphQL 类型(展示态/商品名由 Resolver 计算传入,PRD 10-marketing.md §F-MKT-009)
 */
fun FirstOrderActivityModel.toGraphQL(
  currentStatus: PromotionActivityDisplayStatus,
  productName: String?,
): FirstOrderActivity =
  FirstOrderActivity(
    id = id ?: 0L,
    name = name,
    activityType = activityType.toGraphQL(),
    thresholdCents = thresholdCents,
    discountAmountCents = discountAmountCents,
    discountRatePermille = discountRatePermille,
    scope = scope.toGraphQL(),
    productId = productId,
    productName = productName,
    newcomerPriceCents = newcomerPriceCents,
    validFrom = validFrom,
    validTo = validTo,
    status = status.toGraphQL(),
    currentStatus = currentStatus,
    createdAt = createdAt,
    updatedAt = updatedAt,
  )

/**
 * 活动类型:领域枚举 ↔ GraphQL 枚举同名 valueOf 互转
 */
fun DomainFirstOrderActivityType.toGraphQL(): FirstOrderActivityType = FirstOrderActivityType.valueOf(name)

fun FirstOrderActivityType.toDomain(): DomainFirstOrderActivityType = DomainFirstOrderActivityType.valueOf(name)

/**
 * 活动适用范围:领域枚举 ↔ GraphQL 枚举同名 valueOf 互转
 */
fun DomainPromotionScope.toGraphQL(): PromotionScope = PromotionScope.valueOf(name)

fun PromotionScope.toDomain(): DomainPromotionScope = DomainPromotionScope.valueOf(name)

/**
 * 活动状态:领域枚举 ↔ GraphQL 枚举同名 valueOf 互转
 */
fun DomainPromotionActivityStatus.toGraphQL(): PromotionActivityStatus = PromotionActivityStatus.valueOf(name)
