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

package dev.yidafu.aqua.ops.feedback.domain.table

import dev.yidafu.aqua.common.domain.model.enums.FeedbackStatus
import dev.yidafu.aqua.common.domain.model.enums.FeedbackType
import dev.yidafu.aqua.common.domain.table.SoftDeletableTable
import dev.yidafu.aqua.common.domain.table.jsonString
import org.jetbrains.exposed.v1.javatime.datetime

/**
 * `feedbacks` 表(意见反馈,F-OPS-003)。
 *
 * 列定义逐字对齐 `db/changelog/changes/v1.0.6-add-ops-config-tables.xml`
 * + `v1.7.0-seed-nanshan-demo-data.xml`(种子数据)。
 * 对应 JPA 实体为 `FeedbackModel`。
 */
object FeedbackTable : SoftDeletableTable("feedbacks") {
  val id = long("id")
  val userId = long("user_id")
  val feedbackType = enumerationByName("feedback_type", 30, FeedbackType::class)
  val content = text("content")

  /**
   * DDL 实际类型是 `json`（Liquibase 负责 DDL，Exposed 不参与建表），这里按字符串读写；
   * 序列化/反序列化**必须**走 `ArrayNodeConverter`（与 JPA AttributeConverter 完全同一条路径），
   * 保证迁移前后 JSON 文本逐字节一致（迁移方案 §9 R4）。
   */
  val images = jsonString("images").nullable()
  val contactInfo = varchar("contact_info", 100).nullable()
  val status = enumerationByName("status", 30, FeedbackStatus::class)
  val handleResult = text("handle_result").nullable()
  val handledById = long("handled_by_id").nullable()
  val handledByName = varchar("handled_by_name", 100).nullable()
  val handledAt = datetime("handled_at").nullable()
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")

  override val primaryKey = PrimaryKey(id)
}
