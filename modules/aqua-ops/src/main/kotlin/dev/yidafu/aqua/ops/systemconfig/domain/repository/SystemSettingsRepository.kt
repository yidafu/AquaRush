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
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * 系统参数仓库(系统设置域,SYS)
 * 与 PRD 11-system-settings.md §R-SYS-01 保持一致:setting_key 唯一,键为字母数字下划线。
 */
@Repository
interface SystemSettingsRepository : JpaRepository<SystemSettingsModel, Long> {
  /**
   * 按参数键查询(唯一)
   */
  fun findBySettingKey(settingKey: String): SystemSettingsModel?

  /**
   * 按参数键集合批量查询
   */
  fun findAllBySettingKeyIn(keys: Collection<String>): List<SystemSettingsModel>

  /**
   * 按参数组查询,组内按键排序
   */
  fun findAllBySettingGroupOrderBySettingKey(group: SystemSettingGroup): List<SystemSettingsModel>
}
