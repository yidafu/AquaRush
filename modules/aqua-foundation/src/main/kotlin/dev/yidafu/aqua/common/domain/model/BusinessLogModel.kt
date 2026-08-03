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

/**
 * 业务日志实体类
 * 用于记录SLF4J业务日志
 */
@Entity
@Table(
  name = "business_logs",
  indexes = [
    Index(name = "idx_business_logs_correlation_id", columnList = "correlation_id"),
    Index(name = "idx_business_logs_level", columnList = "level"),
    Index(name = "idx_business_logs_created_at", columnList = "created_at"),
    Index(name = "idx_business_logs_user_id", columnList = "user_id"),
    Index(name = "idx_business_logs_logger_name", columnList = "logger_name"),
  ],
)
class BusinessLogModel(
  @Id
  @SnowflakeIdGenerator
  var id: Long? = null,
  @Column(name = "correlation_id", length = 50)
  val correlationId: String? = null,
  @Column(name = "level", nullable = false, length = 10)
  val level: String = "INFO",
  @Column(name = "logger_name", length = 200)
  val loggerName: String? = null,
  @Column(name = "message", columnDefinition = "text")
  val message: String? = null,
  @Column(name = "stack_trace", columnDefinition = "text")
  val stackTrace: String? = null,
  @Column(name = "user_id")
  val userId: Long? = null,
  @Column(name = "username", length = 100)
  val username: String? = null,
  @Column(name = "created_at", nullable = false, updatable = false)
  val createdAt: LocalDateTime = LocalDateTime.now(),
)
