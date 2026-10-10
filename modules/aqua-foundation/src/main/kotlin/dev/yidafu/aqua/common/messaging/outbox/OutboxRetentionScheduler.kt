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
      var total = 0
      // 循环分批直到删空(或达单次运行上限),避免日事件量超过单批容量导致积压
      while (total < MAX_ROWS_PER_RUN) {
        val deleted = outboxEventRepository.deleteDoneBefore(cutoff = cutoff, limit = CLEANUP_BATCH_SIZE)
        total += deleted
        if (deleted < CLEANUP_BATCH_SIZE) break
      }
      logger.debug("outbox DONE 行清理完成: cutoff={}, deleted={}", cutoff, total)
    } catch (e: Exception) {
      logger.error("outbox DONE 行清理失败", e)
    }
  }

  companion object {
    private const val CLEANUP_BATCH_SIZE = 500

    /** 单次运行删除上限(500×200=10万行),剩余留到次日,防止清理任务长期占用连接。 */
    private const val MAX_ROWS_PER_RUN = 100_000
  }
}
