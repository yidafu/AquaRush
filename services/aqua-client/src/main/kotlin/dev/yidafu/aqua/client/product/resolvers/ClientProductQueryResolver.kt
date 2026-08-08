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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package dev.yidafu.aqua.client.product.resolvers

import dev.yidafu.aqua.common.annotation.ClientService
import dev.yidafu.aqua.common.graphql.generated.Product
import dev.yidafu.aqua.product.mapper.ProductMapper
import dev.yidafu.aqua.product.service.impl.ProductServiceImpl
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

/**
 * 客户端产品查询解析器
 * 提供产品浏览查询功能，仅限访问上线产品
 */
@ClientService
@Controller
class ClientProductQueryResolver(
  private val productService: ProductServiceImpl,
) {
  /**
   * 根据ID查询产品详情（仅限上线产品）
   */
  @PreAuthorize("isAuthenticated()")
  @QueryMapping
  fun product(
    @Argument id: Long,
  ): Product? {
    val product = productService.findById(id)

    return product?.let { ProductMapper.map(it) }
  }
}
