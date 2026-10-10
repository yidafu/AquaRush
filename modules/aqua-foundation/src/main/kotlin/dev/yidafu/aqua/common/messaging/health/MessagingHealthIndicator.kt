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

package dev.yidafu.aqua.common.messaging.health

import dev.yidafu.aqua.common.messaging.outbox.OutboxEventPublisher
import dev.yidafu.aqua.common.messaging.outbox.OutboxEventRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.stereotype.Component

/**
 * outbox 消息健康检查指示器(替代原 Artemis broker 健康检查)。
 *
 * - UP:携带积压明细(pendingDue/processing/oldestPendingAgeMs),可观测轮询滞后;
 * - DOWN:存在 DEAD 事件(重试耗尽,需人工排查)→ 健康告警出口
 *   (docs/compose/spec/outbox-messaging.md,不建 redrive API)。
 */
@Component
@ConditionalOnBean(OutboxEventPublisher::class)
class MessagingHealthIndicator(
  private val outboxEventRepository: OutboxEventRepository,
) : HealthIndicator {
  override fun health(): Health =
    try {
      val stats = outboxEventRepository.stats()
      val details =
        mapOf(
          "publisher" to
            mapOf(
              "name" to "OutboxEventPublisher",
              "type" to "OUTBOX",
              "available" to true,
            ),
          "pendingDue" to stats.pendingDue,
          "processing" to stats.processing,
          "dead" to stats.dead,
          "oldestPendingAgeMs" to stats.oldestPendingAgeMs,
        )
      if (stats.dead > 0) {
        Health.down().withDetails(details).build()
      } else {
        Health.up().withDetails(details).build()
      }
    } catch (e: Exception) {
      Health
        .down()
        .withDetail("error", e.message ?: "Unknown error")
        .withException(e)
        .build()
    }
}
