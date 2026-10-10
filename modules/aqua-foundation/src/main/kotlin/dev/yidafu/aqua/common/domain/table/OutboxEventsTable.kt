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

package dev.yidafu.aqua.common.domain.table

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.datetime

// Transactional outbox 表(spec/tech/design/outbox-messaging.md,替代 Artemis MQ)。
// 列定义对齐 changelog V320RecreateOutboxEvents。
// subscriber:NULL = 竞争消费(任一服务认领);非空 = 指定服务独享(广播扇出行)。
// next_run_at = 可认领时刻,延迟投递写未来时间;主键为 Snowflake(应用侧生成)。

/** 事件 outbox(`outbox_events`)。 */
object OutboxEventsTable : Table("outbox_events") {
  val id = long("id")
  val eventType = varchar("event_type", 100)
  val destination = varchar("destination", 50)
  val subscriber = varchar("subscriber", 20).nullable()
  val payload = text("payload")
  val nextRunAt = datetime("next_run_at")
  val status = varchar("status", 20)
  val retryCount = integer("retry_count").default(0)
  val errorMessage = text("error_message").nullable()
  val lockedBy = varchar("locked_by", 64).nullable()
  val lockedAt = datetime("locked_at").nullable()
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
  override val primaryKey = PrimaryKey(id)
}
