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

import dev.yidafu.aqua.common.graphql.generated.Product
import dev.yidafu.aqua.common.graphql.generated.ProductStatus
import dev.yidafu.aqua.product.domain.model.ProductModel
import tech.mappie.api.EnumMappie
import tech.mappie.api.ObjectMappie

/**
 * Mapper for converting ProductModel domain entity to GraphQL ProductModel type
 */
object ProductMapper : ObjectMappie<ProductModel, Product>() {
    override fun map(from: ProductModel) = mapping {
        to::id fromProperty from::id
        to::name fromProperty from::name
        to::subtitle fromProperty from::subtitle
        to::price fromProperty from::price
        to::originalPrice fromProperty from::originalPrice
        to::depositPrice fromProperty from::depositPrice
        to::coverImageUrl fromProperty from::coverImageUrl
        to::specification fromProperty from::specification
        to::waterSource fromProperty from::waterSource
        to::phValue fromProperty from::phValue
        to::mineralContent fromProperty from::mineralContent
        to::stock fromProperty from::stock
        to::salesVolume fromProperty from::salesVolume
        to::status fromProperty from::status
        to::sortOrder fromProperty from::sortOrder
        to::detailContent fromProperty from::detailContent
        to::isDeleted fromProperty from::isDeleted
        to::createdAt fromProperty from::createdAt
        to::updatedAt fromProperty from::updatedAt

        // Note: JSON fields are handled by JPA converters and will be returned as strings by GraphQL
        // The actual type conversion happens in the resolvers if needed
    }
}


