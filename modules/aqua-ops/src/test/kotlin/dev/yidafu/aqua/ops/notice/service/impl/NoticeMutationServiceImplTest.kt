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
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 公告变更服务单元测试(F-OPS-002 核心规则:状态机 草稿→发布→停用、置顶仅限已发布、时间窗合法、审计留痕)
 */
class NoticeMutationServiceImplTest {
  private lateinit var noticeRepository: NoticeRepository
  private lateinit var opsConfigAuditService: OpsConfigAuditService
  private lateinit var service: NoticeMutationServiceImpl

  private fun createRequest(
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

  @BeforeEach
  fun setUp() {
    noticeRepository = mockk(relaxed = true)
    opsConfigAuditService = mockk(relaxed = true)
    service = NoticeMutationServiceImpl(noticeRepository, opsConfigAuditService)
  }

  @Test
  @DisplayName("createNotice 默认草稿状态且未置顶")
  fun `createNotice defaults to DRAFT and unpinned`() {
    every { noticeRepository.save(any()) } answers { firstArg() }

    val created = service.createNotice(1L, "admin", createRequest())

    assertEquals(NoticeStatus.DRAFT, created.status)
    assertFalse(created.pinned)
    assertEquals(NoticeType.PLATFORM_NOTICE, created.type)
  }

  @Test
  @DisplayName("createNotice 落审计日志(CREATE)")
  fun `createNotice writes audit`() {
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

  @Test
  @DisplayName("createNotice 时间窗开始 >= 结束抛 InvalidNoticeTimeWindowException")
  fun `createNotice rejects invalid time window`() {
    assertFailsWith<InvalidNoticeTimeWindowException> {
      service.createNotice(
        1L,
        "admin",
        createRequest(startAt = LocalDateTime.of(2026, 8, 12, 12, 0), endAt = LocalDateTime.of(2026, 8, 12, 10, 0)),
      )
    }
  }

  @Test
  @DisplayName("publishNotice 从草稿发布:状态=已发布且发布时间非空")
  fun `publishNotice transitions DRAFT to PUBLISHED`() {
    val draft = NoticeModel(id = 1L, title = "草稿", content = "内容", status = NoticeStatus.DRAFT)
    every { noticeRepository.findById(1L) } returns java.util.Optional.of(draft)
    every { noticeRepository.save(any()) } answers { firstArg() }

    val published = service.publishNotice(1L, "admin", 1L)

    assertEquals(NoticeStatus.PUBLISHED, published.status)
    assertNotNull(published.publishedAt)
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

  @Test
  @DisplayName("publishNotice 已停用公告抛 InvalidNoticeStateTransitionException")
  fun `publishNotice rejects STOPPED`() {
    val stopped = NoticeModel(id = 1L, title = "已停用", content = "内容", status = NoticeStatus.STOPPED)
    every { noticeRepository.findById(1L) } returns java.util.Optional.of(stopped)

    assertFailsWith<InvalidNoticeStateTransitionException> {
      service.publishNotice(1L, "admin", 1L)
    }
  }

  @Test
  @DisplayName("updateNotice 已停用公告不可编辑")
  fun `updateNotice rejects STOPPED`() {
    val stopped = NoticeModel(id = 1L, title = "已停用", content = "内容", status = NoticeStatus.STOPPED)
    every { noticeRepository.findById(1L) } returns java.util.Optional.of(stopped)

    assertFailsWith<InvalidNoticeStateTransitionException> {
      service.updateNotice(1L, "admin", 1L, UpdateNoticeRequest("新标题", "新内容", NoticeType.WATER_OUTAGE))
    }
  }

  @Test
  @DisplayName("updateNotice 编辑已发布公告并落审计(含前后值)")
  fun `updateNotice updates PUBLISHED with audit`() {
    val published = NoticeModel(id = 1L, title = "旧标题", content = "旧内容", status = NoticeStatus.PUBLISHED)
    every { noticeRepository.findById(1L) } returns java.util.Optional.of(published)
    every { noticeRepository.save(any()) } answers { firstArg() }

    val updated =
      service.updateNotice(1L, "admin", 1L, UpdateNoticeRequest("新标题", "新内容", NoticeType.WATER_OUTAGE))

    assertEquals("新标题", updated.title)
    assertEquals(NoticeType.WATER_OUTAGE, updated.type)
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

  @Test
  @DisplayName("stopNotice 停用已发布公告并取消置顶")
  fun `stopNotice stops PUBLISHED and unpins`() {
    val published = NoticeModel(id = 1L, title = "已发布", content = "内容", status = NoticeStatus.PUBLISHED, pinned = true)
    every { noticeRepository.findById(1L) } returns java.util.Optional.of(published)
    every { noticeRepository.save(any()) } answers { firstArg() }

    val stopped = service.stopNotice(1L, "admin", 1L)

    assertEquals(NoticeStatus.STOPPED, stopped.status)
    assertFalse(stopped.pinned)
  }

  @Test
  @DisplayName("stopNotice 草稿公告不可停用")
  fun `stopNotice rejects DRAFT`() {
    val draft = NoticeModel(id = 1L, title = "草稿", content = "内容", status = NoticeStatus.DRAFT)
    every { noticeRepository.findById(1L) } returns java.util.Optional.of(draft)

    assertFailsWith<InvalidNoticeStateTransitionException> {
      service.stopNotice(1L, "admin", 1L)
    }
  }

  @Test
  @DisplayName("setNoticePinned 仅已发布公告可置顶")
  fun `setNoticePinned requires PUBLISHED`() {
    val draft = NoticeModel(id = 1L, title = "草稿", content = "内容", status = NoticeStatus.DRAFT)
    every { noticeRepository.findById(1L) } returns java.util.Optional.of(draft)

    assertFailsWith<InvalidNoticeStateTransitionException> {
      service.setNoticePinned(1L, "admin", 1L, true)
    }
  }

  @Test
  @DisplayName("setNoticePinned 置顶/取消置顶落对应审计动作")
  fun `setNoticePinned pins and audits`() {
    val published = NoticeModel(id = 1L, title = "已发布", content = "内容", status = NoticeStatus.PUBLISHED)
    every { noticeRepository.findById(1L) } returns java.util.Optional.of(published)
    every { noticeRepository.save(any()) } answers { firstArg() }

    val pinned = service.setNoticePinned(1L, "admin", 1L, true)

    assertTrue(pinned.pinned)
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

  @Test
  @DisplayName("deleteNotice 任意状态可删除并落审计")
  fun `deleteNotice deletes any status`() {
    val stopped = NoticeModel(id = 1L, title = "已停用", content = "内容", status = NoticeStatus.STOPPED)
    every { noticeRepository.findById(1L) } returns java.util.Optional.of(stopped)

    val result = service.deleteNotice(1L, "admin", 1L)

    assertTrue(result)
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

  @Test
  @DisplayName("公告不存在抛 NoticeNotFoundException")
  fun `notice not found throws`() {
    every { noticeRepository.findById(999L) } returns java.util.Optional.empty()

    assertFailsWith<NoticeNotFoundException> {
      service.publishNotice(1L, "admin", 999L)
    }
  }
}
