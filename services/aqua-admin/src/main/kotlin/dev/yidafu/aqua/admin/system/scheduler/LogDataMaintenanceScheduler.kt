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

package dev.yidafu.aqua.admin.system.scheduler

import dev.yidafu.aqua.api.service.ops.LogPartitionMaintenanceService
import dev.yidafu.aqua.api.service.ops.SystemConfigQueryService
import dev.yidafu.aqua.common.domain.model.SystemSettingKeys
import dev.yidafu.aqua.common.domain.model.enums.SystemSettingGroup
import dev.yidafu.aqua.logging.partition.domain.model.enums.LogTableType
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 数据维护调度器(F-SYS-006,仅 admin 服务)
 * - 每日 03:00 + 启动时:预建三类日志热表未来月度分区(当前月 / +1 / +2 月,幂等);
 * - 每日 03:30 + 启动时:按保留策略自动备份超期分区(转储冷表 + 导出文件,受 log_cleanup_enabled 开关控制)。
 */
@Component
class LogDataMaintenanceScheduler(
  private val logPartitionMaintenanceService: LogPartitionMaintenanceService,
  private val systemConfigQueryService: SystemConfigQueryService,
) {
  private val logger = LoggerFactory.getLogger(LogDataMaintenanceScheduler::class.java)

  /**
   * 每日凌晨 3:00 预建未来月度分区
   */
  @Scheduled(cron = "0 0 3 * * ?")
  fun ensurePartitions() {
    runCatching { logPartitionMaintenanceService.ensureFuturePartitions() }
      .onFailure { logger.error("日志分区预建失败", it) }
  }

  /**
   * 每日凌晨 3:30 自动备份超期分区(log_cleanup_enabled=false 时跳过)
   */
  @Scheduled(cron = "0 30 3 * * ?")
  fun autoBackupExpiredPartitions() {
    val settings = systemConfigQueryService.listByGroup(SystemSettingGroup.DATA_MAINTENANCE)
    val byKey = settings.associateBy { it.settingKey }
    val cleanupEnabled = byKey[SystemSettingKeys.LOG_CLEANUP_ENABLED]?.settingValue != "false"
    if (!cleanupEnabled) {
      logger.info("日志自动备份已关闭(log_cleanup_enabled=false),跳过本次调度")
      return
    }
    LogTableType.entries.forEach { table ->
      val retentionDays = byKey[table.retentionKey]?.settingValue?.toIntOrNull() ?: DEFAULT_RETENTION_DAYS
      runCatching { logPartitionMaintenanceService.backupExpiredPartitions(table, retentionDays) }
        .onFailure { logger.error("自动备份 {} 超期分区失败", table.tableName, it) }
    }
  }

  /**
   * 应用启动后补一次(预建 + 自动备份,保证新月份分区就绪并追补漏备份)
   */
  @EventListener(ApplicationReadyEvent::class)
  fun onStartup() {
    logger.info("数据维护调度:启动补跑分区预建与自动备份")
    ensurePartitions()
    autoBackupExpiredPartitions()
  }

  companion object {
    private const val DEFAULT_RETENTION_DAYS = 90
  }
}
