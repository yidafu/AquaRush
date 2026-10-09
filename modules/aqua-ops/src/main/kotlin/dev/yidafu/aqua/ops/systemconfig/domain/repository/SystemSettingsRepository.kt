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

package dev.yidafu.aqua.ops.systemconfig.domain.repository

import dev.yidafu.aqua.common.domain.model.SystemSettingsModel
import dev.yidafu.aqua.common.domain.model.enums.SystemSettingGroup
import dev.yidafu.aqua.common.id.SnowflakeIdGenerator
import dev.yidafu.aqua.ops.systemconfig.domain.table.SystemSettingsTable
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository

/**
 * 系统参数仓库（Exposed 实现，系统设置域 SYS，F-SYS-001）。
 * 与 PRD 11-system-settings.md §R-SYS-01 保持一致：setting_key 唯一，键为字母数字下划线。
 *
 * 注意：本表**无软删列**（实体未实现 SoftDeletable），查询无需 `is_deleted` 过滤。
 */
@Repository
class SystemSettingsRepository(
  private val database: Database,
) {
  /** 按参数键查询(唯一)。 */
  fun findBySettingKey(settingKey: String): SystemSettingsModel? =
    transaction(database) {
      SystemSettingsTable
        .selectAll()
        .where { SystemSettingsTable.settingKey eq settingKey }
        .singleOrNull()
        ?.let(::toModel)
    }

  /** 按参数键集合批量查询。 */
  fun findAllBySettingKeyIn(keys: Collection<String>): List<SystemSettingsModel> =
    if (keys.isEmpty()) {
      emptyList()
    } else {
      transaction(database) {
        SystemSettingsTable
          .selectAll()
          .where { SystemSettingsTable.settingKey inList keys }
          .orderBy(SystemSettingsTable.settingKey to SortOrder.ASC)
          .map(::toModel)
      }
    }

  /** 按参数组查询，组内按键排序。 */
  fun findAllBySettingGroupOrderBySettingKey(group: SystemSettingGroup): List<SystemSettingsModel> =
    transaction(database) {
      SystemSettingsTable
        .selectAll()
        .where { SystemSettingsTable.settingGroup eq group }
        .orderBy(SystemSettingsTable.settingKey to SortOrder.ASC)
        .map(::toModel)
    }

  fun save(setting: SystemSettingsModel): SystemSettingsModel =
    transaction(database) {
      val id =
        setting.id
          ?: SnowflakeIdGenerator.getInstance().generate().also { setting.id = it }
      val exists =
        SystemSettingsTable
          .selectAll()
          .where { SystemSettingsTable.id eq id }
          .limit(1)
          .any()
      if (exists) {
        val now = java.time.LocalDateTime.now()
        SystemSettingsTable.update({ SystemSettingsTable.id eq id }) {
          it[settingValue] = setting.settingValue
          it[settingType] = setting.settingType
          it[settingGroup] = setting.settingGroup
          it[updatedBy] = setting.updatedBy
          it[updatedAt] = now
        }
        setting.updatedAt = now
      } else {
        SystemSettingsTable.insert {
          it[SystemSettingsTable.id] = id
          it[settingKey] = setting.settingKey
          it[settingValue] = setting.settingValue
          it[settingType] = setting.settingType
          it[settingGroup] = setting.settingGroup
          it[defaultValue] = setting.defaultValue
          it[minValue] = setting.minValue
          it[maxValue] = setting.maxValue
          it[settingOptions] = setting.options
          it[updatedBy] = setting.updatedBy
          it[description] = setting.description
          it[createdAt] = setting.createdAt
          it[updatedAt] = setting.updatedAt
        }
      }
      setting
    }

  fun saveAll(settings: Iterable<SystemSettingsModel>): List<SystemSettingsModel> = settings.map(::save)

  private fun toModel(row: ResultRow): SystemSettingsModel =
    SystemSettingsModel(
      id = row[SystemSettingsTable.id],
      settingKey = row[SystemSettingsTable.settingKey],
      settingValue = row[SystemSettingsTable.settingValue],
      settingType = row[SystemSettingsTable.settingType],
      settingGroup = row[SystemSettingsTable.settingGroup],
      defaultValue = row[SystemSettingsTable.defaultValue],
      minValue = row[SystemSettingsTable.minValue],
      maxValue = row[SystemSettingsTable.maxValue],
      options = row[SystemSettingsTable.settingOptions],
      updatedBy = row[SystemSettingsTable.updatedBy],
      description = row[SystemSettingsTable.description],
      createdAt = row[SystemSettingsTable.createdAt],
      updatedAt = row[SystemSettingsTable.updatedAt],
    )
}
