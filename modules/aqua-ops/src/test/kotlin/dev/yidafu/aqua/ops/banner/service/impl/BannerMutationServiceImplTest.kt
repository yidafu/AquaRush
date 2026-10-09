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

import dev.yidafu.aqua.api.dto.ReorderDirection
import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.common.domain.model.BannerModel
import dev.yidafu.aqua.common.domain.model.enums.BannerLinkType
import dev.yidafu.aqua.common.graphql.generated.CreateBannerInput
import dev.yidafu.aqua.common.graphql.generated.UpdateBannerInput
import dev.yidafu.aqua.ops.audit.OpsConfigAuditService
import dev.yidafu.aqua.ops.banner.domain.exception.DuplicateSortOrderException
import dev.yidafu.aqua.ops.banner.domain.exception.InvalidTimeWindowException
import dev.yidafu.aqua.ops.banner.domain.repository.BannerRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import dev.yidafu.aqua.common.graphql.generated.BannerLinkType as GqlBannerLinkType

/**
 * 轮播图变更服务单元测试(F-OPS-001 核心规则:排序号唯一、时间窗合法、跳转校验、审计留痕)
 */
class BannerMutationServiceImplTest :
  FunSpec({
    var bannerRepository: BannerRepository = mockk()
    var productQueryApiService: ProductQueryApiService = mockk()
    var opsConfigAuditService: OpsConfigAuditService = mockk()
    var service: BannerMutationServiceImpl = mockk()

    fun request(
      title: String = "测试轮播",
      sortOrder: Int? = null,
      linkType: GqlBannerLinkType = GqlBannerLinkType.CUSTOM_PAGE,
      linkTarget: String = "/pages/home/index",
      startAt: LocalDateTime? = null,
      endAt: LocalDateTime? = null,
    ): CreateBannerInput =
      CreateBannerInput(
        imageUrl = "http://localhost:9090/api/v1/storage/files/x.png",
        title = title,
        linkType = linkType,
        linkTarget = linkTarget,
        sortOrder = sortOrder,
        startAt = startAt,
        endAt = endAt,
      )

    beforeTest {
      bannerRepository = mockk(relaxed = true)
      productQueryApiService = mockk(relaxed = true)
      opsConfigAuditService = mockk(relaxed = true)
      service = BannerMutationServiceImpl(bannerRepository, productQueryApiService, opsConfigAuditService)
    }
    test("createBanner assigns default sort order") {
      every { bannerRepository.findMaxSortOrder() } returns 5
      every { bannerRepository.findBySortOrder(any()) } returns null
      every { bannerRepository.save(any()) } answers { firstArg() }

      val created = service.createBanner(1L, "admin", request())

      created.sortOrder shouldBe 6
    }
    test("createBanner assigns sort order 1 when empty") {
      every { bannerRepository.findMaxSortOrder() } returns null
      every { bannerRepository.findBySortOrder(any()) } returns null
      every { bannerRepository.save(any()) } answers { firstArg() }

      val created = service.createBanner(1L, "admin", request())

      created.sortOrder shouldBe 1
    }
    test("createBanner rejects duplicate sort order") {
      every { bannerRepository.findMaxSortOrder() } returns 1
      every { bannerRepository.findBySortOrder(1) } returns BannerModel(id = 99L, title = "已存在", sortOrder = 1)

      shouldThrow<DuplicateSortOrderException> {
        service.createBanner(1L, "admin", request(sortOrder = 1))
      }
    }
    test("createBanner rejects invalid time window") {
      shouldThrow<InvalidTimeWindowException> {
        service.createBanner(
          1L,
          "admin",
          request(startAt = LocalDateTime.of(2026, 8, 12, 12, 0), endAt = LocalDateTime.of(2026, 8, 12, 10, 0)),
        )
      }
    }
    test("createBanner rejects nonexistent product link") {
      every { productQueryApiService.existsById(999L) } returns false

      shouldThrow<IllegalArgumentException> {
        service.createBanner(1L, "admin", request(linkType = GqlBannerLinkType.PRODUCT, linkTarget = "999"))
      }
    }
    test("createBanner writes audit") {
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
    test("updateBanner writes audit with before and after") {
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
          UpdateBannerInput(
            imageUrl = "http://x/1.png",
            title = "新标题",
            subtitle = null,
            linkType = GqlBannerLinkType.CUSTOM_PAGE,
            linkTarget = "/pages/about/index",
            sortOrder = 2,
            enabled = true,
          ),
        )

      updated.title shouldBe "新标题"
      updated.sortOrder shouldBe 2
      updated.enabled shouldBe true
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
    test("reorderBanner swaps adjacent sort orders") {
      val bannerA = BannerModel(id = 1L, title = "A", sortOrder = 1)
      val bannerB = BannerModel(id = 2L, title = "B", sortOrder = 2)
      every { bannerRepository.findAllOrderedForUpdate() } returns listOf(bannerA, bannerB)
      every { bannerRepository.saveAll(any<List<BannerModel>>()) } returns emptyList()

      val result = service.reorderBanner(1L, "admin", 2L, ReorderDirection.UP)

      result[0].sortOrder shouldBe 2 // B 上移
      result[1].sortOrder shouldBe 1
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
    test("reorderBanner at boundary does nothing") {
      val bannerA = BannerModel(id = 1L, title = "A", sortOrder = 1)
      every { bannerRepository.findAllOrderedForUpdate() } returns listOf(bannerA)

      val result = service.reorderBanner(1L, "admin", 1L, ReorderDirection.UP)

      result.size shouldBe 1
      verify(exactly = 0) { bannerRepository.saveAll(any<List<BannerModel>>()) }
    }
    test("deleteBanner writes audit") {
      val existing = BannerModel(id = 1L, title = "待删除", sortOrder = 1)
      every { bannerRepository.findById(1L) } returns java.util.Optional.of(existing)

      val result = service.deleteBanner(1L, "admin", 1L)

      result shouldBe true
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
  })
