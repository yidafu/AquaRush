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

package dev.yidafu.aqua.ops.notice.service.impl

import dev.yidafu.aqua.api.dto.CreateNoticeRequest
import dev.yidafu.aqua.api.dto.UpdateNoticeRequest
import dev.yidafu.aqua.common.domain.model.NoticeModel
import dev.yidafu.aqua.common.domain.model.enums.NoticeStatus
import dev.yidafu.aqua.common.domain.model.enums.NoticeType
import dev.yidafu.aqua.ops.audit.OpsConfigAuditService
import dev.yidafu.aqua.ops.notice.domain.exception.InvalidNoticeStateTransitionException
import dev.yidafu.aqua.ops.notice.domain.exception.InvalidNoticeTimeWindowException
import dev.yidafu.aqua.ops.notice.domain.exception.NoticeNotFoundException
import dev.yidafu.aqua.ops.notice.domain.repository.NoticeRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime

/**
 * 公告变更服务单元测试(F-OPS-002 核心规则:状态机 草稿→发布→停用、置顶仅限已发布、时间窗合法、审计留痕)
 */
class NoticeMutationServiceImplTest :
  FunSpec({
    var noticeRepository: NoticeRepository = mockk()
    var opsConfigAuditService: OpsConfigAuditService = mockk()
    var service: NoticeMutationServiceImpl = mockk()

    fun createRequest(
      title: String = "测试公告",
      type: NoticeType = NoticeType.PLATFORM_NOTICE,
      startAt: LocalDateTime? = null,
      endAt: LocalDateTime? = null,
    ): CreateNoticeRequest =
      CreateNoticeRequest(
        title = title,
        content = "公告内容",
        type = type,
        startAt = startAt,
        endAt = endAt,
      )

    beforeTest {
      noticeRepository = mockk(relaxed = true)
      opsConfigAuditService = mockk(relaxed = true)
      service = NoticeMutationServiceImpl(noticeRepository, opsConfigAuditService)
    }
    test("createNotice defaults to DRAFT and unpinned") {
      every { noticeRepository.save(any()) } answers { firstArg() }

      val created = service.createNotice(1L, "admin", createRequest())

      created.status shouldBe NoticeStatus.DRAFT
      created.pinned shouldBe false
      created.type shouldBe NoticeType.PLATFORM_NOTICE
    }
    test("createNotice writes audit") {
      every { noticeRepository.save(any()) } answers { firstArg() }

      service.createNotice(1L, "admin", createRequest())

      verify {
        opsConfigAuditService.record(
          OpsConfigAuditService.TARGET_NOTICE,
          any(),
          OpsConfigAuditService.ACTION_NOTICE_CREATE,
          1L,
          "admin",
          before = any(),
          after = any(),
          description = any(),
        )
      }
    }
    test("createNotice rejects invalid time window") {
      shouldThrow<InvalidNoticeTimeWindowException> {
        service.createNotice(
          1L,
          "admin",
          createRequest(startAt = LocalDateTime.of(2026, 8, 12, 12, 0), endAt = LocalDateTime.of(2026, 8, 12, 10, 0)),
        )
      }
    }
    test("publishNotice transitions DRAFT to PUBLISHED") {
      val draft = NoticeModel(id = 1L, title = "草稿", content = "内容", status = NoticeStatus.DRAFT)
      every { noticeRepository.findById(1L) } returns java.util.Optional.of(draft)
      every { noticeRepository.save(any()) } answers { firstArg() }

      val published = service.publishNotice(1L, "admin", 1L)

      published.status shouldBe NoticeStatus.PUBLISHED
      published.publishedAt.shouldNotBeNull()
      verify {
        opsConfigAuditService.record(
          OpsConfigAuditService.TARGET_NOTICE,
          1L,
          OpsConfigAuditService.ACTION_NOTICE_PUBLISH,
          1L,
          "admin",
          before = any(),
          after = any(),
          description = any(),
        )
      }
    }
    test("publishNotice rejects STOPPED") {
      val stopped = NoticeModel(id = 1L, title = "已停用", content = "内容", status = NoticeStatus.STOPPED)
      every { noticeRepository.findById(1L) } returns java.util.Optional.of(stopped)

      shouldThrow<InvalidNoticeStateTransitionException> {
        service.publishNotice(1L, "admin", 1L)
      }
    }
    test("updateNotice rejects STOPPED") {
      val stopped = NoticeModel(id = 1L, title = "已停用", content = "内容", status = NoticeStatus.STOPPED)
      every { noticeRepository.findById(1L) } returns java.util.Optional.of(stopped)

      shouldThrow<InvalidNoticeStateTransitionException> {
        service.updateNotice(1L, "admin", 1L, UpdateNoticeRequest("新标题", "新内容", NoticeType.WATER_OUTAGE))
      }
    }
    test("updateNotice updates PUBLISHED with audit") {
      val published = NoticeModel(id = 1L, title = "旧标题", content = "旧内容", status = NoticeStatus.PUBLISHED)
      every { noticeRepository.findById(1L) } returns java.util.Optional.of(published)
      every { noticeRepository.save(any()) } answers { firstArg() }

      val updated =
        service.updateNotice(1L, "admin", 1L, UpdateNoticeRequest("新标题", "新内容", NoticeType.WATER_OUTAGE))

      updated.title shouldBe "新标题"
      updated.type shouldBe NoticeType.WATER_OUTAGE
      verify {
        opsConfigAuditService.record(
          OpsConfigAuditService.TARGET_NOTICE,
          1L,
          OpsConfigAuditService.ACTION_NOTICE_UPDATE,
          1L,
          "admin",
          before = any(),
          after = any(),
          description = any(),
        )
      }
    }
    test("stopNotice stops PUBLISHED and unpins") {
      val published = NoticeModel(id = 1L, title = "已发布", content = "内容", status = NoticeStatus.PUBLISHED, pinned = true)
      every { noticeRepository.findById(1L) } returns java.util.Optional.of(published)
      every { noticeRepository.save(any()) } answers { firstArg() }

      val stopped = service.stopNotice(1L, "admin", 1L)

      stopped.status shouldBe NoticeStatus.STOPPED
      stopped.pinned shouldBe false
    }
    test("stopNotice rejects DRAFT") {
      val draft = NoticeModel(id = 1L, title = "草稿", content = "内容", status = NoticeStatus.DRAFT)
      every { noticeRepository.findById(1L) } returns java.util.Optional.of(draft)

      shouldThrow<InvalidNoticeStateTransitionException> {
        service.stopNotice(1L, "admin", 1L)
      }
    }
    test("setNoticePinned requires PUBLISHED") {
      val draft = NoticeModel(id = 1L, title = "草稿", content = "内容", status = NoticeStatus.DRAFT)
      every { noticeRepository.findById(1L) } returns java.util.Optional.of(draft)

      shouldThrow<InvalidNoticeStateTransitionException> {
        service.setNoticePinned(1L, "admin", 1L, true)
      }
    }
    test("setNoticePinned pins and audits") {
      val published = NoticeModel(id = 1L, title = "已发布", content = "内容", status = NoticeStatus.PUBLISHED)
      every { noticeRepository.findById(1L) } returns java.util.Optional.of(published)
      every { noticeRepository.save(any()) } answers { firstArg() }

      val pinned = service.setNoticePinned(1L, "admin", 1L, true)

      pinned.pinned shouldBe true
      verify {
        opsConfigAuditService.record(
          OpsConfigAuditService.TARGET_NOTICE,
          1L,
          OpsConfigAuditService.ACTION_NOTICE_PIN,
          1L,
          "admin",
          before = any(),
          after = any(),
          description = any(),
        )
      }
    }
    test("deleteNotice deletes any status") {
      val stopped = NoticeModel(id = 1L, title = "已停用", content = "内容", status = NoticeStatus.STOPPED)
      every { noticeRepository.findById(1L) } returns java.util.Optional.of(stopped)

      val result = service.deleteNotice(1L, "admin", 1L)

      result shouldBe true
      verify {
        opsConfigAuditService.record(
          OpsConfigAuditService.TARGET_NOTICE,
          1L,
          OpsConfigAuditService.ACTION_NOTICE_DELETE,
          1L,
          "admin",
          before = any(),
          after = any(),
          description = any(),
        )
      }
    }
    test("notice not found throws") {
      every { noticeRepository.findById(999L) } returns java.util.Optional.empty()

      shouldThrow<NoticeNotFoundException> {
        service.publishNotice(1L, "admin", 999L)
      }
    }
  })
