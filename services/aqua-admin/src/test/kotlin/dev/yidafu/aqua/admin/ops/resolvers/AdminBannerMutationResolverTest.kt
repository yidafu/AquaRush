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

package dev.yidafu.aqua.admin.ops.resolvers

import dev.yidafu.aqua.api.dto.ReorderDirection
import dev.yidafu.aqua.api.service.ops.BannerMutationService
import dev.yidafu.aqua.common.domain.model.BannerModel
import dev.yidafu.aqua.common.graphql.generated.BannerLinkType
import dev.yidafu.aqua.common.graphql.generated.CreateBannerInput
import dev.yidafu.aqua.common.graphql.generated.UpdateBannerInput
import dev.yidafu.aqua.common.security.UserPrincipal
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import dev.yidafu.aqua.common.graphql.generated.ReorderDirection as GraphQLReorderDirection

class AdminBannerMutationResolverTest {
  private lateinit var bannerMutationService: BannerMutationService
  private lateinit var resolver: AdminBannerMutationResolver

  private fun banner(): BannerModel =
    BannerModel(
      id = 1L,
      imageUrl = "http://x/banner.png",
      title = "新客立减",
      linkTarget = "coupon_center",
      enabled = true,
      createdAt = LocalDateTime.now(),
      updatedAt = LocalDateTime.now(),
    )

  private fun principal(): UserPrincipal =
    UserPrincipal(
      id = 1L,
      _username = "admin",
      userType = "ADMIN",
      _authorities = listOf(SimpleGrantedAuthority("ROLE_ADMIN")),
    )

  @BeforeEach
  fun setUp() {
    bannerMutationService = mockk(relaxed = true)
    resolver = AdminBannerMutationResolver(bannerMutationService)
  }

  @Test
  fun `createBanner should delegate`() {
    // Given
    every { bannerMutationService.createBanner(1L, "admin", any()) } returns banner()
    val input =
      CreateBannerInput(
        imageUrl = "http://x/banner.png",
        linkTarget = "coupon_center",
        linkType = BannerLinkType.CUSTOM_PAGE,
        title = "新客立减",
      )

    // When
    val result = resolver.createBanner(input, principal())

    // Then
    assertEquals("新客立减", result.title)
    verify { bannerMutationService.createBanner(1L, "admin", any()) }
  }

  @Test
  fun `updateBanner should delegate`() {
    // Given
    every { bannerMutationService.updateBanner(1L, "admin", 1L, any()) } returns banner()
    val input =
      UpdateBannerInput(
        imageUrl = "http://x/banner.png",
        linkTarget = "coupon_center",
        linkType = BannerLinkType.CUSTOM_PAGE,
        title = "新客立减",
      )

    // When
    val result = resolver.updateBanner(1L, input, principal())

    // Then
    assertEquals("新客立减", result.title)
    verify { bannerMutationService.updateBanner(1L, "admin", 1L, any()) }
  }

  @Test
  fun `deleteBanner should delegate`() {
    // Given
    every { bannerMutationService.deleteBanner(1L, "admin", 1L) } returns true

    // When
    val result = resolver.deleteBanner(1L, principal())

    // Then
    assertTrue(result)
    verify { bannerMutationService.deleteBanner(1L, "admin", 1L) }
  }

  @Test
  fun `reorderBanner should map direction enum`() {
    // Given
    every { bannerMutationService.reorderBanner(1L, "admin", 1L, ReorderDirection.UP) } returns listOf(banner())

    // When
    val result = resolver.reorderBanner(1L, GraphQLReorderDirection.UP, principal())

    // Then
    assertEquals(1, result.size)
    verify { bannerMutationService.reorderBanner(1L, "admin", 1L, ReorderDirection.UP) }
  }
}
