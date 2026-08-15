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

package dev.yidafu.aqua.client.product.resolvers

import dev.yidafu.aqua.api.service.product.ProductFavoriteService
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.security.UserPrincipal
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.core.authority.SimpleGrantedAuthority
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClientFavoriteMutationResolverTest {
  private lateinit var productFavoriteService: ProductFavoriteService
  private lateinit var resolver: ClientFavoriteMutationResolver

  private fun principal(): UserPrincipal =
    UserPrincipal(
      id = 1L,
      _username = "testuser",
      userType = "USER",
      _authorities = listOf(SimpleGrantedAuthority("ROLE_USER")),
    )

  @BeforeEach
  fun setUp() {
    productFavoriteService = mockk(relaxed = true)
    resolver = ClientFavoriteMutationResolver(productFavoriteService)
  }

  @Test
  fun `toggleProductFavorites should return enabled when favorited`() {
    // Given
    every { productFavoriteService.toggleFavorite(1L, 1L) } returns true

    // When
    val result = resolver.toggleProductFavorites(1L, principal())

    // Then
    assertTrue(result)
    verify { productFavoriteService.toggleFavorite(1L, 1L) }
  }

  @Test
  fun `toggleProductFavorites should return disabled when unfavorited`() {
    // Given
    every { productFavoriteService.toggleFavorite(1L, 1L) } returns false

    // When
    val result = resolver.toggleProductFavorites(1L, principal())

    // Then
    assertFalse(result)
  }

  @Test
  fun `toggleProductFavorites should rethrow bad request`() {
    // Given
    every { productFavoriteService.toggleFavorite(1L, 1L) } throws BadRequestException("商品不存在")

    // When & Then
    val exception = assertFailsWith<BadRequestException> { resolver.toggleProductFavorites(1L, principal()) }
    assertTrue(exception.message!!.contains("商品不存在"))
  }

  @Test
  fun `toggleProductFavorites should wrap unexpected errors`() {
    // Given
    every { productFavoriteService.toggleFavorite(1L, 1L) } throws IllegalStateException("db down")

    // When & Then
    val exception = assertFailsWith<BadRequestException> { resolver.toggleProductFavorites(1L, principal()) }
    assertTrue(exception.message!!.contains("切换收藏状态失败"))
  }
}
