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

import dev.yidafu.aqua.api.dto.ProductOptionCreateRequest
import dev.yidafu.aqua.api.dto.ProductOptionUpdateRequest
import dev.yidafu.aqua.common.domain.model.ProductOptionModel
import dev.yidafu.aqua.common.graphql.generated.CreateProductOptionInput
import dev.yidafu.aqua.common.graphql.generated.ProductOption
import dev.yidafu.aqua.common.graphql.generated.ProductOptionType
import dev.yidafu.aqua.common.graphql.generated.UpdateProductOptionInput
import tech.mappie.api.EnumMappie
import tech.mappie.api.ObjectMappie
import dev.yidafu.aqua.common.domain.model.enums.ProductOptionType as DomainProductOptionType

/**
 * 商品属性选项字典 Mapper(F-PRO-009):
 * 实体 → GraphQL ProductOption;GraphQL 入参 → 服务层 DTO。
 */
object ProductOptionMapper : ObjectMappie<ProductOptionModel, ProductOption>() {
  override fun map(from: ProductOptionModel): ProductOption =
    mapping {
      to::id fromValue (from.id ?: 0L)
      to::optionType fromExpression { ProductOptionTypeMapper.map(from.optionType) }
    }
}

object ProductOptionTypeMapper : EnumMappie<DomainProductOptionType, ProductOptionType>()

object ProductOptionTypeInputMapper : EnumMappie<ProductOptionType, DomainProductOptionType>()

object CreateProductOptionInputMapper : ObjectMappie<CreateProductOptionInput, ProductOptionCreateRequest>() {
  override fun map(from: CreateProductOptionInput): ProductOptionCreateRequest =
    mapping {
      to::optionType fromExpression { ProductOptionTypeInputMapper.map(from.optionType) }
      to::enabled fromValue (from.enabled ?: true)
    }
}

object UpdateProductOptionInputMapper : ObjectMappie<UpdateProductOptionInput, ProductOptionUpdateRequest>() {
  override fun map(from: UpdateProductOptionInput): ProductOptionUpdateRequest =
    mapping {
      to::enabled fromValue (from.enabled ?: true)
    }
}
