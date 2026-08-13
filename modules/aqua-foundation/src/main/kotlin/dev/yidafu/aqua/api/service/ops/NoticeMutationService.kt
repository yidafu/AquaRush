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

package dev.yidafu.aqua.api.service.ops

import dev.yidafu.aqua.api.dto.CreateNoticeRequest
import dev.yidafu.aqua.api.dto.UpdateNoticeRequest
import dev.yidafu.aqua.common.domain.model.NoticeModel

/**
 * 公告变更服务接口(管理后台)
 * 操作人显式传参,由实现落审计日志(R-OPS-01)。
 * 生命周期:新建(草稿) → 编辑 → 发布 → 置顶 → 停用 → 删除(PRD 12-operations-config.md §F-OPS-002)。
 */
interface NoticeMutationService {
  /**
   * 新建公告(默认草稿状态,未置顶)
   */
  fun createNotice(
    adminId: Long,
    adminName: String,
    request: CreateNoticeRequest,
  ): NoticeModel

  /**
   * 编辑公告(全量更新;已停用公告不可编辑)
   */
  fun updateNotice(
    adminId: Long,
    adminName: String,
    id: Long,
    request: UpdateNoticeRequest,
  ): NoticeModel

  /**
   * 发布公告(草稿/已发布 → 已发布,重置发布时间为当前时间)
   */
  fun publishNotice(
    adminId: Long,
    adminName: String,
    id: Long,
  ): NoticeModel

  /**
   * 停用公告(已发布 → 已停用,同时取消置顶)
   */
  fun stopNotice(
    adminId: Long,
    adminName: String,
    id: Long,
  ): NoticeModel

  /**
   * 置顶/取消置顶(仅已发布公告可操作)
   */
  fun setNoticePinned(
    adminId: Long,
    adminName: String,
    id: Long,
    pinned: Boolean,
  ): NoticeModel

  /**
   * 删除公告(软删除,任意状态均可)
   */
  fun deleteNotice(
    adminId: Long,
    adminName: String,
    id: Long,
  ): Boolean
}
