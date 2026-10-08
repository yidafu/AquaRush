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

package dev.yidafu.aqua.ops.systemconfig.mapper

import dev.yidafu.aqua.api.dto.SaveSystemConfigItemRequest
import dev.yidafu.aqua.common.domain.model.SystemSettingsModel
import dev.yidafu.aqua.common.graphql.generated.SaveSystemConfigInput
import dev.yidafu.aqua.common.graphql.generated.SystemConfigGroup
import dev.yidafu.aqua.common.graphql.generated.SystemConfigVo
import dev.yidafu.aqua.common.graphql.generated.SystemSettingType
import tech.mappie.api.EnumMappie
import tech.mappie.api.ObjectMappie
import dev.yidafu.aqua.common.domain.model.enums.SystemSettingGroup as DomainSystemSettingGroup
import dev.yidafu.aqua.common.domain.model.enums.SystemSettingType as DomainSystemSettingType

/**
 * 系统参数映射器:域实体 → GraphQL 类型
 * 与 PRD 11-system-settings.md §F-SYS-001 保持一致(键/值/类型/分组/默认值/说明)。
 */
object SystemConfigMapper : ObjectMappie<SystemSettingsModel, SystemConfigVo>() {
  override fun map(from: SystemSettingsModel): SystemConfigVo =
    mapping {
      to::id fromValue (from.id ?: 0L)
    }
}

object SystemSettingTypeMapper : EnumMappie<DomainSystemSettingType, SystemSettingType>()

object SystemSettingGroupMapper : EnumMappie<DomainSystemSettingGroup, SystemConfigGroup>()

/**
 * 保存系统参数输入映射器:GraphQL 输入 → 服务请求
 */
object SaveSystemConfigItemMapper : ObjectMappie<SaveSystemConfigInput, SaveSystemConfigItemRequest>()
