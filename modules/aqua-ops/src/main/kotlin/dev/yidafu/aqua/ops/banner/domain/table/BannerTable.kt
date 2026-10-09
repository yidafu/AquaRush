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

package dev.yidafu.aqua.ops.banner.domain.table

import dev.yidafu.aqua.common.domain.model.enums.BannerLinkType
import dev.yidafu.aqua.common.domain.table.SoftDeletableTable
import org.jetbrains.exposed.v1.javatime.datetime

/**
 * `banners` 表(首页轮播图运营位,F-OPS-001)。
 *
 * 列定义逐字对齐 `db/changelog/changes/v1.0.6-add-ops-config-tables.xml`；
 * 对应 JPA 实体为 `BannerModel`。`id` 为应用侧 Snowflake 生成，非数据库自增。
 */
object BannerTable : SoftDeletableTable("banners") {
  val id = long("id")
  val imageUrl = varchar("image_url", 1000)
  val title = varchar("title", 20)
  val subtitle = varchar("subtitle", 100).nullable()
  val linkType = enumerationByName("link_type", 30, BannerLinkType::class)
  val linkTarget = varchar("link_target", 1000)
  val sortOrder = integer("sort_order")
  val enabled = bool("enabled").default(false)
  val startAt = datetime("start_at").nullable()
  val endAt = datetime("end_at").nullable()
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")

  override val primaryKey = PrimaryKey(id)
}
