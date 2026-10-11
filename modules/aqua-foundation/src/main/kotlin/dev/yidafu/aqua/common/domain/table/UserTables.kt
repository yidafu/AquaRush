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

// user 域本地表(JPA→Exposed 迁移 P3 收尾)。
// addresses/regions 列定义逐字对齐 changelog DDL(V100InitComprehensiveSchema)。
// 通知开关统一 user_notification_settings(PlatformTables;缺陷 #7 双表合一,
// 原 notification_settings 已随 V340 迁数据后删除)。

/** 收货地址表(`addresses`)。 */
object AddressesTable : SoftDeletableTable("aqua_addresses") {
  val id = long("id")
  val userId = long("user_id")
  val receiverName = varchar("receiver_name", 50)
  val phone = varchar("phone", 20)
  val province = varchar("province", 50)
  val provinceCode = varchar("province_code", 12).nullable()
  val city = varchar("city", 50)
  val cityCode = varchar("city_code", 12).nullable()
  val district = varchar("district", 50)
  val districtCode = varchar("district_code", 12).nullable()
  val detailAddress = varchar("detail_address", 500)
  val floor = integer("floor").nullable()
  val longitude = double("longitude").nullable()
  val latitude = double("latitude").nullable()
  val isDefault = bool("is_default").clientDefault { false }
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
  override val primaryKey = PrimaryKey(id)
}

/** 行政区划表(`regions`)。 */
object RegionsTable : SoftDeletableTable("aqua_regions") {
  val id = long("id")
  val name = varchar("name", 100)
  val code = varchar("code", 12)
  val parentCode = varchar("parent_code", 12).nullable()
  val level = integer("level")
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
  override val primaryKey = PrimaryKey(id)
}
