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

import dev.yidafu.aqua.api.service.ops.NoticeQueryApiService
import dev.yidafu.aqua.common.domain.model.NoticeModel
import dev.yidafu.aqua.ops.notice.domain.repository.NoticeRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * 公告查询服务实现(用户端)
 * 返回已发布且处于时间窗内的公告,置顶优先、发布时间倒序(R-OPS-01:配置即展示、停用/到期自动下架)。
 */
@Service
class NoticeQueryApiServiceImpl(
  private val noticeRepository: NoticeRepository,
) : NoticeQueryApiService {
  override fun getActiveNotices(): List<NoticeModel> = noticeRepository.findActiveNotices(LocalDateTime.now())
}
