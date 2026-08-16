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

package dev.yidafu.aqua.logging.partition.service.impl

import dev.yidafu.aqua.api.service.ops.LogPartitionMaintenanceService
import dev.yidafu.aqua.common.domain.model.LogBackupRecordModel
import dev.yidafu.aqua.logging.partition.domain.model.enums.LogTableType
import dev.yidafu.aqua.logging.partition.repository.LogPartitionRepository
import dev.yidafu.aqua.logging.partition.service.LogBackupService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.YearMonth

/**
 * 日志分区维护服务实现(F-SYS-006)
 * 分区预建(当前月 / +1 / +2 月,幂等)与自动备份(按保留天数,受清理开关控制由调度器把关)。
 */
@Service
class LogPartitionMaintenanceServiceImpl(
  private val logPartitionRepository: LogPartitionRepository,
  private val logBackupService: LogBackupService,
) : LogPartitionMaintenanceService {
  private val logger = LoggerFactory.getLogger(LogPartitionMaintenanceServiceImpl::class.java)

  override fun ensureFuturePartitions(monthsAhead: Int) {
    val now = YearMonth.now()
    LogTableType.entries.forEach { logTable ->
      if (!logPartitionRepository.isPartitioned(logTable)) {
        logger.warn("Log table {} is not partitioned, skip partition pre-creation", logTable.tableName)
        return@forEach
      }
      (0..monthsAhead).forEach { offset ->
        runCatching {
          logPartitionRepository.createMonthPartition(logTable, now.plusMonths(offset.toLong()).toString())
        }.onFailure { e ->
          logger.error("Failed to create partition for {} of {}: {}", now.plusMonths(offset.toLong()), logTable.tableName, e.message, e)
        }
      }
      logger.info("Ensured partitions for {} up to {}", logTable.tableName, now.plusMonths(monthsAhead.toLong()))
    }
  }

  override fun backupExpiredPartitions(
    logTable: LogTableType,
    retentionDays: Int,
    operatorName: String,
  ): List<LogBackupRecordModel> =
    logBackupService.backupExpiredPartitions(logTable, retentionDays, operatorName)
}
