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

import dev.yidafu.aqua.api.service.ops.NoticeQueryService
import dev.yidafu.aqua.common.domain.model.NoticeModel
import dev.yidafu.aqua.common.graphql.generated.NoticeListInput
import dev.yidafu.aqua.ops.notice.domain.repository.NoticeFilter
import dev.yidafu.aqua.ops.notice.domain.repository.NoticeRepository
import dev.yidafu.aqua.ops.notice.mapper.toDomain
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

/**
 * 公告查询服务实现(管理后台)
 * 列表按置顶优先、发布时间倒序(草稿无发布时间排最后)、创建时间倒序(AC-OPS-002-02)，
 * 排序由仓储层保证(原 `Sort.by(...)` 已随 JPA Specification 一并迁移进 [NoticeRepository.findPage])。
 */
@Service
class NoticeQueryServiceImpl(
  private val noticeRepository: NoticeRepository,
) : NoticeQueryService {
  override fun listNotices(input: NoticeListInput): Page<NoticeModel> {
    val filter =
      NoticeFilter(
        keyword = input.keyword,
        type = input.type?.toDomain(),
        status = input.status?.toDomain(),
      )
    val pageable = PageRequest.of(input.page ?: 0, input.size ?: 20)
    return noticeRepository.findPage(filter, pageable)
  }

  override fun getNotice(id: Long): NoticeModel? = noticeRepository.findById(id).orElse(null)
}
