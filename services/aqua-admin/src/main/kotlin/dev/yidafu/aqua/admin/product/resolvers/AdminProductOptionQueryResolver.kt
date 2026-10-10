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

package dev.yidafu.aqua.admin.product.resolvers

import dev.yidafu.aqua.api.service.product.ProductOptionQueryService
import dev.yidafu.aqua.common.graphql.generated.ProductOptionType
import dev.yidafu.aqua.common.graphql.generated.ProductOptionVo
import dev.yidafu.aqua.product.mapper.toDomain
import dev.yidafu.aqua.product.mapper.toGraphQL
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

/**
 * 商品属性选项字典查询 Resolver(管理后台,PERMISSION_PRODUCT_READ)
 * 与 PRD 03-product.md §F-PRO-009 保持一致:商品表单「规格 / 水源地」下拉选项的来源。
 */
@Controller
class AdminProductOptionQueryResolver(
  private val productOptionQueryService: ProductOptionQueryService,
) {
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_PRODUCT_READ')")
  fun productOptions(
    @Argument type: ProductOptionType,
    @Argument enabledOnly: Boolean = false,
  ): List<ProductOptionVo> =
    productOptionQueryService
      .listOptions(
        type = type.toDomain(),
        enabledOnly = enabledOnly,
      ).map { it.toGraphQL() }
}
