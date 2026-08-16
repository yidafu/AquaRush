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

import dev.yidafu.aqua.api.service.ops.LogPartitionMutationService
import dev.yidafu.aqua.common.domain.model.LogBackupRecordModel
import dev.yidafu.aqua.logging.partition.domain.model.enums.LogTableType
import dev.yidafu.aqua.logging.partition.service.LogBackupService
import org.springframework.stereotype.Service

/**
 * 日志分区离线备份服务实现(F-SYS-006)
 */
@Service
class LogPartitionMutationServiceImpl(
  private val logBackupService: LogBackupService,
) : LogPartitionMutationService {
  override fun backup(
    logTable: LogTableType,
    partitionName: String,
    operatorId: Long?,
    operatorName: String,
    retentionDays: Int,
  ): LogBackupRecordModel =
    logBackupService.backup(
      logTable = logTable,
      partitionName = partitionName,
      operatorId = operatorId,
      operatorName = operatorName,
      retentionDays = retentionDays,
    )
}
