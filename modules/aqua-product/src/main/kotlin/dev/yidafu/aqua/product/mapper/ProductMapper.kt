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

package dev.yidafu.aqua.product.mapper

import dev.yidafu.aqua.api.dto.ProductSearchRequest
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.enums.ProductModelStatus
import dev.yidafu.aqua.common.graphql.generated.PriceSource
import dev.yidafu.aqua.common.graphql.generated.Product
import dev.yidafu.aqua.common.graphql.generated.ProductSearchInput
import dev.yidafu.aqua.common.graphql.generated.ProductStatus
import tech.mappie.api.EnumMappie
import tech.mappie.api.ObjectMappie

/**
 * Mapper for converting ProductModel domain entity to GraphQL Product type
 */
object ProductMapper : ObjectMappie<ProductModel, Product>() {
  override fun map(from: ProductModel): Product =
    mapping {
      to::id fromValue (from.id ?: 0L)
      to::priceCents fromProperty from::price
      to::originalPriceCents fromProperty from::originalPrice
      to::depositPriceCents fromProperty from::depositPrice
      // Fields with same name and type - auto-mapped by Mappie
      to::status fromExpression {
        ProductModelStatusMapper.map(from.status)
      }
      // Map coverImageUrl to image field
      to::coverImageUrl fromProperty from::coverImageUrl
      // 管理侧无用户分组上下文:有效价=目录价,未命中熟客价(R-MKT-01)
      to::effectivePriceCents fromProperty from::price
      to::hasGroupPrice fromValue false
      // 管理侧成交单价来源=目录价(R-MKT-02,用户端按登录用户解析)
      to::priceSource fromValue PriceSource.CATALOG_PRICE
    }
}

object ProductQueryMapper : ObjectMappie<ProductSearchInput, ProductSearchRequest>() {
  override fun map(from: ProductSearchInput): ProductSearchRequest =
    mapping {
      to::sortBy fromValue from.sortBy.toString()
      to::status fromExpression { from.status?.let { ProductStatusMapper.map(it) } }
    }
}

object ProductModelStatusMapper : EnumMappie<ProductModelStatus, ProductStatus>()

object ProductStatusMapper : EnumMappie<ProductStatus, ProductModelStatus>()
