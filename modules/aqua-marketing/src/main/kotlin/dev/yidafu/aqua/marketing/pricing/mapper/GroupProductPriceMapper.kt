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

package dev.yidafu.aqua.marketing.pricing.mapper

import dev.yidafu.aqua.common.domain.model.GroupProductPriceModel
import dev.yidafu.aqua.common.graphql.generated.GroupProductPriceVo

/**
 * 分组商品专属价映射器:域实体 → GraphQL 类型(商品名称/规格/目录价由商品服务补充,PRD 10-marketing.md §F-MKT-002)
 */
fun GroupProductPriceModel.toGraphQL(
  productName: String?,
  productSpecification: String?,
  catalogPriceCents: Long,
): GroupProductPriceVo =
  GroupProductPriceVo(
    id = id ?: 0L,
    customerGroupId = customerGroupId,
    productId = productId,
    priceCents = priceCents,
    productName = productName,
    productSpecification = productSpecification,
    catalogPriceCents = catalogPriceCents,
  )
