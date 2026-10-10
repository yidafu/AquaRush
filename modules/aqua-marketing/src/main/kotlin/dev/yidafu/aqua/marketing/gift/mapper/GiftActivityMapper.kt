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

import dev.yidafu.aqua.common.domain.model.GiftActivityModel
import dev.yidafu.aqua.common.graphql.generated.GiftActivityType
import dev.yidafu.aqua.common.graphql.generated.GiftActivityVo
import dev.yidafu.aqua.common.graphql.generated.PromotionActivityDisplayStatus
import dev.yidafu.aqua.common.graphql.generated.PromotionActivityStatus
import dev.yidafu.aqua.common.graphql.generated.PromotionScope
import dev.yidafu.aqua.common.domain.model.enums.GiftActivityType as DomainGiftActivityType
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus as DomainPromotionActivityStatus
import dev.yidafu.aqua.common.domain.model.enums.PromotionScope as DomainPromotionScope

/**
 * 买赠/满赠活动映射器:域实体 → GraphQL 类型(展示态/赠品名由 Resolver 计算传入,PRD 10-marketing.md §F-MKT-008)
 */
fun GiftActivityModel.toGraphQL(
  currentStatus: PromotionActivityDisplayStatus,
  giftProductName: String?,
): GiftActivityVo =
  GiftActivityVo(
    id = id ?: 0L,
    name = name,
    giftType = giftType.toGraphQL(),
    thresholdCents = thresholdCents,
    thresholdQuantity = thresholdQuantity,
    scope = scope.toGraphQL(),
    productId = productId,
    customerGroupId = customerGroupId,
    giftProductId = giftProductId,
    giftProductName = giftProductName,
    giftQuantity = giftQuantity,
    validFrom = validFrom,
    validTo = validTo,
    status = status.toGraphQL(),
    currentStatus = currentStatus,
    createdAt = createdAt,
    updatedAt = updatedAt,
  )

/**
 * 赠品类型:领域枚举 ↔ GraphQL 枚举同名 valueOf 互转
 */
fun DomainGiftActivityType.toGraphQL(): GiftActivityType = GiftActivityType.valueOf(name)

fun GiftActivityType.toDomain(): DomainGiftActivityType = DomainGiftActivityType.valueOf(name)

/**
 * 活动适用范围:领域枚举 ↔ GraphQL 枚举同名 valueOf 互转
 */
fun DomainPromotionScope.toGraphQL(): PromotionScope = PromotionScope.valueOf(name)

fun PromotionScope.toDomain(): DomainPromotionScope = DomainPromotionScope.valueOf(name)

/**
 * 活动状态:领域枚举 ↔ GraphQL 枚举同名 valueOf 互转
 */
fun DomainPromotionActivityStatus.toGraphQL(): PromotionActivityStatus = PromotionActivityStatus.valueOf(name)
