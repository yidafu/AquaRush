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

package dev.yidafu.aqua.ops.systemconfig.service.impl

import dev.yidafu.aqua.api.service.ops.SystemConfigQueryService
import dev.yidafu.aqua.common.domain.model.SystemSettingsModel
import dev.yidafu.aqua.common.domain.model.enums.SystemSettingGroup
import dev.yidafu.aqua.ops.systemconfig.domain.repository.SystemSettingsRepository
import org.springframework.stereotype.Service

/**
 * 系统参数查询服务实现(管理后台)
 * 与 PRD 11-system-settings.md §F-SYS-001 保持一致(按参数组浏览)。
 */
@Service
class SystemConfigQueryServiceImpl(
  private val systemSettingsRepository: SystemSettingsRepository,
) : SystemConfigQueryService {
  override fun listByGroup(group: SystemSettingGroup): List<SystemSettingsModel> =
    systemSettingsRepository.findAllBySettingGroupOrderBySettingKey(group)
}
