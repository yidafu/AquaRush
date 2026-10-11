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

package dev.yidafu.aqua.ops.storeprofile.domain.table

import dev.yidafu.aqua.common.domain.table.SoftDeletableTable
import org.jetbrains.exposed.v1.javatime.datetime
import org.jetbrains.exposed.v1.javatime.time

/**
 * `store_profiles` 表(店铺信息,F-OPS-004,单行配置固定 id=1)。
 *
 * 列定义逐字对齐 `db/changelog/changes/v1.0.7-add-ops-notice-store-delivery-fee.xml`
 * + `v1.7.0-seed-nanshan-demo-data.xml`(种子数据)。
 * 对应 JPA 实体为 `StoreProfileModel`。
 */
object StoreProfileTable : SoftDeletableTable("aqua_store_profiles") {
  val id = long("id")
  val shopName = varchar("shop_name", 20)
  val logoUrl = varchar("logo_url", 1000).nullable()
  val serviceHotline = varchar("service_hotline", 30)
  val businessHoursStart = time("business_hours_start").nullable()
  val businessHoursEnd = time("business_hours_end").nullable()
  val shopIntro = varchar("shop_intro", 100).nullable()
  val aboutUs = text("about_us").nullable()
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")

  override val primaryKey = PrimaryKey(id)
}
