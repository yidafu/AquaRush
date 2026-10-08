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

import dev.yidafu.aqua.api.dto.FlashSaleActivityCreateRequest
import dev.yidafu.aqua.api.dto.FlashSaleActivityUpdateRequest
import dev.yidafu.aqua.common.domain.model.FlashSaleActivityModel
import dev.yidafu.aqua.common.graphql.generated.CreateFlashSaleActivityInput
import dev.yidafu.aqua.common.graphql.generated.FlashSaleActivity
import dev.yidafu.aqua.common.graphql.generated.PromotionActivityDisplayStatus
import dev.yidafu.aqua.common.graphql.generated.PromotionActivityStatus
import dev.yidafu.aqua.common.graphql.generated.UpdateFlashSaleActivityInput
import tech.mappie.api.EnumMappie
import tech.mappie.api.ObjectMappie
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus as DomainPromotionActivityStatus

/**
 * 限时特价活动映射器:域实体 → GraphQL 类型(展示态/商品名/目录价由 Resolver 计算传入,PRD 10-marketing.md §F-MKT-005)
 */
object FlashSaleActivityMapper {
  fun toGraphQL(
    model: FlashSaleActivityModel,
    currentStatus: PromotionActivityDisplayStatus,
    productName: String?,
    catalogPriceCents: Long?,
  ): FlashSaleActivity =
    FlashSaleActivity(
      id = model.id ?: 0L,
      name = model.name,
      productId = model.productId,
      productName = productName,
      catalogPriceCents = catalogPriceCents,
      salePriceCents = model.salePriceCents,
      validFrom = model.validFrom,
      validTo = model.validTo,
      status = FlashSaleActivityStatusMapper.map(model.status),
      currentStatus = currentStatus,
      createdAt = model.createdAt,
      updatedAt = model.updatedAt,
    )
}

object FlashSaleActivityStatusMapper : EnumMappie<DomainPromotionActivityStatus, PromotionActivityStatus>()

object CreateFlashSaleActivityInputMapper : ObjectMappie<CreateFlashSaleActivityInput, FlashSaleActivityCreateRequest>()

object UpdateFlashSaleActivityInputMapper : ObjectMappie<UpdateFlashSaleActivityInput, FlashSaleActivityUpdateRequest>()
