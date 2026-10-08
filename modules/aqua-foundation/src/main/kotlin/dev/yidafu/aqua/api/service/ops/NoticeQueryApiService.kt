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

/**
 * 公告查询服务接口(用户端)
 * 返回已发布且处于时间窗内的公告,置顶优先、发布时间倒序(PRD 12-operations-config.md §F-OPS-002 / AC-OPS-002-02);
 * 停用/时间窗过期自动下架。
 */
interface NoticeQueryApiService {
  /**
   * 获取当前生效的公告列表
   */
  fun getActiveNotices(): List<NoticeModel>
}
