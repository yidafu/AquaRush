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

package dev.yidafu.aqua.ops.banner.domain.repository

import dev.yidafu.aqua.common.domain.model.BannerModel
import dev.yidafu.aqua.common.domain.model.enums.BannerLinkType
import dev.yidafu.aqua.ops.common.exposed.ExposedH2TestSupport
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

/**
 * 轮播图仓储实现测试（H2 + Exposed，JPA -> Exposed 迁移的回归网）。
 * 重点覆盖软删过滤、时间窗生效条件与分页（迁移方案 §9 R3/R5）。
 */
class BannerRepositoryTest :
  FunSpec({
    val database = ExposedH2TestSupport.database
    val repository = BannerRepository(database)

    beforeSpec { ExposedH2TestSupport.reset() }

    fun banner(
      sortOrder: Int,
      enabled: Boolean = true,
      title: String = "轮播$sortOrder",
      startAt: LocalDateTime? = null,
      endAt: LocalDateTime? = null,
    ): BannerModel =
      BannerModel(
        imageUrl = "/static/banner$sortOrder.png",
        title = title,
        linkType = BannerLinkType.CUSTOM_PAGE,
        linkTarget = "/pages/index",
        sortOrder = sortOrder,
        enabled = enabled,
        startAt = startAt,
        endAt = endAt,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )

    test("save 新建：生成 Snowflake 主键并落库") {
      ExposedH2TestSupport.reset()
      val saved = repository.save(banner(1))

      saved.id.shouldNotBeNull()
      val loaded = repository.findById(saved.id!!)
      loaded.isPresent shouldBe true
      loaded.get().title shouldBe "轮播1"
      loaded.get().linkType shouldBe BannerLinkType.CUSTOM_PAGE
    }

    test("save 更新：字段覆盖且 updatedAt 刷新") {
      ExposedH2TestSupport.reset()
      val saved = repository.save(banner(1))
      val before = saved.updatedAt

      saved.title = "改过标题"
      repository.save(saved)

      val reloaded = repository.findById(saved.id!!).orElseThrow()
      reloaded.title shouldBe "改过标题"
      (reloaded.updatedAt.isAfter(before) || reloaded.updatedAt == before) shouldBe true
    }

    test("delete 后软删过滤生效：findById/findBySortOrder 查不到") {
      ExposedH2TestSupport.reset()
      val saved = repository.save(banner(1))

      repository.delete(saved)

      repository.findById(saved.id!!).isPresent shouldBe false
      repository.findBySortOrder(1) shouldBe null
      repository.findMaxSortOrder() shouldBe null
    }

    test("findMaxSortOrder 返回当前最大排序号") {
      ExposedH2TestSupport.reset()
      repository.save(banner(3))
      repository.save(banner(7))

      repository.findMaxSortOrder() shouldBe 7
    }

    test("findActiveBanners：仅启用且在时间窗内，按 sort_order 升序") {
      ExposedH2TestSupport.reset()
      val now = LocalDateTime.now()
      repository.save(banner(2, enabled = false))
      repository.save(banner(1, enabled = true))
      repository.save(banner(3, enabled = true, startAt = now.plusHours(1))) // 未开始
      repository.save(banner(4, enabled = true, endAt = now.minusHours(1))) // 已结束
      repository.save(banner(5, enabled = true, startAt = now.minusHours(2), endAt = now.plusHours(2)))

      val active = repository.findActiveBanners(now)

      active.map { it.sortOrder } shouldBe listOf(1, 5)
    }

    test("findPage：关键字与启用状态过滤 + 分页") {
      ExposedH2TestSupport.reset()
      repository.save(banner(1, enabled = true, title = "新品上市"))
      repository.save(banner(2, enabled = false, title = "新品促销"))
      repository.save(banner(3, enabled = true, title = "老客回馈"))

      val page = repository.findPage(BannerFilter(keyword = "新品", enabled = true), PageRequest.of(0, 10))

      page.totalElements shouldBe 1
      page.content.single().title shouldBe "新品上市"
      page.content.single().sortOrder shouldBe 1
    }

    test("findPage：分页参数生效（第二页为空）") {
      ExposedH2TestSupport.reset()
      repository.save(banner(1))
      repository.save(banner(2))

      val page = repository.findPage(BannerFilter(), PageRequest.of(1, 2))

      page.totalElements shouldBe 2
      page.content shouldBe emptyList()
    }

    test("findPage：关键字大小写不敏感") {
      ExposedH2TestSupport.reset()
      repository.save(banner(1, title = "Fresh水"))

      val page = repository.findPage(BannerFilter(keyword = "fresh"), PageRequest.of(0, 10))

      page.totalElements shouldBe 1
      page.content.single().title shouldContain "水"
    }

    test("findAllOrderedForUpdate 按排序号升序返回") {
      ExposedH2TestSupport.reset()
      repository.save(banner(2))
      repository.save(banner(1))

      repository.findAllOrderedForUpdate().map { it.sortOrder } shouldBe listOf(1, 2)
    }
  })
