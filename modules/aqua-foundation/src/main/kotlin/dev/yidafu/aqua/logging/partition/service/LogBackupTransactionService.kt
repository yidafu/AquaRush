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
import dev.yidafu.aqua.logging.partition.domain.exception.LogPartitionBackupInProgressException
import dev.yidafu.aqua.logging.partition.domain.exception.LogPartitionNotBackupableException
import dev.yidafu.aqua.logging.partition.domain.model.LogPartitionInfo
import dev.yidafu.aqua.logging.partition.domain.model.enums.LogBackupStatus
import dev.yidafu.aqua.logging.partition.domain.repository.LogBackupRecordRepository
import dev.yidafu.aqua.logging.partition.repository.LogPartitionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 日志备份事务执行器(F-SYS-006)
 * 独立事务承载备份全流程:事务级 advisory lock → RUNNING 记录 → 导出文件 → 并入年度冷表 → DROP 分区 → SUCCEEDED。
 * 任一步失败整体回滚(分区仍在热表,数据无损),由 LogBackupService 统一记 FAILED 记录。
 */
@Service
class LogBackupTransactionService(
  private val logPartitionRepository: LogPartitionRepository,
  private val logBackupRecordRepository: LogBackupRecordRepository,
  private val dataMaintenanceProperties: DataMaintenanceProperties,
) {
  /**
   * 事务内执行备份(调用方需捕获 BackupExecutionException 处理后事)
   */
  @Transactional
  fun doBackup(
    partition: LogPartitionInfo,
    operatorId: Long?,
    operatorName: String,
  ): LogBackupRecordModel {
    if (!logPartitionRepository.tryAcquireBackupLock(partition.partitionName)) {
      throw LogPartitionBackupInProgressException(partition.partitionName)
    }

    val month = partition.partitionMonth ?: throw LogPartitionNotBackupableException("DEFAULT 兜底分区不可备份")
    val year = LogPartitionRules.yearOfPartition(partition.partitionName) ?: throw LogPartitionNotBackupableException("非月度分区")
    val coldTableName = "log_archive.${partition.logTable.tableName}_$year"

    val record =
      logBackupRecordRepository.save(
        LogBackupRecordModel(
          logTable = partition.logTable,
          partitionName = partition.partitionName,
          partitionMonth = month,
          coldTableName = coldTableName,
          fromBound = partition.fromBound,
          toBound = partition.toBound,
          status = LogBackupStatus.RUNNING,
          operatorId = operatorId,
          operatorName = operatorName,
          startedAt = LocalDateTime.now(),
        ),
      )

    var exportCompleted = false
    try {
      val file = LogBackupService.backupFileOf(dataMaintenanceProperties, partition.partitionName)
      val export = logPartitionRepository.exportPartitionToGzip(partition.partitionName, file)
      exportCompleted = true

      logPartitionRepository.ensureColdYearTable(partition.logTable, year)
      val appended = logPartitionRepository.appendToColdTable(partition.logTable, year, partition.partitionName)
      logPartitionRepository.dropPartition(partition.partitionName)

      record.rowCount = appended.toLong()
      record.filePath = file.toString()
      record.fileSizeBytes = export.sizeBytes
      record.fileChecksum = export.sha256
      record.status = LogBackupStatus.SUCCEEDED
      record.finishedAt = LocalDateTime.now()
      return logBackupRecordRepository.save(record)
    } catch (e: Exception) {
      throw BackupExecutionException(exportCompleted, e)
    }
  }
}
