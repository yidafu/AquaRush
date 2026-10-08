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

package dev.yidafu.aqua.logging.partition.service

import dev.yidafu.aqua.common.domain.model.LogBackupRecordModel
import dev.yidafu.aqua.logging.partition.config.DataMaintenanceProperties
import dev.yidafu.aqua.logging.partition.domain.LogPartitionRules
import dev.yidafu.aqua.logging.partition.domain.exception.BackupExecutionException
import dev.yidafu.aqua.logging.partition.domain.exception.LogPartitionAlreadyBackedUpException
import dev.yidafu.aqua.logging.partition.domain.exception.LogPartitionBackupFailedException
import dev.yidafu.aqua.logging.partition.domain.exception.LogPartitionBackupInProgressException
import dev.yidafu.aqua.logging.partition.domain.exception.LogPartitionNotBackupableException
import dev.yidafu.aqua.logging.partition.domain.exception.LogPartitionNotFoundException
import dev.yidafu.aqua.logging.partition.domain.model.LogPartitionInfo
import dev.yidafu.aqua.logging.partition.domain.model.enums.LogBackupStatus
import dev.yidafu.aqua.logging.partition.domain.model.enums.LogTableType
import dev.yidafu.aqua.logging.partition.domain.repository.LogBackupRecordRepository
import dev.yidafu.aqua.logging.partition.repository.LogPartitionRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime

/**
 * 日志分区离线备份服务(F-SYS-006)
 * 超期分区备份 = 转储冷表(按年份组织 log_archive.{table}_{yyyy})+ 导出 gzip 数据文件,
 * 成功后分区从热表移除;任何执行失败数据仍在热表(导出/并入均在 DROP 前完成),并记录 FAILED。
 */
@Service
class LogBackupService(
  private val logPartitionRepository: LogPartitionRepository,
  private val logBackupRecordRepository: LogBackupRecordRepository,
  private val logBackupTransactionService: LogBackupTransactionService,
  private val dataMaintenanceProperties: DataMaintenanceProperties,
) {
  private val logger = LoggerFactory.getLogger(LogBackupService::class.java)

  /**
   * 备份指定超期月分区(手动/自动共用入口)
   */
  fun backup(
    logTable: LogTableType,
    partitionName: String,
    operatorId: Long?,
    operatorName: String,
    retentionDays: Int,
  ): LogBackupRecordModel {
    val partition =
      logPartitionRepository.listPartitions(logTable).find { it.partitionName == partitionName }
        ?: throw LogPartitionNotFoundException(partitionName)
    validate(partition, retentionDays)

    return try {
      logBackupTransactionService.doBackup(partition, operatorId, operatorName)
    } catch (e: LogPartitionBackupInProgressException) {
      throw e
    } catch (e: BackupExecutionException) {
      val file = backupFile(partitionName)
      if (!e.exportCompleted) {
        runCatching { Files.deleteIfExists(file) }
      }
      val cause = e.cause ?: e
      recordFailed(partition, operatorId, operatorName, cause, if (e.exportCompleted) file.toString() else null)
      throw LogPartitionBackupFailedException("日志分区备份失败:${cause.message}", cause)
    }
  }

  /**
   * 自动备份指定日志表全部超期分区(单分区失败记录日志并继续其余)
   */
  fun backupExpiredPartitions(
    logTable: LogTableType,
    retentionDays: Int,
    operatorName: String,
  ): List<LogBackupRecordModel> {
    val done = mutableListOf<LogBackupRecordModel>()
    logPartitionRepository.listPartitions(logTable).forEach { partition ->
      if (!LogPartitionRules.isBackupable(partition, retentionDays)) return@forEach
      if (logBackupRecordRepository.existsByPartitionNameAndStatusIn(partition.partitionName, ACTIVE_STATUSES)) return@forEach
      try {
        done += backup(logTable, partition.partitionName, operatorId = null, operatorName = operatorName, retentionDays = retentionDays)
      } catch (e: Exception) {
        logger.error("Auto backup failed for partition {} of {}: {}", partition.partitionName, logTable.tableName, e.message, e)
      }
    }
    return done
  }

  /**
   * 校验:存在 / 月度分区(非 DEFAULT)/ 非当前月 / 已超期 / 未备份
   */
  private fun validate(
    partition: LogPartitionInfo,
    retentionDays: Int,
  ) {
    val month = partition.partitionMonth ?: throw LogPartitionNotBackupableException("DEFAULT 兜底分区不可备份")
    if (partition.isCurrentMonth) throw LogPartitionNotBackupableException("当前月分区仍在写入,不可备份")
    if (!LogPartitionRules.isExpired(partition, retentionDays)) {
      throw LogPartitionNotBackupableException("分区 $month 未达到保留期($retentionDays 天)")
    }
    if (logBackupRecordRepository.existsByPartitionNameAndStatusIn(partition.partitionName, ACTIVE_STATUSES)) {
      throw LogPartitionAlreadyBackedUpException(partition.partitionName)
    }
  }

  /**
   * 记 FAILED 记录(独立事务;导出文件完整时保留并记路径)
   */
  private fun recordFailed(
    partition: LogPartitionInfo,
    operatorId: Long?,
    operatorName: String,
    cause: Throwable,
    filePath: String?,
  ): LogBackupRecordModel =
    logBackupRecordRepository.save(
      LogBackupRecordModel(
        logTable = partition.logTable,
        partitionName = partition.partitionName,
        partitionMonth = partition.partitionMonth ?: "",
        coldTableName = coldTableName(partition),
        fromBound = partition.fromBound,
        toBound = partition.toBound,
        filePath = filePath,
        status = LogBackupStatus.FAILED,
        errorMessage = cause.message?.take(2000),
        operatorId = operatorId,
        operatorName = operatorName,
        startedAt = LocalDateTime.now(),
        finishedAt = LocalDateTime.now(),
      ),
    )

  /** 备份文件路径:{backupDir}/{partitionName}.csv.gz */
  fun backupFile(partitionName: String): Path = backupFileOf(dataMaintenanceProperties, partitionName)

  /** 冷表名:log_archive.{table}_{yyyy} */
  private fun coldTableName(partition: LogPartitionInfo): String {
    val year = LogPartitionRules.yearOfPartition(partition.partitionName) ?: return "${partition.logTable.tableName}_archive"
    return "log_archive.${partition.logTable.tableName}_$year"
  }

  companion object {
    private val ACTIVE_STATUSES = listOf(LogBackupStatus.RUNNING, LogBackupStatus.SUCCEEDED)

    /** 备份文件路径:{backupDir}/{partitionName}.csv.gz */
    fun backupFileOf(
      properties: DataMaintenanceProperties,
      partitionName: String,
    ): Path = Path.of(properties.backupDir, "$partitionName.csv.gz")
  }
}
