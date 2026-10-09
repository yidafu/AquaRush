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

import dev.yidafu.aqua.api.service.product.ProductFavoriteService
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.graphql.generated.FavoriteProduct
import dev.yidafu.aqua.common.graphql.generated.FavoriteProductVoPage
import dev.yidafu.aqua.common.graphql.generated.PageInfo
import dev.yidafu.aqua.product.mapper.toGraphQL
import org.springframework.data.domain.PageRequest
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import java.time.LocalDateTime

/**
 * 用户收藏查询 Resolver(管理后台,PERMISSION_USER_READ):
 * 供用户详情页「收藏商品」Tab 展示,按 userId 显式过滤(PRD 03-product.md)。
 */
@Controller
class AdminFavoriteQueryResolver(
  private val productFavoriteService: ProductFavoriteService,
) {
  /**
   * 用户收藏的商品列表(分页,收藏时间 addedAt 取自收藏记录)
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_USER_READ')")
  fun favoriteProducts(
    @Argument userId: Long,
    @Argument page: Int,
    @Argument size: Int,
  ): FavoriteProductVoPage {
    val pageable = PageRequest.of(page, size)
    val productPage = productFavoriteService.getFavoriteProducts(userId, pageable)
    val favoriteEntities = productFavoriteService.getUserFavoriteEntities(userId, pageable)
    val favoriteMap = favoriteEntities.content.associateBy({ it.productId }, { it.createdAt })
    val list =
      productPage.content.map { product ->
        createFavoriteProduct(product, favoriteMap[product.id])
      }
    return FavoriteProductVoPage(
      list = list,
      pageInfo =
        PageInfo(
          total = productPage.totalElements.toInt(),
          pageSize = productPage.size,
          pageNum = productPage.number,
          hasNext = productPage.hasNext(),
          hasPrevious = productPage.hasPrevious(),
          totalPages = productPage.totalPages,
        ),
    )
  }

  private fun createFavoriteProduct(
    product: ProductModel,
    addedAt: LocalDateTime?,
  ): FavoriteProduct =
    FavoriteProduct(
      id = product.id!!,
      name = product.name,
      subtitle = product.subtitle ?: "",
      priceCents = product.price,
      originalPriceCents = product.originalPrice,
      coverImageUrl = product.coverImageUrl,
      stock = product.stock,
      salesVolume = product.salesVolume,
      status = product.status.toGraphQL(),
      addedAt = addedAt ?: product.createdAt,
    )
}
