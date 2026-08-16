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
import dev.yidafu.aqua.api.service.ops.DeliveryFeeRuleQueryApiService
import dev.yidafu.aqua.api.service.ops.NoticeQueryApiService
import dev.yidafu.aqua.api.service.ops.StoreProfileQueryApiService
import dev.yidafu.aqua.common.domain.model.BannerModel
import dev.yidafu.aqua.common.domain.model.DeliveryFeeRuleModel
import dev.yidafu.aqua.common.domain.model.NoticeModel
import dev.yidafu.aqua.common.domain.model.StoreProfileModel
import dev.yidafu.aqua.common.domain.model.enums.BannerLinkType
import dev.yidafu.aqua.common.domain.model.enums.NoticeStatus
import dev.yidafu.aqua.common.domain.model.enums.NoticeType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

/**
 * 用户端运营配置查询 Resolver 测试(F-OPS-001/002/004/005 用户端消费:
 * activeBanners / activeNotices / storeProfile / deliveryFeeRule)
 */
class ClientOpsQueryResolverTest :
  FunSpec({
    var bannerQueryApiService: BannerQueryApiService = mockk()
    var noticeQueryApiService: NoticeQueryApiService = mockk()
    var storeProfileQueryApiService: StoreProfileQueryApiService = mockk()
    var deliveryFeeRuleQueryApiService: DeliveryFeeRuleQueryApiService = mockk()
    var resolver: ClientOpsQueryResolver = mockk()

    beforeTest {
      bannerQueryApiService = mockk(relaxed = true)
      noticeQueryApiService = mockk(relaxed = true)
      storeProfileQueryApiService = mockk(relaxed = true)
      deliveryFeeRuleQueryApiService = mockk(relaxed = true)
      resolver =
        ClientOpsQueryResolver(
          bannerQueryApiService,
          noticeQueryApiService,
          storeProfileQueryApiService,
          deliveryFeeRuleQueryApiService,
        )
    }
    test("activeBanners returns mapped banners") {
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

      result.size shouldBe 2
      result[0].title shouldBe "轮播A"
      result[1].linkType.name shouldBe "PRODUCT"
      result[1].linkTarget.toLong() shouldBe 10L
      verify { bannerQueryApiService.getActiveBanners() }
    }
    test("activeBanners returns empty when none") {
      every { bannerQueryApiService.getActiveBanners() } returns emptyList()

      val result = resolver.activeBanners()

      result.isEmpty() shouldBe true
    }
    test("activeNotices returns mapped notices") {
      val notices =
        listOf(
          NoticeModel(
            id = 1L,
            title = "停水通知",
            content = "内容",
            type = NoticeType.WATER_OUTAGE,
            status = NoticeStatus.PUBLISHED,
            pinned = true,
          ),
          NoticeModel(id = 2L, title = "活动预告", content = "内容", type = NoticeType.ACTIVITY_PREVIEW, status = NoticeStatus.PUBLISHED),
        )
      every { noticeQueryApiService.getActiveNotices() } returns notices

      val result = resolver.activeNotices()

      result.size shouldBe 2
      result[0].title shouldBe "停水通知"
      result[0].pinned shouldBe true
      result[0].type.name shouldBe "WATER_OUTAGE"
      verify { noticeQueryApiService.getActiveNotices() }
    }
    test("storeProfile returns mapped profile") {
      val profile = StoreProfileModel(id = 1L, shopName = "好喝山泉", serviceHotline = "400-800-8888")
      every { storeProfileQueryApiService.getStoreProfile() } returns profile

      val result = resolver.storeProfile()

      result?.shopName shouldBe "好喝山泉"
      result?.serviceHotline shouldBe "400-800-8888"
    }
    test("storeProfile returns null when unconfigured") {
      every { storeProfileQueryApiService.getStoreProfile() } returns null

      val result = resolver.storeProfile()

      result.shouldBeNull()
    }
    test("deliveryFeeRule returns mapped rule") {
      every { deliveryFeeRuleQueryApiService.getEffectiveDeliveryFeeRule() } returns
        DeliveryFeeRuleModel(freeFloorLimit = 4, surchargeCents = 200)

      val result = resolver.deliveryFeeRule()

      result.freeFloorLimit shouldBe 4
      result.surchargeCents shouldBe 200L
      verify { deliveryFeeRuleQueryApiService.getEffectiveDeliveryFeeRule() }
    }
  })
