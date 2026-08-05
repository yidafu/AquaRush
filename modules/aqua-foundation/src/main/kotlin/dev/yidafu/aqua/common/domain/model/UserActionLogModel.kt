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

import jakarta.persistence.*
import java.time.LocalDateTime
import dev.yidafu.aqua.common.annotation.SnowflakeIdGenerator

/**
 * 用户操作日志实体类
 * 用于记录用户在系统中的操作行为
 */
@Entity
@Table(
  name = "user_action_logs",
  indexes = [
    Index(name = "idx_user_action_logs_user_id", columnList = "user_id"),
    Index(name = "idx_user_action_logs_action_type", columnList = "action_type"),
    Index(name = "idx_user_action_logs_created_at", columnList = "created_at"),
  ],
)
class UserActionLogModel(
  @Id
  @SnowflakeIdGenerator
  @Column(name = "id", nullable = false, updatable = false)
  var id: Long? = null,
  @Column(name = "user_id", length = 50)
  val userId: String? = null,
  @Column(name = "username", length = 100)
  val username: String? = null,
  @Column(name = "action_type", nullable = false, length = 50)
  val actionType: String = "UNKNOWN",
  @Column(name = "target", length = 500)
  val target: String? = null,
  @Column(name = "page_url", length = 1000)
  val pageUrl: String? = null,
  @Column(name = "element_id", length = 100)
  val elementId: String? = null,
  @Column(name = "element_type", length = 50)
  val elementType: String? = null,
  @Column(name = "element_text", length = 200)
  val elementText: String? = null,
  @Column(name = "client_ip", length = 50)
  val clientIp: String? = null,
  @Column(name = "user_agent", length = 500)
  val userAgent: String? = null,
  @Column(name = "properties", columnDefinition = "text")
  val properties: String? = null,
  @Column(name = "created_at", nullable = false, updatable = false)
  val createdAt: LocalDateTime = LocalDateTime.now(),
)
