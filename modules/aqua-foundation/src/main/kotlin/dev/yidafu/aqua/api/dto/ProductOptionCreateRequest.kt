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

package dev.yidafu.aqua.api.dto

import dev.yidafu.aqua.common.domain.model.enums.ProductOptionType

/**
 * 商品属性选项新建请求(F-PRO-009):
 * 规格/水源地字典选项的新增入参(由 GraphQL CreateProductOptionInput 映射而来)。
 */
data class ProductOptionCreateRequest(
  val optionType: ProductOptionType,
  val value: String,
  val sortOrder: Int?,
  val enabled: Boolean,
)
