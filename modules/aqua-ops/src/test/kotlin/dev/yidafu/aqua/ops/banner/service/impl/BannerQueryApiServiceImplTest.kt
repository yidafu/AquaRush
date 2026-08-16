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
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

/**
 * 用户端轮播图查询服务单元测试(R-OPS-01:时间窗/启用过滤由仓库 JPQL 完成,服务透传)
 */
class BannerQueryApiServiceImplTest :
  FunSpec({
    var bannerRepository: BannerRepository = mockk()
    var service: BannerQueryApiServiceImpl = mockk()

    beforeTest {
      bannerRepository = mockk(relaxed = true)
      service = BannerQueryApiServiceImpl(bannerRepository)
    }
    test("getActiveBanners returns repository filtered list") {
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

      result.size shouldBe 2
      result[0].sortOrder shouldBe 1
      verify { bannerRepository.findActiveBanners(any()) }
    }
    test("getActiveBanners returns empty when none active") {
      every { bannerRepository.findActiveBanners(any()) } returns emptyList()

      val result = service.getActiveBanners()

      result.size shouldBe 0
    }
  })
