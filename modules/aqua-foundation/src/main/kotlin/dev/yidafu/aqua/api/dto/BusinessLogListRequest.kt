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

package dev.yidafu.aqua.api.dto

import jakarta.validation.constraints.Min
import java.time.LocalDateTime

/**
 * 业务日志分页查询请求(F-LOG-003:业务域/操作类型维度筛选)
 */
data class BusinessLogListRequest(
  /** 业务域枚举名(ORDER/PAYMENT/DELIVERY/USER/AUTH),映射 logger_name 模糊筛选 */
  val domain: String? = null,
  /** 操作类型,精确匹配 operation 列 */
  val operationType: String? = null,
  /** 日志级别(INFO/WARN/ERROR/DEBUG/TRACE) */
  val level: String? = null,
  val userId: Long? = null,
  val startTime: LocalDateTime? = null,
  val endTime: LocalDateTime? = null,
  @field:Min(value = 0, message = "页码不能小于0")
  val page: Int = 0,
  @field:Min(value = 1, message = "每页条数必须大于0")
  val size: Int = 10,
)
