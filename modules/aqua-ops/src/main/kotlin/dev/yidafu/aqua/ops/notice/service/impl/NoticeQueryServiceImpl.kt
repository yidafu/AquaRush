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

import dev.yidafu.aqua.api.dto.NoticeListRequest
import dev.yidafu.aqua.api.service.ops.NoticeQueryService
import dev.yidafu.aqua.common.domain.model.NoticeModel
import dev.yidafu.aqua.ops.notice.domain.repository.NoticeRepository
import dev.yidafu.aqua.ops.notice.domain.repository.NoticeSpecifications
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service

/**
 * 公告查询服务实现(管理后台)
 * 列表按置顶优先、发布时间倒序(草稿无发布时间排最后)、创建时间倒序(AC-OPS-002-02)。
 */
@Service
class NoticeQueryServiceImpl(
  private val noticeRepository: NoticeRepository,
) : NoticeQueryService {
  override fun listNotices(request: NoticeListRequest): Page<NoticeModel> {
    var spec: Specification<NoticeModel> = Specification { _, _, cb -> cb.conjunction() }
    request.keyword?.takeIf { it.isNotBlank() }?.let { spec = spec.and(NoticeSpecifications.keywordContains(it)) }
    request.type?.let { spec = spec.and(NoticeSpecifications.typeEquals(it)) }
    request.status?.let { spec = spec.and(NoticeSpecifications.statusEquals(it)) }
    val pageable =
      PageRequest.of(
        request.page,
        request.size,
        Sort.by(
          Sort.Order.desc("pinned"),
          Sort.Order.desc("publishedAt").nullsLast(),
          Sort.Order.desc("createdAt"),
        ),
      )
    return noticeRepository.findAll(spec, pageable)
  }

  override fun getNotice(id: Long): NoticeModel? = noticeRepository.findById(id).orElse(null)
}
