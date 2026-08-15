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

import dev.yidafu.aqua.api.service.ops.BannerQueryService
import dev.yidafu.aqua.common.domain.model.BannerModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AdminBannerQueryResolverTest {
  private lateinit var bannerQueryService: BannerQueryService
  private lateinit var resolver: AdminBannerQueryResolver

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

  @BeforeEach
  fun setUp() {
    bannerQueryService = mockk(relaxed = true)
    resolver = AdminBannerQueryResolver(bannerQueryService)
  }

  @Test
  fun `banners should list with default request`() {
    // Given
    every { bannerQueryService.listBanners(any()) } returns PageImpl(listOf(banner()))

    // When
    val result = resolver.banners(null)

    // Then
    assertEquals(1, result.content.size)
    assertEquals("新客立减", result.content[0].title)
    verify { bannerQueryService.listBanners(any()) }
  }

  @Test
  fun `banner should return null when missing`() {
    // Given
    every { bannerQueryService.getBanner(999L) } returns null

    // When
    val result = resolver.banner(999L)

    // Then
    assertNull(result)
  }

  @Test
  fun `banner should map by id`() {
    // Given
    every { bannerQueryService.getBanner(1L) } returns banner()

    // When
    val result = resolver.banner(1L)

    // Then
    assertEquals("新客立减", result!!.title)
    verify { bannerQueryService.getBanner(1L) }
  }
}
