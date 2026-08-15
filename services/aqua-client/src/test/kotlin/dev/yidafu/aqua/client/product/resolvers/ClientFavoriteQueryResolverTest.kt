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
import dev.yidafu.aqua.common.domain.model.ProductFavoriteModel
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.enums.ProductModelStatus
import dev.yidafu.aqua.common.security.UserPrincipal
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClientFavoriteQueryResolverTest {
  private lateinit var productFavoriteService: ProductFavoriteService
  private lateinit var resolver: ClientFavoriteQueryResolver

  private fun principal(): UserPrincipal =
    UserPrincipal(
      id = 1L,
      _username = "testuser",
      userType = "USER",
      _authorities = listOf(SimpleGrantedAuthority("ROLE_USER")),
    )

  private fun product(): ProductModel =
    ProductModel(
      id = 1L,
      name = "桶装水19L",
      price = 2100,
      originalPrice = null,
      depositPrice = null,
      coverImageUrl = "http://x/cover.png",
      imageGallery = null,
      specification = "19L",
      waterSource = "山泉水",
      mineralContent = null,
      stock = 100,
      salesVolume = 0,
      status = ProductModelStatus.ONLINE,
      sortOrder = 1,
      tags = null,
      detailContent = null,
      certificateImages = null,
      deliverySettings = null,
    )

  private fun favorite(): ProductFavoriteModel =
    ProductFavoriteModel(
      id = 1L,
      userId = 1L,
      productId = 1L,
      createdAt = LocalDateTime.of(2025, 1, 1, 10, 0),
      updatedAt = LocalDateTime.of(2025, 1, 1, 10, 0),
    )

  @BeforeEach
  fun setUp() {
    productFavoriteService = mockk(relaxed = true)
    resolver = ClientFavoriteQueryResolver(productFavoriteService)
  }

  @Test
  fun `favoriteProducts should return paginated favorites`() {
    // Given
    every { productFavoriteService.getFavoriteProducts(1L, PageRequest.of(0, 10)) } returns
      PageImpl(listOf(product()))
    every { productFavoriteService.getUserFavoriteEntities(1L, PageRequest.of(0, 10)) } returns
      PageImpl(listOf(favorite()))

    // When
    val result = resolver.favoriteProducts(0, 10, principal())

    // Then
    assertEquals(1, result.list.size)
    assertEquals("桶装水19L", result.list[0].name)
    assertEquals(LocalDateTime.of(2025, 1, 1, 10, 0), result.list[0].addedAt)
    verify { productFavoriteService.getFavoriteProducts(1L, PageRequest.of(0, 10)) }
    verify { productFavoriteService.getUserFavoriteEntities(1L, PageRequest.of(0, 10)) }
  }

  @Test
  fun `favoriteProducts should throw on service error`() {
    // Given
    every { productFavoriteService.getFavoriteProducts(1L, PageRequest.of(0, 10)) } throws RuntimeException("db down")

    // When & Then
    val exception = assertFailsWith<RuntimeException> { resolver.favoriteProducts(0, 10, principal()) }
    assertTrue(exception.message!!.contains("获取收藏列表失败"))
  }

  @Test
  fun `isProductFavorited should return true when favorited`() {
    // Given
    every { productFavoriteService.isProductFavorited(1L, 1L) } returns true

    // When
    val result = resolver.isProductFavorited(1L, principal())

    // Then
    assertTrue(result)
  }

  @Test
  fun `isProductFavorited should return false on service error`() {
    // Given
    every { productFavoriteService.isProductFavorited(1L, 1L) } throws RuntimeException("db down")

    // When
    val result = resolver.isProductFavorited(1L, principal())

    // Then
    assertFalse(result)
  }

  @Test
  fun `favoritesCount should return count`() {
    // Given
    every { productFavoriteService.getFavoritesCount(1L) } returns 3L

    // When
    val result = resolver.favoritesCount(principal())

    // Then
    assertEquals(3L, result)
  }

  @Test
  fun `favoritesCount should return zero on service error`() {
    // Given
    every { productFavoriteService.getFavoritesCount(1L) } throws RuntimeException("db down")

    // When
    val result = resolver.favoritesCount(principal())

    // Then
    assertEquals(0L, result)
  }
}
