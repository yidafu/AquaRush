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

import dev.yidafu.aqua.api.dto.PromotionActivityCreateRequest
import dev.yidafu.aqua.api.dto.PromotionActivityUpdateRequest
import dev.yidafu.aqua.api.dto.PromotionTierItem
import dev.yidafu.aqua.common.domain.model.PromotionActivityModel
import dev.yidafu.aqua.common.domain.model.PromotionActivityTierModel
import dev.yidafu.aqua.common.graphql.generated.CreatePromotionActivityInput
import dev.yidafu.aqua.common.graphql.generated.PromotionActivity
import dev.yidafu.aqua.common.graphql.generated.PromotionActivityDisplayStatus
import dev.yidafu.aqua.common.graphql.generated.PromotionActivityStatus
import dev.yidafu.aqua.common.graphql.generated.PromotionActivityTier
import dev.yidafu.aqua.common.graphql.generated.PromotionActivityType
import dev.yidafu.aqua.common.graphql.generated.PromotionScope
import dev.yidafu.aqua.common.graphql.generated.PromotionTierInput
import dev.yidafu.aqua.common.graphql.generated.UpdatePromotionActivityInput
import tech.mappie.api.EnumMappie
import tech.mappie.api.ObjectMappie
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus as DomainPromotionActivityStatus
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityType as DomainPromotionActivityType
import dev.yidafu.aqua.common.domain.model.enums.PromotionScope as DomainPromotionScope

/**
 * 满减/折扣活动映射器:域实体 → GraphQL 类型(档位与展示态由服务/Resolver 计算后传入,PRD 10-marketing.md §F-MKT-004)
 */
object PromotionActivityMapper {
  fun toGraphQL(
    model: PromotionActivityModel,
    tiers: List<PromotionActivityTierModel>,
    currentStatus: PromotionActivityDisplayStatus,
  ): PromotionActivity =
    PromotionActivity(
      id = model.id ?: 0L,
      name = model.name,
      activityType = PromotionActivityTypeMapper.map(model.activityType),
      scope = PromotionScopeMapper.map(model.scope),
      productId = model.productId,
      customerGroupId = model.customerGroupId,
      validFrom = model.validFrom,
      validTo = model.validTo,
      status = PromotionActivityStatusMapper.map(model.status),
      currentStatus = currentStatus,
      tiers = tiers.map { PromotionTierMapper.map(it) },
      createdAt = model.createdAt,
      updatedAt = model.updatedAt,
    )
}

/**
 * 活动档位映射器:域实体 → GraphQL 类型
 */
object PromotionTierMapper : ObjectMappie<PromotionActivityTierModel, PromotionActivityTier>() {
  override fun map(from: PromotionActivityTierModel): PromotionActivityTier =
    mapping {
      to::id fromValue (from.id ?: 0L)
    }
}

object PromotionActivityTypeMapper : EnumMappie<DomainPromotionActivityType, PromotionActivityType>()

object PromotionScopeMapper : EnumMappie<DomainPromotionScope, PromotionScope>()

object PromotionActivityStatusMapper : EnumMappie<DomainPromotionActivityStatus, PromotionActivityStatus>()

object PromotionActivityTypeInputMapper : EnumMappie<PromotionActivityType, DomainPromotionActivityType>()

object PromotionScopeInputMapper : EnumMappie<PromotionScope, DomainPromotionScope>()

/**
 * 活动档位输入映射器:GraphQL 输入 → 服务请求
 */
object PromotionTierInputMapper : ObjectMappie<PromotionTierInput, PromotionTierItem>()

/**
 * 新建活动输入映射器
 */
object CreatePromotionActivityInputMapper : ObjectMappie<CreatePromotionActivityInput, PromotionActivityCreateRequest>() {
  override fun map(from: CreatePromotionActivityInput): PromotionActivityCreateRequest =
    mapping {
      to::activityType fromExpression { PromotionActivityTypeInputMapper.map(from.activityType) }
      to::scope fromExpression { PromotionScopeInputMapper.map(from.scope) }
      to::tiers fromExpression { from.tiers.map { PromotionTierInputMapper.map(it) } }
    }
}

/**
 * 编辑活动输入映射器(全量覆盖,含档位)
 */
object UpdatePromotionActivityInputMapper : ObjectMappie<UpdatePromotionActivityInput, PromotionActivityUpdateRequest>() {
  override fun map(from: UpdatePromotionActivityInput): PromotionActivityUpdateRequest =
    mapping {
      to::activityType fromExpression { PromotionActivityTypeInputMapper.map(from.activityType) }
      to::scope fromExpression { PromotionScopeInputMapper.map(from.scope) }
      to::tiers fromExpression { from.tiers.map { PromotionTierInputMapper.map(it) } }
    }
}
