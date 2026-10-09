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
// 注意:notification_settings 表在 Liquibase DDL 中**不存在**(历史由 Hibernate
// ddl-auto 建),列定义按 NotificationSettingsModel 实体对齐 —— 存量风险,P5 收口时
// 需补 DDL 或裁剪该功能。

/** 收货地址表(`addresses`)。 */
object AddressesTable : SoftDeletableTable("addresses") {
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
object RegionsTable : SoftDeletableTable("regions") {
  val id = long("id")
  val name = varchar("name", 100)
  val code = varchar("code", 12)
  val parentCode = varchar("parent_code", 12).nullable()
  val level = integer("level")
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
  override val primaryKey = PrimaryKey(id)
}

/** 用户通知开关表(`notification_settings`,DDL 缺失,列按实体对齐;见文件头说明)。 */
object NotificationSettingsTable : SoftDeletableTable("notification_settings") {
  val id = long("id")
  val userId = long("user_id")
  val orderUpdates = bool("order_updates").clientDefault { true }
  val paymentNotifications = bool("payment_notifications").clientDefault { true }
  val deliveryNotifications = bool("delivery_notifications").clientDefault { true }
  val promotionalNotifications = bool("promotional_notifications").clientDefault { false }
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
  override val primaryKey = PrimaryKey(id)
}
