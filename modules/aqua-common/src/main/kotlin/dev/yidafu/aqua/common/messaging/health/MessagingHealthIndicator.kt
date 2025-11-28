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

package dev.yidafu.aqua.common.messaging.health

import dev.yidafu.aqua.common.messaging.publisher.EventPublisher
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.stereotype.Component

/**
 * Spring Messaging健康检查指示器
 * 提供消息队列系统的健康状态信息
 */
@Component
@ConditionalOnBean(EventPublisher::class)
class MessagingHealthIndicator(
  private val eventPublisher: EventPublisher,
) : HealthIndicator {
  override fun health(): Health =
    try {
      Health
        .up()
        .withDetail(
          "publisher",
          mapOf(
            "name" to eventPublisher.getName(),
            "type" to eventPublisher.getType().name,
            "available" to eventPublisher.isAvailable(),
            "priority" to eventPublisher.getPriority(),
          ),
        ).build()
    } catch (e: Exception) {
      Health
        .down()
        .withDetail("error", e.message ?: "Unknown error")
        .withException(e)
    } as Health
}
