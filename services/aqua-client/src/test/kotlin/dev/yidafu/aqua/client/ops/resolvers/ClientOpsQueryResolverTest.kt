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

package dev.yidafu.aqua.client.ops.resolvers

import dev.yidafu.aqua.api.service.ops.BannerQueryApiService
import dev.yidafu.aqua.common.domain.model.BannerModel
import dev.yidafu.aqua.common.domain.model.enums.BannerLinkType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 用户端运营配置查询 Resolver 测试(F-OPS-001 用户端消费:activeBanners)
 */
@DisplayName("ClientOpsQueryResolver Tests")
class ClientOpsQueryResolverTest {
  private lateinit var bannerQueryApiService: BannerQueryApiService
  private lateinit var resolver: ClientOpsQueryResolver

  @BeforeEach
  fun setUp() {
    bannerQueryApiService = mockk(relaxed = true)
    resolver = ClientOpsQueryResolver(bannerQueryApiService)
  }

  @Test
  @DisplayName("activeBanners should return mapped banners")
  fun `activeBanners returns mapped banners`() {
    val banners =
      listOf(
        BannerModel(
          id = 1L,
          imageUrl = "http://x/1.png",
          title = "轮播A",
          linkType = BannerLinkType.CUSTOM_PAGE,
          linkTarget = "/pages/home/index",
          sortOrder = 1,
          enabled = true,
        ),
        BannerModel(
          id = 2L,
          imageUrl = "http://x/2.png",
          title = "轮播B",
          linkType = BannerLinkType.PRODUCT,
          linkTarget = "10",
          sortOrder = 2,
          enabled = true,
        ),
      )
    every { bannerQueryApiService.getActiveBanners() } returns banners

    val result = resolver.activeBanners()

    assertEquals(2, result.size)
    assertEquals("轮播A", result[0].title)
    assertEquals("PRODUCT", result[1].linkType.name)
    assertEquals(10L, result[1].linkTarget.toLong())
    verify { bannerQueryApiService.getActiveBanners() }
  }

  @Test
  @DisplayName("activeBanners should return empty list when no active banners")
  fun `activeBanners returns empty when none`() {
    every { bannerQueryApiService.getActiveBanners() } returns emptyList()

    val result = resolver.activeBanners()

    assertTrue(result.isEmpty())
  }
}
