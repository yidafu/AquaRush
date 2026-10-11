/**
 * AquaRush
 *
 * Copyright (C) 2026 AquaRush Team
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

package dev.yidafu.aqua.common.domain.table

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.datetime

// 日志域表定义(JPA→Exposed 迁移 P5:foundation logging 仓储去 QueryDSL)。
//
// 三张日志表在 DDL 中都是 **按 created_at 月度 RANGE 分区** 的表(v1.0.4 建、v1.3.0 改造为分区,
// 数据落在默认分区 `<表名>_default`),主键统一为 (id, created_at) 复合键;分区键必须是主键列,
// 因此 Exposed 的 PrimaryKey 也要带 created_at —— 否则 SchemaUtils.create 在 PG 上建不出等价结构。
// 列定义逐字对齐 changelog(V100InitComprehensiveSchema 1.0.4/1.0.6 + V130LogPartitionBackup)。
//
// 登录/行为日志(user_action_logs)定义在 SourceTables.kt(analytics 聚合先用到,保持原处)。

/** API 请求日志表(`api_logs`,logging 域,17 列)。 */
object ApiLogsTable : Table("aqua_api_logs") {
  val id = long("id")
  val correlationId = varchar("correlation_id", 50).nullable()
  val requestType = varchar("request_type", 20)
  val method = varchar("method", 10).nullable()
  val uri = varchar("uri", 500).nullable()
  val query = text("query").nullable()
  val requestBody = text("request_body").nullable()
  val responseStatus = integer("response_status").nullable()
  val durationMs = long("duration_ms").nullable()
  val ipAddress = varchar("ip_address", 50).nullable()
  val userAgent = varchar("user_agent", 500).nullable()
  val userId = long("user_id").nullable()
  val username = varchar("username", 100).nullable()
  val errorMessage = text("error_message").nullable()
  val operationType = varchar("operation_type", 20).nullable()
  val operationName = varchar("operation_name", 100).nullable()
  val createdAt = datetime("created_at")
  override val primaryKey = PrimaryKey(id, createdAt)
}

/** 业务日志表(`business_logs`,logging 域,10 列;operation 列由 1.1.1 加入后合并至 v1.0.0 DDL)。 */
object BusinessLogsTable : Table("aqua_business_logs") {
  val id = long("id")
  val correlationId = varchar("correlation_id", 50).nullable()
  val level = varchar("level", 10)
  val loggerName = varchar("logger_name", 200).nullable()
  val message = text("message").nullable()
  val stackTrace = text("stack_trace").nullable()
  val userId = long("user_id").nullable()
  val username = varchar("username", 100).nullable()
  val createdAt = datetime("created_at")
  val operation = varchar("operation", 100).nullable()
  override val primaryKey = PrimaryKey(id, createdAt)
}

/** 日志离线备份记录表(`log_backup_records`,logging 域,18 列,单列主键)。 */
object LogBackupRecordsTable : Table("aqua_log_backup_records") {
  val id = long("id")
  val logTable = varchar("log_table", 30)
  val partitionName = varchar("partition_name", 100)
  val partitionMonth = varchar("partition_month", 7)
  val coldTableName = varchar("cold_table_name", 200)
  val fromBound = datetime("from_bound").nullable()
  val toBound = datetime("to_bound").nullable()
  val rowCount = long("row_count").default(0L)
  val filePath = varchar("file_path", 500).nullable()
  val fileSizeBytes = long("file_size_bytes").nullable()
  val fileChecksum = varchar("file_checksum", 64).nullable()
  val status = varchar("status", 20)
  val errorMessage = text("error_message").nullable()
  val operatorId = long("operator_id").nullable()
  val operatorName = varchar("operator_name", 100).nullable()
  val startedAt = datetime("started_at")
  val finishedAt = datetime("finished_at").nullable()
  val createdAt = datetime("created_at")
  override val primaryKey = PrimaryKey(id)
}
