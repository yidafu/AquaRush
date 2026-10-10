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

package dev.yidafu.aqua.common.domain.model

import dev.yidafu.aqua.logging.partition.domain.model.enums.LogBackupStatus
import dev.yidafu.aqua.logging.partition.domain.model.enums.LogTableType
import java.time.LocalDateTime

/**
 * 日志离线备份记录实体(F-SYS-006)
 * 记录每次「转储冷表 + 导出数据文件」的结果(冷表名/文件/校验和/操作人/时间),供数据维护页查询与追溯。
 */
data class LogBackupRecordModel(
  var id: Long? = null,
  var logTable: LogTableType = LogTableType.API_LOG,
  var partitionName: String = "",
  var partitionMonth: String = "",
  var coldTableName: String = "",
  var fromBound: LocalDateTime? = null,
  var toBound: LocalDateTime? = null,
  var rowCount: Long = 0,
  var filePath: String? = null,
  var fileSizeBytes: Long? = null,
  var fileChecksum: String? = null,
  var status: LogBackupStatus = LogBackupStatus.RUNNING,
  var errorMessage: String? = null,
  var operatorId: Long? = null,
  var operatorName: String? = null,
  var startedAt: LocalDateTime = LocalDateTime.now(),
  var finishedAt: LocalDateTime? = null,
  var createdAt: LocalDateTime = LocalDateTime.now(),
)
