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

import dev.yidafu.aqua.api.dto.FirstOrderActivityCreateRequest
import dev.yidafu.aqua.api.dto.FirstOrderActivityUpdateRequest
import dev.yidafu.aqua.common.domain.model.FirstOrderActivityModel
import dev.yidafu.aqua.common.graphql.generated.CreateFirstOrderActivityInput
import dev.yidafu.aqua.common.graphql.generated.FirstOrderActivity
import dev.yidafu.aqua.common.graphql.generated.FirstOrderActivityType
import dev.yidafu.aqua.common.graphql.generated.PromotionActivityDisplayStatus
import dev.yidafu.aqua.common.graphql.generated.PromotionActivityStatus
import dev.yidafu.aqua.common.graphql.generated.PromotionScope
import dev.yidafu.aqua.common.graphql.generated.UpdateFirstOrderActivityInput
import tech.mappie.api.EnumMappie
import tech.mappie.api.ObjectMappie
import dev.yidafu.aqua.common.domain.model.enums.FirstOrderActivityType as DomainFirstOrderActivityType
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus as DomainPromotionActivityStatus
import dev.yidafu.aqua.common.domain.model.enums.PromotionScope as DomainPromotionScope

/**
 * 首单立减/新人专享价活动映射器:域实体 → GraphQL 类型(展示态/商品名由 Resolver 计算传入,PRD 10-marketing.md §F-MKT-009)
 */
object FirstOrderActivityMapper {
  fun toGraphQL(
    model: FirstOrderActivityModel,
    currentStatus: PromotionActivityDisplayStatus,
    productName: String?,
  ): FirstOrderActivity =
    FirstOrderActivity(
      id = model.id ?: 0L,
      name = model.name,
      activityType = FirstOrderActivityTypeMapper.map(model.activityType),
      thresholdCents = model.thresholdCents,
      discountAmountCents = model.discountAmountCents,
      discountRatePermille = model.discountRatePermille,
      scope = FirstOrderActivityScopeMapper.map(model.scope),
      productId = model.productId,
      productName = productName,
      newcomerPriceCents = model.newcomerPriceCents,
      validFrom = model.validFrom,
      validTo = model.validTo,
      status = FirstOrderActivityStatusMapper.map(model.status),
      currentStatus = currentStatus,
      createdAt = model.createdAt,
      updatedAt = model.updatedAt,
    )
}

object FirstOrderActivityTypeMapper : EnumMappie<DomainFirstOrderActivityType, FirstOrderActivityType>()

object FirstOrderActivityScopeMapper : EnumMappie<DomainPromotionScope, PromotionScope>()

object FirstOrderActivityStatusMapper : EnumMappie<DomainPromotionActivityStatus, PromotionActivityStatus>()

object CreateFirstOrderActivityInputMapper : ObjectMappie<CreateFirstOrderActivityInput, FirstOrderActivityCreateRequest>() {
  override fun map(from: CreateFirstOrderActivityInput): FirstOrderActivityCreateRequest =
    mapping {
      to::activityType fromExpression { FirstOrderActivityTypeInputMapper.map(from.activityType) }
      to::scope fromExpression { FirstOrderActivityScopeInputMapper.map(from.scope) }
    }
}

object UpdateFirstOrderActivityInputMapper : ObjectMappie<UpdateFirstOrderActivityInput, FirstOrderActivityUpdateRequest>() {
  override fun map(from: UpdateFirstOrderActivityInput): FirstOrderActivityUpdateRequest =
    mapping {
      to::activityType fromExpression { FirstOrderActivityTypeInputMapper.map(from.activityType) }
      to::scope fromExpression { FirstOrderActivityScopeInputMapper.map(from.scope) }
    }
}

object FirstOrderActivityTypeInputMapper : EnumMappie<FirstOrderActivityType, DomainFirstOrderActivityType>()

object FirstOrderActivityScopeInputMapper : EnumMappie<PromotionScope, DomainPromotionScope>()
