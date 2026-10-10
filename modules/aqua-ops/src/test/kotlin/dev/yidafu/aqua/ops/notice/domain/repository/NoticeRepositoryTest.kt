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

package dev.yidafu.aqua.ops.notice.domain.repository

import dev.yidafu.aqua.common.domain.model.NoticeModel
import dev.yidafu.aqua.common.domain.model.enums.NoticeStatus
import dev.yidafu.aqua.common.domain.model.enums.NoticeType
import dev.yidafu.aqua.ops.common.exposed.OpsPgTestSupport
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

/**
 * 公告仓储实现测试（Testcontainers 真实 PG + Exposed）。
 * 重点覆盖生效公告的「状态 + 时间窗 + 置顶优先」口径（AC-OPS-002-02）。
 */
class NoticeRepositoryTest :
  FunSpec({
    val database = OpsPgTestSupport.database
    val repository = NoticeRepository(database)

    beforeSpec { OpsPgTestSupport.reset() }

    fun notice(
      title: String,
      status: NoticeStatus = NoticeStatus.PUBLISHED,
      type: NoticeType = NoticeType.PLATFORM_NOTICE,
      pinned: Boolean = false,
      publishedAt: LocalDateTime? = LocalDateTime.now(),
      startAt: LocalDateTime? = null,
      endAt: LocalDateTime? = null,
    ): NoticeModel =
      NoticeModel(
        title = title,
        content = "$title 内容",
        type = type,
        pinned = pinned,
        startAt = startAt,
        endAt = endAt,
        status = status,
        publishedAt = publishedAt,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )

    test("save 新建 + findById 回读") {
      OpsPgTestSupport.reset()
      val saved = repository.save(notice("停水公告"))

      val loaded = repository.findById(saved.id!!).orElseThrow()
      loaded.title shouldBe "停水公告"
      loaded.status shouldBe NoticeStatus.PUBLISHED
    }

    test("save 更新：状态与发布时间覆盖") {
      OpsPgTestSupport.reset()
      val saved = repository.save(notice("草稿公告", status = NoticeStatus.DRAFT, publishedAt = null))
      saved.status = NoticeStatus.PUBLISHED
      saved.publishedAt = LocalDateTime.now()
      repository.save(saved)

      repository.findById(saved.id!!).orElseThrow().status shouldBe NoticeStatus.PUBLISHED
    }

    test("findActiveNotices：草稿/已停用不出现，时间窗外不出现，置顶优先") {
      OpsPgTestSupport.reset()
      val now = LocalDateTime.now()
      repository.save(notice("普通已发布", publishedAt = now.minusHours(3)))
      repository.save(notice("置顶已发布", pinned = true, publishedAt = now.minusHours(5)))
      repository.save(notice("草稿", status = NoticeStatus.DRAFT, publishedAt = null))
      repository.save(notice("已停用", status = NoticeStatus.STOPPED))
      repository.save(notice("未开始", startAt = now.plusHours(1)))
      repository.save(notice("已结束", endAt = now.minusHours(1)))

      repository.findActiveNotices(now).map { it.title } shouldBe listOf("置顶已发布", "普通已发布")
    }

    test("delete 后 findById/findAll/findPage 均过滤") {
      OpsPgTestSupport.reset()
      val saved = repository.save(notice("待删除"))

      repository.delete(saved)

      repository.findById(saved.id!!).isPresent shouldBe false
      repository.findAll() shouldBe emptyList()
      repository.findPage(NoticeFilter(), PageRequest.of(0, 10)).totalElements shouldBe 0
    }

    test("findPage：关键字/类型/状态组合过滤") {
      OpsPgTestSupport.reset()
      repository.save(notice("停水通知A", type = NoticeType.WATER_OUTAGE))
      repository.save(notice("活动通知B", type = NoticeType.ACTIVITY_PREVIEW, status = NoticeStatus.DRAFT, publishedAt = null))

      val byKeyword = repository.findPage(NoticeFilter(keyword = "停水"), PageRequest.of(0, 10))
      byKeyword.totalElements shouldBe 1
      byKeyword.content.single().title shouldBe "停水通知A"

      val byTypeAndStatus =
        repository.findPage(
          NoticeFilter(type = NoticeType.ACTIVITY_PREVIEW, status = NoticeStatus.DRAFT),
          PageRequest.of(0, 10),
        )
      byTypeAndStatus.totalElements shouldBe 1
      byTypeAndStatus.content.single().title shouldBe "活动通知B"
    }
  })
