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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.security.core.authority.SimpleGrantedAuthority

class ClientFavoriteMutationResolverTest :
  FunSpec({
    var productFavoriteService: ProductFavoriteService = mockk()
    var resolver: ClientFavoriteMutationResolver = mockk()

    fun principal(): UserPrincipal =
      UserPrincipal(
        id = 1L,
        _username = "testuser",
        userType = "USER",
        _authorities = listOf(SimpleGrantedAuthority("ROLE_USER")),
      )

    beforeTest {
      productFavoriteService = mockk(relaxed = true)
      resolver = ClientFavoriteMutationResolver(productFavoriteService)
    }
    test("toggleProductFavorites should return enabled when favorited") {
      // Given
      every { productFavoriteService.toggleFavorite(1L, 1L) } returns true

      // When
      val result = resolver.toggleProductFavorites(1L, principal())

      // Then
      result shouldBe true
      verify { productFavoriteService.toggleFavorite(1L, 1L) }
    }
    test("toggleProductFavorites should return disabled when unfavorited") {
      // Given
      every { productFavoriteService.toggleFavorite(1L, 1L) } returns false

      // When
      val result = resolver.toggleProductFavorites(1L, principal())

      // Then
      result shouldBe false
    }
    test("toggleProductFavorites should rethrow bad request") {
      // Given
      every { productFavoriteService.toggleFavorite(1L, 1L) } throws BadRequestException("商品不存在")

      // When & Then
      val exception = shouldThrow<BadRequestException> { resolver.toggleProductFavorites(1L, principal()) }
      exception.message!!.contains("商品不存在") shouldBe true
    }
    test("toggleProductFavorites should wrap unexpected errors") {
      // Given
      every { productFavoriteService.toggleFavorite(1L, 1L) } throws IllegalStateException("db down")

      // When & Then
      val exception = shouldThrow<BadRequestException> { resolver.toggleProductFavorites(1L, principal()) }
      exception.message!!.contains("切换收藏状态失败") shouldBe true
    }
  })
