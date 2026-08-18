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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.time.LocalDateTime

class ClientFavoriteQueryResolverTest :
  FunSpec({
    var productFavoriteService: ProductFavoriteService = mockk()
    var resolver: ClientFavoriteQueryResolver = mockk()

    fun principal(): UserPrincipal =
      UserPrincipal(
        id = 1L,
        _username = "testuser",
        userType = "USER",
        _authorities = listOf(SimpleGrantedAuthority("ROLE_USER")),
      )

    fun product(): ProductModel =
      ProductModel(
        id = 1L,
        name = "桶装水19L",
        price = 2100,
        originalPrice = null,
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
      )

    fun favorite(): ProductFavoriteModel =
      ProductFavoriteModel(
        id = 1L,
        userId = 1L,
        productId = 1L,
        createdAt = LocalDateTime.of(2025, 1, 1, 10, 0),
        updatedAt = LocalDateTime.of(2025, 1, 1, 10, 0),
      )

    beforeTest {
      productFavoriteService = mockk(relaxed = true)
      resolver = ClientFavoriteQueryResolver(productFavoriteService)
    }
    test("favoriteProducts should return paginated favorites") {
      // Given
      every { productFavoriteService.getFavoriteProducts(1L, PageRequest.of(0, 10)) } returns
        PageImpl(listOf(product()))
      every { productFavoriteService.getUserFavoriteEntities(1L, PageRequest.of(0, 10)) } returns
        PageImpl(listOf(favorite()))

      // When
      val result = resolver.favoriteProducts(0, 10, principal())

      // Then
      result.list.size shouldBe 1
      result.list[0].name shouldBe "桶装水19L"
      result.list[0].addedAt shouldBe LocalDateTime.of(2025, 1, 1, 10, 0)
      verify { productFavoriteService.getFavoriteProducts(1L, PageRequest.of(0, 10)) }
      verify { productFavoriteService.getUserFavoriteEntities(1L, PageRequest.of(0, 10)) }
    }
    test("favoriteProducts should throw on service error") {
      // Given
      every { productFavoriteService.getFavoriteProducts(1L, PageRequest.of(0, 10)) } throws RuntimeException("db down")

      // When & Then
      val exception = shouldThrow<RuntimeException> { resolver.favoriteProducts(0, 10, principal()) }
      exception.message!!.contains("获取收藏列表失败") shouldBe true
    }
    test("isProductFavorited should return true when favorited") {
      // Given
      every { productFavoriteService.isProductFavorited(1L, 1L) } returns true

      // When
      val result = resolver.isProductFavorited(1L, principal())

      // Then
      result shouldBe true
    }
    test("isProductFavorited should return false on service error") {
      // Given
      every { productFavoriteService.isProductFavorited(1L, 1L) } throws RuntimeException("db down")

      // When
      val result = resolver.isProductFavorited(1L, principal())

      // Then
      result shouldBe false
    }
    test("favoritesCount should return count") {
      // Given
      every { productFavoriteService.getFavoritesCount(1L) } returns 3L

      // When
      val result = resolver.favoritesCount(principal())

      // Then
      result shouldBe 3L
    }
    test("favoritesCount should return zero on service error") {
      // Given
      every { productFavoriteService.getFavoritesCount(1L) } throws RuntimeException("db down")

      // When
      val result = resolver.favoritesCount(principal())

      // Then
      result shouldBe 0L
    }
  })
