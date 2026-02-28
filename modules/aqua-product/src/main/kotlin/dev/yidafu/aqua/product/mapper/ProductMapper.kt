/*
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

import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.graphql.generated.Product
import tech.mappie.api.ObjectMappie

/**
 * Mapper for converting ProductModel domain entity to GraphQL Product type
 */
object ProductMapper : ObjectMappie<ProductModel, Product>() {
  override fun map(from: ProductModel): Product = mapping {
    to::id fromProperty from::id
    to::name fromProperty from::name
    to::subtitle fromProperty from::subtitle
    to::coverImageUrl fromProperty from::coverImageUrl
    to::price fromProperty from::price
    to::originalPrice fromProperty from::originalPrice
    to::depositPrice fromProperty from::depositPrice
    to::specification fromProperty from::specification
    to::waterSource fromProperty from::waterSource
    to::mineralContent fromProperty from::mineralContent
    to::stock fromProperty from::stock
    to::salesVolume fromProperty from::salesVolume
    to::status fromExpression {
      dev.yidafu.aqua.common.graphql.generated.ProductStatus.valueOf(from.status.name)
    }
    to::sortOrder fromProperty from::sortOrder
    to::detailContent fromProperty from::detailContent
    to::certificateImages fromProperty from::certificateImages
    to::deliverySettings fromProperty from::deliverySettings
    to::imageGallery fromProperty from::imageGallery
    to::isDeleted fromValue false
    to::tags fromProperty from::tags
    to::createdAt fromProperty from::createdAt
    to::updatedAt fromProperty from::updatedAt
  }
}
