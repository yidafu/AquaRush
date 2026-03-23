/*
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

package dev.yidafu.aqua.logging.repository

import dev.yidafu.aqua.common.domain.model.UserActionLogModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime

/**
 * 用户操作日志Repository自定义查询接口
 */
interface UserActionLogRepositoryCustom {
  /**
   * 根据多个条件分页查询用户操作日志
   */
  fun findByFilters(
    userId: String?,
    actionType: String?,
    username: String?,
    startTime: LocalDateTime?,
    endTime: LocalDateTime?,
    pageable: Pageable,
  ): Page<UserActionLogModel>
}
