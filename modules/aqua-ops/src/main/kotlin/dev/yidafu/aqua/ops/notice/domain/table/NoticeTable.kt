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

package dev.yidafu.aqua.ops.notice.domain.table

import dev.yidafu.aqua.common.domain.model.enums.NoticeStatus
import dev.yidafu.aqua.common.domain.model.enums.NoticeType
import dev.yidafu.aqua.common.domain.table.SoftDeletableTable
import org.jetbrains.exposed.v1.javatime.datetime

/**
 * `notices` 表(平台公告,F-OPS-002)。
 *
 * 列定义逐字对齐 `db/changelog/changes/v1.0.7-add-ops-notice-store-delivery-fee.xml`；
 * 对应 JPA 实体为 `NoticeModel`。
 */
object NoticeTable : SoftDeletableTable("notices") {
  val id = long("id")
  val title = varchar("title", 50)
  val content = text("content")
  val type = enumerationByName("type", 30, NoticeType::class)
  val pinned = bool("pinned").default(false)
  val startAt = datetime("start_at").nullable()
  val endAt = datetime("end_at").nullable()
  val status = enumerationByName("status", 30, NoticeStatus::class)
  val publishedAt = datetime("published_at").nullable()
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")

  override val primaryKey = PrimaryKey(id)
}
