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

package dev.yidafu.aqua.common.domain.model

import java.time.LocalDateTime

/**
 * 用户操作日志实体类
 * 用于记录用户在系统中的操作行为
 */
class UserActionLogModel(
  var id: Long? = null,
  val userId: String? = null,
  val username: String? = null,
  val actionType: String = "UNKNOWN",
  val target: String? = null,
  val pageUrl: String? = null,
  val elementId: String? = null,
  val elementType: String? = null,
  val elementText: String? = null,
  val clientIp: String? = null,
  val userAgent: String? = null,
  val properties: String? = null,
  val createdAt: LocalDateTime = LocalDateTime.now(),
)
