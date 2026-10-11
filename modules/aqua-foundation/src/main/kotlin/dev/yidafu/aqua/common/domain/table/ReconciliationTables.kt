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

package dev.yidafu.aqua.common.domain.table

import org.jetbrains.exposed.v1.javatime.datetime

// 微信资金对账域三表(JPA→Exposed 迁移 P3)。列定义逐字对齐 changelog DDL;对应 JPA 实体为 Reconciliation*Model。
// 三表均带 @SoftDelete(is_deleted) —— 查询必须显式过滤各表的 isDeleted 列。

/** 对账任务表(`reconciliation_tasks`)。 */
object ReconciliationTasksTable : SoftDeletableTable("reconciliation_tasks") {
  val id = long("id")
  val taskId = varchar("task_id", 100)
  val taskType = varchar("task_type", 20)
  val status = varchar("status", 20)
  val taskDate = datetime("task_date")
  val startTime = datetime("start_time").nullable()
  val endTime = datetime("end_time").nullable()
  val totalRecords = integer("total_records")
  val matchedRecords = integer("matched_records")
  val unmatchedRecords = integer("unmatched_records")
  val errorMessage = text("error_message").nullable()

  /** 触发方式:SCHEDULED-每日调度 / MANUAL-管理员手动(F-ORD-031) */
  val triggerType = varchar("trigger_type", 20)
  val executedBy = long("executed_by").nullable()
  val wechatTotalCount = integer("wechat_total_count")
  val wechatTotalAmountCents = long("wechat_total_amount_cents")
  val localTotalCount = integer("local_total_count")
  val localTotalAmountCents = long("local_total_amount_cents")
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
  override val primaryKey = PrimaryKey(id)
}

/** 对账报表表(`reconciliation_reports`)。 */
object ReconciliationReportsTable : SoftDeletableTable("reconciliation_reports") {
  val id = long("id")
  val taskId = varchar("task_id", 100)
  val reportType = varchar("report_type", 20)

  /** 报表数据(JSON,DDL `json`),内容为 `Map<String, Any>`,序列化由仓储层负责。 */
  val reportData = jsonString("report_data")
  val filePath = varchar("file_path", 500).nullable()
  val fileName = varchar("file_name", 255).nullable()
  val fileSize = long("file_size").nullable()
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
  val generatedAt = datetime("generated_at")
  override val primaryKey = PrimaryKey(id)
}

/** 对账差异表(`reconciliation_discrepancies`)。 */
object ReconciliationDiscrepanciesTable : SoftDeletableTable("reconciliation_discrepancies") {
  val id = long("id")
  val taskId = varchar("task_id", 100)
  val discrepancyType = varchar("discrepancy_type", 20)
  val sourceSystem = varchar("source_system", 20)
  val recordId = varchar("record_id", 100)

  /** 差异明细(JSON,DDL `json`),内容为 `Map<String, Any>?`,序列化由仓储层负责。 */
  val recordDetails = jsonString("record_details").nullable()
  val status = varchar("status", 20)
  val resolutionNotes = text("resolution_notes").nullable()
  val resolvedBy = varchar("resolved_by", 100).nullable()
  val resolvedAt = datetime("resolved_at").nullable()
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
  override val primaryKey = PrimaryKey(id)
}
