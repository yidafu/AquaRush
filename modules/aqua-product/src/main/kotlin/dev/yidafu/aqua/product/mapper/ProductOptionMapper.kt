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

import dev.yidafu.aqua.common.domain.model.ProductOptionModel
import dev.yidafu.aqua.common.graphql.generated.ProductOptionType
import dev.yidafu.aqua.common.graphql.generated.ProductOptionVo
import dev.yidafu.aqua.common.domain.model.enums.ProductOptionType as DomainProductOptionType

/**
 * 商品属性选项字典 Mapper(F-PRO-009):
 * 实体 → GraphQL ProductOptionVo;GraphQL 枚举 ↔ 领域枚举同名互转。
 */
fun ProductOptionModel.toGraphQL(): ProductOptionVo =
  ProductOptionVo(
    createdAt = createdAt,
    enabled = enabled,
    id = id ?: 0L,
    optionType = optionType.toGraphQL(),
    sortOrder = sortOrder,
    updatedAt = updatedAt,
    value = value,
  )

/**
 * 领域枚举 → GraphQL 枚举(同名 valueOf 互转)
 */
fun DomainProductOptionType.toGraphQL(): ProductOptionType = ProductOptionType.valueOf(name)

/**
 * GraphQL 枚举 → 领域枚举(同名 valueOf 互转)
 */
fun ProductOptionType.toDomain(): DomainProductOptionType = DomainProductOptionType.valueOf(name)
