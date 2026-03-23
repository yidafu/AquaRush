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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package dev.yidafu.aqua.admin.product.resolvers

import dev.yidafu.aqua.api.query.ProductSearchRequest
import dev.yidafu.aqua.common.annotation.AdminService
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.graphql.generated.*
import dev.yidafu.aqua.common.graphql.generated.ProductStatistics
import dev.yidafu.aqua.common.graphql.util.toPageInfo
import dev.yidafu.aqua.product.mapper.ProductMapper
import dev.yidafu.aqua.product.mapper.ProductQueryMapper
import dev.yidafu.aqua.product.service.impl.ProductServiceImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

/**
 * 管理端产品查询解析器
 * 提供产品管理的完整查询功能，仅管理员可访问
 */
@AdminService
@Controller
class AdminProductQueryResolver(
  private val productService: ProductServiceImpl,
) {
  /**
   * 查询所有产品（管理员功能，包括下线产品）
   */
  @PreAuthorize("hasRole('ADMIN')")
  @QueryMapping
  fun productsPaginated(
    @Argument input: ProductSearchInput,
  ): ProductPage {
    val actualPage = input.page ?: 0
    val actualSize = input.size ?: 20
    val pageable: Pageable = PageRequest.of(actualPage, actualSize)
    val query = ProductQueryMapper.map(input)
    val productsPage = productService.productsPaginated(query, pageable)

    val (productList, pageInfo) = productsPage.toPageInfo { ProductMapper.map(it) }
    return ProductPage(
      list = productList,
      pageInfo = pageInfo,
    )
  }

  /**
   * 根据ID查询产品详细信息（管理员功能）
   */

  @PreAuthorize("hasRole('ADMIN')")
  @QueryMapping
  fun product(
    @Argument id: Long,
  ): Product? = productService.findById(id)?.let { ProductMapper.map(it) }

  @PreAuthorize("hasRole('ADMIN')")
  @QueryMapping
  fun activeProducts(
    @Argument keyword: String?,
    @Argument page: Int = 0,
    @Argument size: Int = 20,
  ): ProductPage {
    val pageable = PageRequest.of(page, size)
    val productsPage = productService.productsPaginated(ProductSearchRequest(keyword), pageable)
    val (productList, pageInfo) = productsPage.toPageInfo { ProductMapper.map(it) }
    return ProductPage(
      list = productList,
      pageInfo = pageInfo,
    )
  }

  /**
   * 获取产品统计信息（管理员功能）- GraphQL
   */
  @QueryMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun productStatistics(): ProductStatistics = productService.getProductStatistics()

  /**
   * 获取热销产品（管理员功能）
   */
  @QueryMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun topSalesProducts(
    @Argument limit: Int? = 10,
  ): List<ProductModel> = productService.getTopSalesProducts(limit ?: 10)
}
