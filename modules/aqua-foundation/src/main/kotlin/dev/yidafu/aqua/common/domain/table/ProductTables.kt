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

// product 域表(JPA→Exposed 迁移 P4):商品收藏/商品属性选项字典。
// 列定义逐字对齐 changelog DDL(v1.0.0 基础 schema 的 product_favorites + v1.7.0 的 product_options,
// 已与线上库 psql \d 输出逐表核对)。对应 JPA 实体为 ProductFavoriteModel/ProductOptionModel。
// FK/CHECK/UNIQUE 约束不由 Exposed 表表达(DDL 层持有),唯一约束在各表 KDoc 注明;
// created_at/updated_at 均 NOT NULL,由仓储插入时赋值(与 ReconciliationTables 风格一致)。

/** 商品收藏表(`product_favorites`,9 列;(user_id, product_id) 有 DB 唯一约束)。 */
object ProductFavoritesTable : SoftDeletableTable("product_favorites") {
  val id = long("id")
  val userId = long("user_id")
  val productId = long("product_id")
  val enable = bool("enable").default(true)
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
  override val primaryKey = PrimaryKey(id)
}

/** 商品属性选项字典表(`product_options`,10 列,规格/水源下拉候选;无 product_id,全局字典)。 */
object ProductOptionsTable : SoftDeletableTable("product_options") {
  val id = long("id")
  val optionType = varchar("option_type", 32)
  val optionValue = varchar("option_value", 100)
  val sortOrder = integer("sort_order").default(999)
  val enabled = bool("enabled").default(true)
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
  override val primaryKey = PrimaryKey(id)
}
