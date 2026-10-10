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
 * outbox 分区滚动维护(每日 04:40;docs/compose/spec/outbox-messaging.md 修订)。
 *
 * 职责(取代原 DONE 行删除任务):
 * 1. 补建「当前半区 + 下一半区」分区,承接未来约 6 个月的事件写入;
 * 2. 分区结束满 [OutboxProperties.partitionRetentionDays] 的分区 DETACH 并改名归档
 *    (`outbox_events_archive_YYYYHn`,数据不丢,主表恒小)。
 *
 * 双服务实例并发:事务级 advisory lock 串行化,未获锁本次跳过(另一实例在执行)。
 */
@Component
@ConditionalOnProperty(name = ["aqua.outbox.enabled"], havingValue = "true", matchIfMissing = false)
class OutboxPartitionScheduler(
  private val outboxEventRepository: OutboxEventRepository,
  private val properties: OutboxProperties,
) {
  private val logger = LoggerFactory.getLogger(OutboxPartitionScheduler::class.java)

  @Scheduled(cron = "0 40 4 * * *")
  fun maintainPartitions() {
    try {
      val result =
        outboxEventRepository.maintainPartitions(
          now = LocalDateTime.now(),
          retentionDays = properties.partitionRetentionDays,
        )
      if (result.lockAcquired) {
        logger.info(
          "outbox 分区维护完成: created={}, detached={}",
          result.createdPartitions,
          result.detachedArchives,
        )
      } else {
        logger.debug("outbox 分区维护锁被另一实例持有,本次跳过")
      }
    } catch (e: Exception) {
      logger.error("outbox 分区维护失败", e)
    }
  }
}
