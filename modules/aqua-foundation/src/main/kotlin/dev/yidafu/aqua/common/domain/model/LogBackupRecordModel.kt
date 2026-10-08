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

import dev.yidafu.aqua.common.annotation.SnowflakeIdGenerator
import dev.yidafu.aqua.logging.partition.domain.model.enums.LogBackupStatus
import dev.yidafu.aqua.logging.partition.domain.model.enums.LogTableType
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 日志离线备份记录实体(F-SYS-006)
 * 记录每次「转储冷表 + 导出数据文件」的结果(冷表名/文件/校验和/操作人/时间),供数据维护页查询与追溯。
 */
@Entity
@Table(
  name = "log_backup_records",
  indexes = [
    Index(name = "idx_log_backup_records_log_table", columnList = "log_table"),
    Index(name = "idx_log_backup_records_created_at", columnList = "created_at"),
  ],
)
data class LogBackupRecordModel(
  @Id
  @SnowflakeIdGenerator
  @Column(name = "id", nullable = false, updatable = false)
  var id: Long? = null,
  @Column(name = "log_table", nullable = false, length = 30)
  @Enumerated(EnumType.STRING)
  var logTable: LogTableType = LogTableType.API_LOG,
  @Column(name = "partition_name", nullable = false, length = 100)
  var partitionName: String = "",
  @Column(name = "partition_month", nullable = false, length = 7)
  var partitionMonth: String = "",
  @Column(name = "cold_table_name", nullable = false, length = 200)
  var coldTableName: String = "",
  @Column(name = "from_bound")
  var fromBound: LocalDateTime? = null,
  @Column(name = "to_bound")
  var toBound: LocalDateTime? = null,
  @Column(name = "row_count", nullable = false)
  var rowCount: Long = 0,
  @Column(name = "file_path", length = 500)
  var filePath: String? = null,
  @Column(name = "file_size_bytes")
  var fileSizeBytes: Long? = null,
  @Column(name = "file_checksum", length = 64)
  var fileChecksum: String? = null,
  @Column(name = "status", nullable = false, length = 20)
  @Enumerated(EnumType.STRING)
  var status: LogBackupStatus = LogBackupStatus.RUNNING,
  @Column(name = "error_message", columnDefinition = "text")
  var errorMessage: String? = null,
  @Column(name = "operator_id")
  var operatorId: Long? = null,
  @Column(name = "operator_name", length = 100)
  var operatorName: String? = null,
  @Column(name = "started_at", nullable = false, updatable = false)
  var startedAt: LocalDateTime = LocalDateTime.now(),
  @Column(name = "finished_at")
  var finishedAt: LocalDateTime? = null,
  @Column(name = "created_at", nullable = false, updatable = false)
  var createdAt: LocalDateTime = LocalDateTime.now(),
)
