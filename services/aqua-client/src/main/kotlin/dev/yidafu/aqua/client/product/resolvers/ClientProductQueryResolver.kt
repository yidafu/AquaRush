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

import dev.yidafu.aqua.api.dto.ProductSearchRequest
import dev.yidafu.aqua.common.annotation.ClientService
import dev.yidafu.aqua.common.domain.model.enums.ProductModelStatus
import dev.yidafu.aqua.common.graphql.generated.Product
import dev.yidafu.aqua.common.graphql.generated.ProductVoPage
import dev.yidafu.aqua.common.graphql.util.toPageInfo
import dev.yidafu.aqua.product.mapper.toGraphQL
import dev.yidafu.aqua.product.service.impl.ProductServiceImpl
import org.springframework.data.domain.PageRequest
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

    return product?.toGraphQL()
  }

  /**
   * 查询在售商品列表(F-PRO-001/R-PRO-03)
   * 服务端按 在售(ONLINE)且库存>0 过滤,前端不自行拼接;支持关键词与排序
   */
  @PreAuthorize("isAuthenticated()")
  @QueryMapping
  fun activeProducts(
    @Argument keyword: String?,
    @Argument page: Int = 0,
    @Argument size: Int = 20,
    @Argument sortBy: String = "createdAt",
    @Argument sortDirection: String = "desc",
  ): ProductVoPage {
    val pageable = PageRequest.of(page, size)
    val query =
      ProductSearchRequest(
        keyword = keyword,
        status = ProductModelStatus.ONLINE,
        minStock = 0,
        sortBy = resolveActiveSort(sortBy, sortDirection),
      )
    val productsPage = productService.productsPaginated(query, pageable)
    val (productList, pageInfo) = productsPage.toPageInfo { it.toGraphQL() }
    return ProductVoPage(
      list = productList,
      pageInfo = pageInfo,
    )
  }

  /**
   * 用户端排序参数 → 仓储排序键;非法值回退 创建时间倒序
   */
  private fun resolveActiveSort(
    sortBy: String,
    sortDirection: String,
  ): String {
    val direction = if (sortDirection.equals("asc", ignoreCase = true)) "ASC" else "DESC"
    return when (sortBy.lowercase()) {
      "price" -> "PRICE_$direction"
      "salesvolume", "sales_volume" -> "SALES_VOLUME_$direction"
      "sortorder", "sort_order" -> "SORT_ORDER_$direction"
      else -> "CREATED_AT_$direction"
    }
  }
}
