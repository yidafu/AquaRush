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

package dev.yidafu.aqua.ops.banner.service.impl

import dev.yidafu.aqua.common.domain.model.BannerModel
import dev.yidafu.aqua.common.domain.model.enums.BannerLinkType
import dev.yidafu.aqua.ops.banner.domain.repository.BannerRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * 用户端轮播图查询服务单元测试(R-OPS-01:时间窗/启用过滤由仓库 JPQL 完成,服务透传)
 */
class BannerQueryApiServiceImplTest {
  private lateinit var bannerRepository: BannerRepository
  private lateinit var service: BannerQueryApiServiceImpl

  @BeforeEach
  fun setUp() {
    bannerRepository = mockk(relaxed = true)
    service = BannerQueryApiServiceImpl(bannerRepository)
  }

  @Test
  @DisplayName("getActiveBanners 返回仓库过滤结果(启用+时间窗内)")
  fun `getActiveBanners returns repository filtered list`() {
    val active =
      listOf(
        BannerModel(
          id = 1L,
          title = "A",
          linkType = BannerLinkType.CUSTOM_PAGE,
          linkTarget = "/pages/home/index",
          sortOrder = 1,
          enabled = true,
        ),
        BannerModel(
          id = 2L,
          title = "B",
          linkType = BannerLinkType.CUSTOM_PAGE,
          linkTarget = "/pages/home/index",
          sortOrder = 2,
          enabled = true,
        ),
      )
    every { bannerRepository.findActiveBanners(any()) } returns active

    val result = service.getActiveBanners()

    assertEquals(2, result.size)
    assertEquals(1, result[0].sortOrder)
    verify { bannerRepository.findActiveBanners(any()) }
  }

  @Test
  @DisplayName("无生效轮播时返回空列表(用户端隐藏轮播位)")
  fun `getActiveBanners returns empty when none active`() {
    every { bannerRepository.findActiveBanners(any()) } returns emptyList()

    val result = service.getActiveBanners()

    assertEquals(0, result.size)
  }
}
