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
import dev.yidafu.aqua.api.service.ops.NoticeMutationService
import dev.yidafu.aqua.common.domain.model.NoticeModel
import dev.yidafu.aqua.common.domain.model.enums.NoticeStatus
import dev.yidafu.aqua.ops.audit.OpsConfigAuditService
import dev.yidafu.aqua.ops.notice.domain.exception.InvalidNoticeStateTransitionException
import dev.yidafu.aqua.ops.notice.domain.exception.InvalidNoticeTimeWindowException
import dev.yidafu.aqua.ops.notice.domain.exception.NoticeNotFoundException
import dev.yidafu.aqua.ops.notice.domain.repository.NoticeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 公告变更服务实现(管理后台)
 * 生命周期(PRD 12-operations-config.md §F-OPS-002):新建(草稿) → 发布 → 置顶 → 停用 → 删除;
 * 已停用公告不可再编辑/发布/置顶(PRD 开放问题,采用最简规则);
 * 所有变更操作落审计日志(R-OPS-01:操作人/时间/前后值)。
 */
@Service
class NoticeMutationServiceImpl(
  private val noticeRepository: NoticeRepository,
  private val opsConfigAuditService: OpsConfigAuditService,
) : NoticeMutationService {
  @Transactional
  override fun createNotice(
    adminId: Long,
    adminName: String,
    request: CreateNoticeRequest,
  ): NoticeModel {
    validateTimeWindow(request.startAt, request.endAt)
    val notice =
      NoticeModel(
        title = request.title,
        content = request.content,
        type = request.type,
        pinned = false,
        startAt = request.startAt,
        endAt = request.endAt,
        status = NoticeStatus.DRAFT,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )
    val saved = noticeRepository.save(notice)
    opsConfigAuditService.record(
      targetType = OpsConfigAuditService.TARGET_NOTICE,
      targetId = saved.id,
      actionType = OpsConfigAuditService.ACTION_NOTICE_CREATE,
      operatorId = adminId,
      operatorName = adminName,
      after = noticeSnapshot(saved),
    )
    return saved
  }

  @Transactional
  override fun updateNotice(
    adminId: Long,
    adminName: String,
    id: Long,
    request: UpdateNoticeRequest,
  ): NoticeModel {
    val existing = noticeRepository.findById(id).orElseThrow { NoticeNotFoundException(id) }
    if (existing.status == NoticeStatus.STOPPED) {
      throw InvalidNoticeStateTransitionException(id, existing.status, NoticeStatus.DRAFT.name)
    }
    validateTimeWindow(request.startAt, request.endAt)

    val before = noticeSnapshot(existing)
    existing.apply {
      title = request.title
      content = request.content
      this.type = request.type
      pinned = request.pinned
      startAt = request.startAt
      endAt = request.endAt
    }
    val saved = noticeRepository.save(existing)
    opsConfigAuditService.record(
      targetType = OpsConfigAuditService.TARGET_NOTICE,
      targetId = saved.id,
      actionType = OpsConfigAuditService.ACTION_NOTICE_UPDATE,
      operatorId = adminId,
      operatorName = adminName,
      before = before,
      after = noticeSnapshot(saved),
    )
    return saved
  }

  @Transactional
  override fun publishNotice(
    adminId: Long,
    adminName: String,
    id: Long,
  ): NoticeModel {
    val existing = noticeRepository.findById(id).orElseThrow { NoticeNotFoundException(id) }
    if (existing.status != NoticeStatus.DRAFT && existing.status != NoticeStatus.PUBLISHED) {
      throw InvalidNoticeStateTransitionException(id, existing.status, NoticeStatus.PUBLISHED.name)
    }
    val before = noticeSnapshot(existing)
    // 每次发布重置发布时间,保证「发布时间倒序」排序在停用→重新发布后依然有意义(AC-OPS-002-02)
    existing.status = NoticeStatus.PUBLISHED
    existing.publishedAt = LocalDateTime.now()
    val saved = noticeRepository.save(existing)
    opsConfigAuditService.record(
      targetType = OpsConfigAuditService.TARGET_NOTICE,
      targetId = saved.id,
      actionType = OpsConfigAuditService.ACTION_NOTICE_PUBLISH,
      operatorId = adminId,
      operatorName = adminName,
      before = before,
      after = noticeSnapshot(saved),
    )
    return saved
  }

  @Transactional
  override fun stopNotice(
    adminId: Long,
    adminName: String,
    id: Long,
  ): NoticeModel {
    val existing = noticeRepository.findById(id).orElseThrow { NoticeNotFoundException(id) }
    if (existing.status != NoticeStatus.PUBLISHED) {
      throw InvalidNoticeStateTransitionException(id, existing.status, NoticeStatus.STOPPED.name)
    }
    val before = noticeSnapshot(existing)
    existing.status = NoticeStatus.STOPPED
    existing.pinned = false
    val saved = noticeRepository.save(existing)
    opsConfigAuditService.record(
      targetType = OpsConfigAuditService.TARGET_NOTICE,
      targetId = saved.id,
      actionType = OpsConfigAuditService.ACTION_NOTICE_STOP,
      operatorId = adminId,
      operatorName = adminName,
      before = before,
      after = noticeSnapshot(saved),
    )
    return saved
  }

  @Transactional
  override fun setNoticePinned(
    adminId: Long,
    adminName: String,
    id: Long,
    pinned: Boolean,
  ): NoticeModel {
    val existing = noticeRepository.findById(id).orElseThrow { NoticeNotFoundException(id) }
    if (existing.status != NoticeStatus.PUBLISHED) {
      throw InvalidNoticeStateTransitionException(id, existing.status, if (pinned) "PIN" else "UNPIN")
    }
    val before = noticeSnapshot(existing)
    existing.pinned = pinned
    val saved = noticeRepository.save(existing)
    opsConfigAuditService.record(
      targetType = OpsConfigAuditService.TARGET_NOTICE,
      targetId = saved.id,
      actionType = if (pinned) OpsConfigAuditService.ACTION_NOTICE_PIN else OpsConfigAuditService.ACTION_NOTICE_UNPIN,
      operatorId = adminId,
      operatorName = adminName,
      before = before,
      after = noticeSnapshot(saved),
    )
    return saved
  }

  @Transactional
  override fun deleteNotice(
    adminId: Long,
    adminName: String,
    id: Long,
  ): Boolean {
    val existing = noticeRepository.findById(id).orElseThrow { NoticeNotFoundException(id) }
    val before = noticeSnapshot(existing)
    noticeRepository.delete(existing)
    opsConfigAuditService.record(
      targetType = OpsConfigAuditService.TARGET_NOTICE,
      targetId = id,
      actionType = OpsConfigAuditService.ACTION_NOTICE_DELETE,
      operatorId = adminId,
      operatorName = adminName,
      before = before,
    )
    return true
  }

  private fun validateTimeWindow(
    startAt: LocalDateTime?,
    endAt: LocalDateTime?,
  ) {
    if (startAt != null && endAt != null && !startAt.isBefore(endAt)) {
      throw InvalidNoticeTimeWindowException("时间窗不合法:开始时间必须早于结束时间")
    }
  }

  private fun noticeSnapshot(notice: NoticeModel): Map<String, Any?> =
    linkedMapOf(
      "title" to notice.title,
      "content" to notice.content,
      "type" to notice.type.name,
      "pinned" to notice.pinned,
      "startAt" to notice.startAt,
      "endAt" to notice.endAt,
      "status" to notice.status.name,
      "publishedAt" to notice.publishedAt,
    )
}
