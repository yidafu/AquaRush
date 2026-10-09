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

package dev.yidafu.aqua.client.marketing.resolvers

import dev.yidafu.aqua.api.service.marketing.MarketingPricingQueryApiService
import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.client.marketing.mapper.toGraphQL
import dev.yidafu.aqua.common.annotation.ClientService
import dev.yidafu.aqua.common.graphql.generated.Product
import dev.yidafu.aqua.common.graphql.generated.ProductPriceVo
import dev.yidafu.aqua.common.security.UserPrincipal
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import dev.yidafu.aqua.common.graphql.generated.PriceSource as GraphQLPriceSource

/**
 * 用户端商品成交单价 Resolver(R-MKT-01/02)
 * 解析当前登录用户的成交单价:②熟客价(分组商品专属价)→ ③商品目录价;
 * Product 扩展字段 effectivePriceCents/hasGroupPrice 未登录时回退目录价。
 */
@ClientService
@Controller
class ClientProductPriceResolver(
  private val marketingPricingQueryApiService: MarketingPricingQueryApiService,
  private val productQueryApiService: ProductQueryApiService,
) {
  /**
   * 商品成交单价解析(商品不存在返回 null)
   */
  @QueryMapping
  @PreAuthorize("isAuthenticated()")
  fun productPrice(
    @Argument productId: Long,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): ProductPriceVo? {
    val product = productQueryApiService.findById(productId) ?: return null
    val resolution = marketingPricingQueryApiService.resolveUnitPrice(principal.id, productId)
    return resolution.toGraphQL(productId = productId, catalogPriceCents = product.price)
  }

  /**
   * Product 扩展字段:当前用户有效成交单价(分,熟客价优先,回退目录价;未登录回退目录价)
   */
  @SchemaMapping(typeName = "Product", field = "effectivePriceCents")
  fun effectivePriceCents(
    product: Product,
    @AuthenticationPrincipal principal: UserPrincipal?,
  ): Long {
    if (principal == null) {
      return productQueryApiService.findById(product.id)?.price ?: 0L
    }
    return marketingPricingQueryApiService.resolveUnitPrice(principal.id, product.id).unitPriceCents
  }

  /**
   * Product 扩展字段:是否命中熟客价(分组价)
   */
  @SchemaMapping(typeName = "Product", field = "hasGroupPrice")
  fun hasGroupPrice(
    product: Product,
    @AuthenticationPrincipal principal: UserPrincipal?,
  ): Boolean {
    if (principal == null) {
      return false
    }
    return marketingPricingQueryApiService.resolveUnitPrice(principal.id, product.id).hasGroupPrice
  }

  /**
   * Product 扩展字段:成交单价来源(展示标签依据:熟客价/特价/新人价/目录价;未登录回退目录价)
   */
  @SchemaMapping(typeName = "Product", field = "priceSource")
  fun priceSource(
    product: Product,
    @AuthenticationPrincipal principal: UserPrincipal?,
  ): GraphQLPriceSource {
    if (principal == null) {
      return GraphQLPriceSource.CATALOG_PRICE
    }
    return marketingPricingQueryApiService.resolveUnitPrice(principal.id, product.id).priceSource.toGraphQL()
  }
}
