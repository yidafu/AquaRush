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

import dev.yidafu.aqua.api.dto.BannerCreateRequest
import dev.yidafu.aqua.api.dto.BannerUpdateRequest
import dev.yidafu.aqua.api.dto.ReorderDirection
import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.common.domain.model.BannerModel
import dev.yidafu.aqua.common.domain.model.enums.BannerLinkType
import dev.yidafu.aqua.ops.audit.OpsConfigAuditService
import dev.yidafu.aqua.ops.banner.domain.exception.DuplicateSortOrderException
import dev.yidafu.aqua.ops.banner.domain.exception.InvalidTimeWindowException
import dev.yidafu.aqua.ops.banner.domain.repository.BannerRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * 轮播图变更服务单元测试(F-OPS-001 核心规则:排序号唯一、时间窗合法、跳转校验、审计留痕)
 */
class BannerMutationServiceImplTest {
  private lateinit var bannerRepository: BannerRepository
  private lateinit var productQueryApiService: ProductQueryApiService
  private lateinit var opsConfigAuditService: OpsConfigAuditService
  private lateinit var service: BannerMutationServiceImpl

  private fun request(
    title: String = "测试轮播",
    sortOrder: Int? = null,
    linkType: BannerLinkType = BannerLinkType.CUSTOM_PAGE,
    linkTarget: String = "/pages/home/index",
    startAt: LocalDateTime? = null,
    endAt: LocalDateTime? = null,
  ): BannerCreateRequest =
    BannerCreateRequest(
      imageUrl = "http://localhost:9090/api/v1/storage/files/x.png",
      title = title,
      linkType = linkType,
      linkTarget = linkTarget,
      sortOrder = sortOrder,
      startAt = startAt,
      endAt = endAt,
    )

  @BeforeEach
  fun setUp() {
    bannerRepository = mockk(relaxed = true)
    productQueryApiService = mockk(relaxed = true)
    opsConfigAuditService = mockk(relaxed = true)
    service = BannerMutationServiceImpl(bannerRepository, productQueryApiService, opsConfigAuditService)
  }

  @Test
  @DisplayName("createBanner 缺省排序号 = 当前最大 + 1")
  fun `createBanner assigns default sort order`() {
    every { bannerRepository.findMaxSortOrder() } returns 5
    every { bannerRepository.findBySortOrder(any()) } returns null
    every { bannerRepository.save(any()) } answers { firstArg() }

    val created = service.createBanner(1L, "admin", request())

    assertEquals(6, created.sortOrder)
  }

  @Test
  @DisplayName("createBanner 空表时缺省排序号 = 1")
  fun `createBanner assigns sort order 1 when empty`() {
    every { bannerRepository.findMaxSortOrder() } returns null
    every { bannerRepository.findBySortOrder(any()) } returns null
    every { bannerRepository.save(any()) } answers { firstArg() }

    val created = service.createBanner(1L, "admin", request())

    assertEquals(1, created.sortOrder)
  }

  @Test
  @DisplayName("createBanner 重复排序号抛 DuplicateSortOrderException")
  fun `createBanner rejects duplicate sort order`() {
    every { bannerRepository.findMaxSortOrder() } returns 1
    every { bannerRepository.findBySortOrder(1) } returns BannerModel(id = 99L, title = "已存在", sortOrder = 1)

    assertFailsWith<DuplicateSortOrderException> {
      service.createBanner(1L, "admin", request(sortOrder = 1))
    }
  }

  @Test
  @DisplayName("createBanner 时间窗开始 >= 结束抛 InvalidTimeWindowException")
  fun `createBanner rejects invalid time window`() {
    assertFailsWith<InvalidTimeWindowException> {
      service.createBanner(
        1L,
        "admin",
        request(startAt = LocalDateTime.of(2026, 8, 12, 12, 0), endAt = LocalDateTime.of(2026, 8, 12, 10, 0)),
      )
    }
  }

  @Test
  @DisplayName("createBanner PRODUCT 跳转商品不存在抛异常")
  fun `createBanner rejects nonexistent product link`() {
    every { productQueryApiService.existsById(999L) } returns false

    assertFailsWith<IllegalArgumentException> {
      service.createBanner(1L, "admin", request(linkType = BannerLinkType.PRODUCT, linkTarget = "999"))
    }
  }

  @Test
  @DisplayName("createBanner 落审计日志(CREATE)")
  fun `createBanner writes audit`() {
    every { bannerRepository.findMaxSortOrder() } returns null
    every { bannerRepository.findBySortOrder(any()) } returns null
    every { bannerRepository.save(any()) } answers { firstArg() }

    service.createBanner(1L, "admin", request())

    verify {
      opsConfigAuditService.record(
        OpsConfigAuditService.TARGET_BANNER,
        any(),
        OpsConfigAuditService.ACTION_BANNER_CREATE,
        1L,
        "admin",
        before = any(),
        after = any(),
        description = any(),
      )
    }
  }

  @Test
  @DisplayName("updateBanner 落审计日志且含前后值")
  fun `updateBanner writes audit with before and after`() {
    val existing =
      BannerModel(id = 1L, title = "旧标题", linkType = BannerLinkType.CUSTOM_PAGE, linkTarget = "/pages/home/index", sortOrder = 1)
    every { bannerRepository.findById(1L) } returns java.util.Optional.of(existing)
    every { bannerRepository.save(any()) } answers { firstArg() }
    every { bannerRepository.findBySortOrder(any()) } returns null

    val updated =
      service.updateBanner(
        1L,
        "admin",
        1L,
        BannerUpdateRequest("http://x/1.png", "新标题", null, BannerLinkType.CUSTOM_PAGE, "/pages/about/index", 2, true),
      )

    assertEquals("新标题", updated.title)
    assertEquals(2, updated.sortOrder)
    assertTrue(updated.enabled)
    verify {
      opsConfigAuditService.record(
        OpsConfigAuditService.TARGET_BANNER,
        1L,
        OpsConfigAuditService.ACTION_BANNER_UPDATE,
        1L,
        "admin",
        before = any(),
        after = any(),
        description = any(),
      )
    }
  }

  @Test
  @DisplayName("reorderBanner UP 交换相邻排序号并落审计")
  fun `reorderBanner swaps adjacent sort orders`() {
    val bannerA = BannerModel(id = 1L, title = "A", sortOrder = 1)
    val bannerB = BannerModel(id = 2L, title = "B", sortOrder = 2)
    every { bannerRepository.findAllOrderedForUpdate() } returns listOf(bannerA, bannerB)
    every { bannerRepository.saveAll(any<List<BannerModel>>()) } returns emptyList()

    val result = service.reorderBanner(1L, "admin", 2L, ReorderDirection.UP)

    assertEquals(2, result[0].sortOrder) // B 上移
    assertEquals(1, result[1].sortOrder)
    verify {
      opsConfigAuditService.record(
        OpsConfigAuditService.TARGET_BANNER,
        2L,
        OpsConfigAuditService.ACTION_BANNER_REORDER,
        1L,
        "admin",
        before = any(),
        after = any(),
        description = any(),
      )
    }
  }

  @Test
  @DisplayName("reorderBanner 边界(已在顶部)不调整")
  fun `reorderBanner at boundary does nothing`() {
    val bannerA = BannerModel(id = 1L, title = "A", sortOrder = 1)
    every { bannerRepository.findAllOrderedForUpdate() } returns listOf(bannerA)

    val result = service.reorderBanner(1L, "admin", 1L, ReorderDirection.UP)

    assertEquals(1, result.size)
    verify(exactly = 0) { bannerRepository.saveAll(any<List<BannerModel>>()) }
  }

  @Test
  @DisplayName("deleteBanner 落审计日志(DELETE)")
  fun `deleteBanner writes audit`() {
    val existing = BannerModel(id = 1L, title = "待删除", sortOrder = 1)
    every { bannerRepository.findById(1L) } returns java.util.Optional.of(existing)

    val result = service.deleteBanner(1L, "admin", 1L)

    assertTrue(result)
    verify {
      opsConfigAuditService.record(
        OpsConfigAuditService.TARGET_BANNER,
        1L,
        OpsConfigAuditService.ACTION_BANNER_DELETE,
        1L,
        "admin",
        before = any(),
        after = any(),
        description = any(),
      )
    }
  }
}
