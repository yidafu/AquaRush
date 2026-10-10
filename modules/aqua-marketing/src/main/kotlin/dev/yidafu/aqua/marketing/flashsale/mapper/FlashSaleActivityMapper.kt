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

package dev.yidafu.aqua.marketing.flashsale.mapper

import dev.yidafu.aqua.common.domain.model.FlashSaleActivityModel
import dev.yidafu.aqua.common.graphql.generated.FlashSaleActivityVo
import dev.yidafu.aqua.common.graphql.generated.PromotionActivityDisplayStatus
import dev.yidafu.aqua.common.graphql.generated.PromotionActivityStatus
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus as DomainPromotionActivityStatus

/**
 * 限时特价活动映射器:域实体 → GraphQL 类型(展示态/商品名/目录价由 Resolver 计算传入,PRD 10-marketing.md §F-MKT-005)
 */
fun FlashSaleActivityModel.toGraphQL(
  currentStatus: PromotionActivityDisplayStatus,
  productName: String?,
  catalogPriceCents: Long?,
): FlashSaleActivityVo =
  FlashSaleActivityVo(
    id = id ?: 0L,
    name = name,
    productId = productId,
    productName = productName,
    catalogPriceCents = catalogPriceCents,
    salePriceCents = salePriceCents,
    validFrom = validFrom,
    validTo = validTo,
    status = status.toGraphQL(),
    currentStatus = currentStatus,
    createdAt = createdAt,
    updatedAt = updatedAt,
  )

/**
 * 活动状态:领域枚举 → GraphQL 枚举同名 valueOf 互转
 */
fun DomainPromotionActivityStatus.toGraphQL(): PromotionActivityStatus = PromotionActivityStatus.valueOf(name)
