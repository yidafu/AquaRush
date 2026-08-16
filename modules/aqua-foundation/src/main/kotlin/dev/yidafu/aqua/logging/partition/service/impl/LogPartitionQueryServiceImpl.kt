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

import dev.yidafu.aqua.api.service.ops.LogPartitionQueryService
import dev.yidafu.aqua.common.domain.model.LogBackupRecordModel
import dev.yidafu.aqua.logging.partition.domain.model.LogPartitionInfo
import dev.yidafu.aqua.logging.partition.domain.model.enums.LogBackupStatus
import dev.yidafu.aqua.logging.partition.domain.model.enums.LogTableType
import dev.yidafu.aqua.logging.partition.domain.repository.LogBackupRecordRepository
import dev.yidafu.aqua.logging.partition.repository.LogPartitionRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service

/**
 * 日志分区查询服务实现(F-SYS-006)
 */
@Service
class LogPartitionQueryServiceImpl(
  private val logPartitionRepository: LogPartitionRepository,
  private val logBackupRecordRepository: LogBackupRecordRepository,
) : LogPartitionQueryService {
  override fun listPartitions(logTable: LogTableType): List<LogPartitionInfo> = logPartitionRepository.listPartitions(logTable)

  override fun listBackupRecords(
    logTable: LogTableType?,
    page: Int,
    size: Int,
  ): Page<LogBackupRecordModel> {
    val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startedAt"))
    return if (logTable != null) {
      logBackupRecordRepository.findByLogTableOrderByStartedAtDesc(logTable, pageable)
    } else {
      logBackupRecordRepository.findAll(pageable)
    }
  }

  override fun isBackedUp(partitionName: String): Boolean =
    logBackupRecordRepository.existsByPartitionNameAndStatusIn(
      partitionName,
      listOf(LogBackupStatus.RUNNING, LogBackupStatus.SUCCEEDED),
    )
}
