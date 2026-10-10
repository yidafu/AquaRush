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

package dev.yidafu.aqua.logging.partition.mapper

import dev.yidafu.aqua.common.domain.model.LogBackupRecordModel
import dev.yidafu.aqua.common.graphql.generated.LogBackupRecordVo
import dev.yidafu.aqua.common.graphql.generated.LogBackupStatus
import dev.yidafu.aqua.common.graphql.generated.LogPartitionVo
import dev.yidafu.aqua.common.graphql.generated.LogTableType
import dev.yidafu.aqua.logging.partition.domain.LogPartitionRules
import dev.yidafu.aqua.logging.partition.domain.model.LogPartitionInfo
import java.time.LocalDateTime

/**
 * 日志分区映射器:域模型 → GraphQL 类型(F-SYS-006)
 */
object LogPartitionVoMapper {
  /**
   * 分区信息 → VO(expired/backupable 按保留天数与备份记录计算)
   */
  fun map(
    partition: LogPartitionInfo,
    retentionDays: Int,
    backedUp: Boolean,
    now: LocalDateTime = LocalDateTime.now(),
  ): LogPartitionVo =
    LogPartitionVo(
      logTable = partition.logTable.toGraphQL(),
      partitionName = partition.partitionName,
      partitionMonth = partition.partitionMonth,
      rowCount = partition.rowCount,
      sizeBytes = partition.sizeBytes,
      isDefault = partition.isDefault,
      isCurrentMonth = partition.isCurrentMonth,
      expired = LogPartitionRules.isExpired(partition, retentionDays, now),
      backupable = !backedUp && LogPartitionRules.isBackupable(partition, retentionDays, now),
    )
}

/**
 * 离线备份记录 → GraphQL 类型
 */
fun LogBackupRecordModel.toGraphQL(): LogBackupRecordVo =
  LogBackupRecordVo(
    checksum = fileChecksum,
    coldTableName = coldTableName,
    errorMessage = errorMessage,
    filePath = filePath,
    fileSizeBytes = fileSizeBytes,
    finishedAt = finishedAt,
    id = id ?: 0L,
    logTable = logTable.toGraphQL(),
    operatorName = operatorName,
    partitionMonth = partitionMonth,
    partitionName = partitionName,
    rowCount = rowCount,
    startedAt = startedAt,
    status = status.toGraphQL(),
  )

private fun dev.yidafu.aqua.logging.partition.domain.model.enums.LogTableType.toGraphQL(): LogTableType =
  when (this) {
    dev.yidafu.aqua.logging.partition.domain.model.enums.LogTableType.API_LOG -> LogTableType.API_LOG
    dev.yidafu.aqua.logging.partition.domain.model.enums.LogTableType.USER_ACTION_LOG -> LogTableType.USER_ACTION_LOG
    dev.yidafu.aqua.logging.partition.domain.model.enums.LogTableType.BUSINESS_LOG -> LogTableType.BUSINESS_LOG
  }

private fun dev.yidafu.aqua.logging.partition.domain.model.enums.LogBackupStatus.toGraphQL(): LogBackupStatus =
  when (this) {
    dev.yidafu.aqua.logging.partition.domain.model.enums.LogBackupStatus.RUNNING -> LogBackupStatus.RUNNING
    dev.yidafu.aqua.logging.partition.domain.model.enums.LogBackupStatus.SUCCEEDED -> LogBackupStatus.SUCCEEDED
    dev.yidafu.aqua.logging.partition.domain.model.enums.LogBackupStatus.FAILED -> LogBackupStatus.FAILED
  }
