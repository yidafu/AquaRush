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
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 用户端运营配置查询 Resolver 测试(F-OPS-001/002/004/005 用户端消费:
 * activeBanners / activeNotices / storeProfile / deliveryFeeRule)
 */
@DisplayName("ClientOpsQueryResolver Tests")
class ClientOpsQueryResolverTest {
  private lateinit var bannerQueryApiService: BannerQueryApiService
  private lateinit var noticeQueryApiService: NoticeQueryApiService
  private lateinit var storeProfileQueryApiService: StoreProfileQueryApiService
  private lateinit var deliveryFeeRuleQueryApiService: DeliveryFeeRuleQueryApiService
  private lateinit var resolver: ClientOpsQueryResolver

  @BeforeEach
  fun setUp() {
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

  @Test
  @DisplayName("activeNotices should return mapped notices")
  fun `activeNotices returns mapped notices`() {
    val notices =
      listOf(
        NoticeModel(id = 1L, title = "停水通知", content = "内容", type = NoticeType.WATER_OUTAGE, status = NoticeStatus.PUBLISHED, pinned = true),
        NoticeModel(id = 2L, title = "活动预告", content = "内容", type = NoticeType.ACTIVITY_PREVIEW, status = NoticeStatus.PUBLISHED),
      )
    every { noticeQueryApiService.getActiveNotices() } returns notices

    val result = resolver.activeNotices()

    assertEquals(2, result.size)
    assertEquals("停水通知", result[0].title)
    assertTrue(result[0].pinned)
    assertEquals("WATER_OUTAGE", result[0].type.name)
    verify { noticeQueryApiService.getActiveNotices() }
  }

  @Test
  @DisplayName("storeProfile should return mapped profile when configured")
  fun `storeProfile returns mapped profile`() {
    val profile = StoreProfileModel(id = 1L, shopName = "好喝山泉", serviceHotline = "400-800-8888")
    every { storeProfileQueryApiService.getStoreProfile() } returns profile

    val result = resolver.storeProfile()

    assertEquals("好喝山泉", result?.shopName)
    assertEquals("400-800-8888", result?.serviceHotline)
  }

  @Test
  @DisplayName("storeProfile should return null when unconfigured")
  fun `storeProfile returns null when unconfigured`() {
    every { storeProfileQueryApiService.getStoreProfile() } returns null

    val result = resolver.storeProfile()

    assertNull(result)
  }

  @Test
  @DisplayName("deliveryFeeRule should return mapped rule (default 4/200)")
  fun `deliveryFeeRule returns mapped rule`() {
    every { deliveryFeeRuleQueryApiService.getEffectiveDeliveryFeeRule() } returns
      DeliveryFeeRuleModel(freeFloorLimit = 4, surchargeCents = 200)

    val result = resolver.deliveryFeeRule()

    assertEquals(4, result.freeFloorLimit)
    assertEquals(200L, result.surchargeCents)
    verify { deliveryFeeRuleQueryApiService.getEffectiveDeliveryFeeRule() }
  }
}
