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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.yidafu.aqua.client.product.resolvers

import dev.yidafu.aqua.api.service.product.ProductFavoriteService
import dev.yidafu.aqua.common.annotation.ClientService
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.graphql.generated.FavoriteProductVo
import dev.yidafu.aqua.common.graphql.generated.FavoriteProductVoPage
import dev.yidafu.aqua.common.graphql.generated.PageInfoVo
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.product.mapper.toGraphQL
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import java.time.LocalDateTime

/**
 * 客户端收藏查询解析器
 * 提供用户收藏相关查询功能，用户只能管理自己的收藏
 */
@ClientService
@Controller
class ClientFavoriteQueryResolver(
  private val productFavoriteService: ProductFavoriteService,
) {
  private val logger = LoggerFactory.getLogger(ClientFavoriteQueryResolver::class.java)

  /**
   * 获取用户收藏的商品列表
   */
  @QueryMapping
  @PreAuthorize("isAuthenticated()")
  fun favoriteProducts(
    @Argument page: Int,
    @Argument size: Int,
    @AuthenticationPrincipal userPrincipal: UserPrincipal,
  ): FavoriteProductVoPage =
    try {
      val pageable = PageRequest.of(page, size)
      val productPage = productFavoriteService.getFavoriteProducts(userPrincipal.id, pageable)

      // Get user's favorite entities to get creation dates
      val favoriteEntities = productFavoriteService.getUserFavoriteEntities(userPrincipal.id, pageable)

      // Create map of product ID to creation date
      val favoriteMap = favoriteEntities.content.associateBy({ it.productId }, { it.createdAt })

      val favoriteProducts =
        productPage.content.map { product ->
          createFavoriteProduct(product, favoriteMap[product.id])
        }

      FavoriteProductVoPage(
        list = favoriteProducts,
        pageInfo =
          PageInfoVo(
            total = productPage.totalElements.toInt(),
            pageSize = productPage.size,
            pageNum = productPage.number,
            hasNext = productPage.hasNext(),
            hasPrevious = productPage.hasPrevious(),
            totalPages = productPage.totalPages,
          ),
      )
    } catch (e: Exception) {
      logger.error("Failed to get favorite products for user: ${userPrincipal.id}", e)
      throw RuntimeException("获取收藏列表失败: ${e.message}")
    }

  /**
   * 检查商品是否被用户收藏
   */
  @QueryMapping
  @PreAuthorize("isAuthenticated()")
  fun isProductFavorited(
    @Argument productId: Long,
    @AuthenticationPrincipal userPrincipal: UserPrincipal,
  ): Boolean =
    try {
      productFavoriteService.isProductFavorited(userPrincipal.id, productId)
    } catch (e: Exception) {
      logger.error("Failed to check favorite status for user: ${userPrincipal.id}, product: $productId", e)
      false
    }

  /**
   * 获取用户收藏数量
   */
  @QueryMapping
  @PreAuthorize("isAuthenticated()")
  fun favoritesCount(
    @AuthenticationPrincipal userPrincipal: UserPrincipal,
  ): Long =
    try {
      productFavoriteService.getFavoritesCount(userPrincipal.id)
    } catch (e: Exception) {
      logger.error("Failed to get favorites count for user: ${userPrincipal.id}", e)
      0L
    }

  /**
   * Create FavoriteProductVo from ProductModel with addedAt timestamp
   */
  private fun createFavoriteProduct(
    product: ProductModel,
    addedAt: LocalDateTime?,
  ): FavoriteProductVo =
    FavoriteProductVo(
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
