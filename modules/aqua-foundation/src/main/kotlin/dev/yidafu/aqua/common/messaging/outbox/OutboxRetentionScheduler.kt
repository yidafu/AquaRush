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

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * outbox DONE 行保留期清理(每日一次,分批限删,不做无界 DELETE)。
 * DEAD 行不清理 —— 保留给人工排查(docs/compose/spec/outbox-messaging.md)。
 */
@Component
@ConditionalOnProperty(name = ["aqua.outbox.enabled"], havingValue = "true", matchIfMissing = false)
class OutboxRetentionScheduler(
  private val outboxEventRepository: OutboxEventRepository,
  private val properties: OutboxProperties,
) {
  private val logger = LoggerFactory.getLogger(OutboxRetentionScheduler::class.java)

  @Scheduled(cron = "0 40 4 * * *")
  fun cleanupExpiredDoneRows() {
    try {
      val cutoff = LocalDateTime.now().minusDays(properties.doneRetentionDays)
      outboxEventRepository.deleteDoneBefore(cutoff = cutoff, limit = CLEANUP_BATCH_SIZE)
      logger.debug("outbox DONE 行清理完成: cutoff={}, batch={}", cutoff, CLEANUP_BATCH_SIZE)
    } catch (e: Exception) {
      logger.error("outbox DONE 行清理失败", e)
    }
  }

  companion object {
    private const val CLEANUP_BATCH_SIZE = 500
  }
}
