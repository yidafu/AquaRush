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

package dev.yidafu.aqua.common.messaging.outbox

import java.time.LocalDateTime

/** outbox 行状态(PENDING → PROCESSING → DONE/DEAD)。 */
object OutboxStatus {
  const val PENDING = "PENDING"
  const val PROCESSING = "PROCESSING"
  const val DONE = "DONE"
  const val DEAD = "DEAD"
}

/** 非领域事件类型的占位 event_type(如 WebSocket 推送广播行)。 */
object OutboxEventTypes {
  const val PUSH_EVENT = "PUSH_EVENT"
}

/**
 * outbox 行快照(认领返回)。
 */
data class OutboxEventRecord(
  val id: Long,
  val eventType: String,
  val destination: String,
  val subscriber: String?,
  val payload: String,
  val nextRunAt: LocalDateTime,
  val status: String,
  val retryCount: Int,
)

/** outbox 健康统计(健康指标用,全部为聚合计数,无明细扫描)。 */
data class OutboxStats(
  val pendingDue: Long,
  val processing: Long,
  val dead: Long,
  val oldestPendingAgeMs: Long,
)
