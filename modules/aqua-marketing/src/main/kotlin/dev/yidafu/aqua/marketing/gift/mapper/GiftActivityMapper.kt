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

package dev.yidafu.aqua.marketing.gift.mapper

import dev.yidafu.aqua.api.dto.GiftActivityCreateRequest
import dev.yidafu.aqua.api.dto.GiftActivityUpdateRequest
import dev.yidafu.aqua.common.domain.model.GiftActivityModel
import dev.yidafu.aqua.common.graphql.generated.CreateGiftActivityInput
import dev.yidafu.aqua.common.graphql.generated.GiftActivity
import dev.yidafu.aqua.common.graphql.generated.GiftActivityType
import dev.yidafu.aqua.common.graphql.generated.PromotionActivityDisplayStatus
import dev.yidafu.aqua.common.graphql.generated.PromotionActivityStatus
import dev.yidafu.aqua.common.graphql.generated.PromotionScope
import dev.yidafu.aqua.common.graphql.generated.UpdateGiftActivityInput
import tech.mappie.api.EnumMappie
import tech.mappie.api.ObjectMappie
import dev.yidafu.aqua.common.domain.model.enums.GiftActivityType as DomainGiftActivityType
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus as DomainPromotionActivityStatus
import dev.yidafu.aqua.common.domain.model.enums.PromotionScope as DomainPromotionScope

/**
 * 买赠/满赠活动映射器:域实体 → GraphQL 类型(展示态/赠品名由 Resolver 计算传入,PRD 10-marketing.md §F-MKT-008)
 */
object GiftActivityMapper {
  fun toGraphQL(
    model: GiftActivityModel,
    currentStatus: PromotionActivityDisplayStatus,
    giftProductName: String?,
  ): GiftActivity =
    GiftActivity(
      id = model.id ?: 0L,
      name = model.name,
      giftType = GiftActivityTypeMapper.map(model.giftType),
      thresholdCents = model.thresholdCents,
      thresholdQuantity = model.thresholdQuantity,
      scope = GiftActivityScopeMapper.map(model.scope),
      productId = model.productId,
      customerGroupId = model.customerGroupId,
      giftProductId = model.giftProductId,
      giftProductName = giftProductName,
      giftQuantity = model.giftQuantity,
      validFrom = model.validFrom,
      validTo = model.validTo,
      status = GiftActivityStatusMapper.map(model.status),
      currentStatus = currentStatus,
      createdAt = model.createdAt,
      updatedAt = model.updatedAt,
    )
}

object GiftActivityTypeMapper : EnumMappie<DomainGiftActivityType, GiftActivityType>()

object GiftActivityScopeMapper : EnumMappie<DomainPromotionScope, PromotionScope>()

object GiftActivityStatusMapper : EnumMappie<DomainPromotionActivityStatus, PromotionActivityStatus>()

object CreateGiftActivityInputMapper : ObjectMappie<CreateGiftActivityInput, GiftActivityCreateRequest>() {
  override fun map(from: CreateGiftActivityInput): GiftActivityCreateRequest =
    mapping {
      to::giftType fromExpression { GiftActivityTypeInputMapper.map(from.giftType) }
      to::scope fromExpression { GiftActivityScopeInputMapper.map(from.scope) }
    }
}

object UpdateGiftActivityInputMapper : ObjectMappie<UpdateGiftActivityInput, GiftActivityUpdateRequest>() {
  override fun map(from: UpdateGiftActivityInput): GiftActivityUpdateRequest =
    mapping {
      to::giftType fromExpression { GiftActivityTypeInputMapper.map(from.giftType) }
      to::scope fromExpression { GiftActivityScopeInputMapper.map(from.scope) }
    }
}

object GiftActivityTypeInputMapper : EnumMappie<GiftActivityType, DomainGiftActivityType>()

object GiftActivityScopeInputMapper : EnumMappie<PromotionScope, DomainPromotionScope>()
