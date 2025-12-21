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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.yidafu.aqua.client.product.resolvers

import dev.yidafu.aqua.common.annotation.ClientService
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.product.service.UserFavoriteService
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import org.springframework.transaction.annotation.Transactional

/**
 * 客户端收藏变更解析器
 * 提供用户收藏管理功能，用户只能管理自己的收藏
 */
@ClientService
@Controller
class ClientFavoriteMutationResolver(
  private val userFavoriteService: UserFavoriteService,
) {
  private val logger = LoggerFactory.getLogger(ClientFavoriteMutationResolver::class.java)

  /**
   * 添加商品到收藏
   */
  @MutationMapping
  @PreAuthorize("isAuthenticated()")
  @Transactional
  fun addToFavorites(
    @Argument productId: Long,
    @AuthenticationPrincipal userPrincipal: UserPrincipal,
  ): Boolean {
    return try {
      userFavoriteService.addToFavorites(userPrincipal.id, productId)
      logger.info("User ${userPrincipal.id} added product $productId to favorites")
      true
    } catch (e: BadRequestException) {
      logger.warn("Failed to add to favorites: ${e.message}")
      throw e
    } catch (e: Exception) {
      logger.error("Failed to add product $productId to favorites for user: ${userPrincipal.id}", e)
      throw BadRequestException("添加收藏失败: ${e.message}")
    }
  }

}
