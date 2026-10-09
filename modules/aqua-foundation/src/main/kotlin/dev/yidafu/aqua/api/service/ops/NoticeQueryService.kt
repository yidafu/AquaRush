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

import dev.yidafu.aqua.common.domain.model.NoticeModel
import dev.yidafu.aqua.common.graphql.generated.NoticeListInput
import org.springframework.data.domain.Page

/**
 * 公告查询服务接口(管理后台)
 * 与 PRD 12-operations-config.md §F-OPS-002 保持一致(置顶优先、发布时间倒序)。
 */
interface NoticeQueryService {
  /**
   * 分页查询公告列表(置顶优先,再按发布时间倒序,草稿排最后)
   */
  fun listNotices(input: NoticeListInput): Page<NoticeModel>

  /**
   * 根据 ID 查询公告
   */
  fun getNotice(id: Long): NoticeModel?
}
