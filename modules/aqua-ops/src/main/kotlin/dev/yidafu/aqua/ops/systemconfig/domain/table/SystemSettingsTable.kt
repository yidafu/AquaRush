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

package dev.yidafu.aqua.ops.systemconfig.domain.table

import dev.yidafu.aqua.common.domain.model.enums.SystemSettingGroup
import dev.yidafu.aqua.common.domain.model.enums.SystemSettingType
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.datetime

/**
 * `system_settings` 表(系统参数,F-SYS-001)。
 *
 * 列定义逐字对齐 `db/changelog/changes/v1.0.0-init-comprehensive-schema.xml` + `v1.0.8-add-system-config-params.xml`。
 * 对应 JPA 实体为 `SystemSettingsModel`。
 *
 * **注意**：本表**没有** `is_deleted/deleted_at/deleted_by` 软删列（实体也未实现 SoftDeletable），
 * 因此不继承 SoftDeletableTable；删除语义是物理删除。
 */
object SystemSettingsTable : Table("aqua_system_settings") {
  val id = long("id")
  val settingKey = varchar("setting_key", 64)
  val settingValue = varchar("setting_value", 255).nullable()
  val settingType = enumerationByName("setting_type", 20, SystemSettingType::class)
  val settingGroup = enumerationByName("setting_group", 50, SystemSettingGroup::class)
  val defaultValue = varchar("default_value", 255).nullable()
  val minValue = varchar("min_value", 64).nullable()
  val maxValue = varchar("max_value", 64).nullable()

  // 注意：属性名不能用 `options`（与 Table.options 成员冲突），列名保持 DDL 的 "options"
  val settingOptions = varchar("options", 255).nullable()
  val updatedBy = varchar("updated_by", 50).nullable()
  val description = varchar("description", 255).nullable()
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")

  override val primaryKey = PrimaryKey(id)
}
