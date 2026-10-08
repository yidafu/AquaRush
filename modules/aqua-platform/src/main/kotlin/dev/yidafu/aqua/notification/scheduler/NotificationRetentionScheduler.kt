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

package dev.yidafu.aqua.notification.scheduler

import dev.yidafu.aqua.api.service.ops.SystemConfigQueryApiService
import dev.yidafu.aqua.common.domain.model.SystemSettingKeys
import dev.yidafu.aqua.notification.domain.repository.NotificationRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * 站内消息保留期清理调度器(R-NTF-04)
 * 保留时长可后台配置(notification_retention_days,默认 90 天);每日凌晨清理超期消息。
 */
@Component
class NotificationRetentionScheduler(
  private val notificationRepository: NotificationRepository,
  private val systemConfigQueryApiService: SystemConfigQueryApiService,
) {
  private val logger = LoggerFactory.getLogger(NotificationRetentionScheduler::class.java)

  @Scheduled(cron = "0 30 3 * * *")
  fun cleanupExpiredNotifications() {
    val retentionDays = systemConfigQueryApiService.getLongValue(SystemSettingKeys.NOTIFICATION_RETENTION_DAYS, 90L)
    val before = LocalDateTime.now().minusDays(retentionDays)
    val deleted = notificationRepository.deleteExpired(before)
    if (deleted > 0) {
      logger.info("已清理超期站内消息 {} 条(保留 {} 天)", deleted, retentionDays)
    }
  }
}
