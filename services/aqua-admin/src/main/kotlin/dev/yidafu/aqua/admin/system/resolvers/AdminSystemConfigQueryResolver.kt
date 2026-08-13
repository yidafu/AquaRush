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

package dev.yidafu.aqua.admin.system.resolvers

import dev.yidafu.aqua.api.service.ops.SystemConfigQueryService
import dev.yidafu.aqua.common.domain.model.enums.SystemSettingGroup
import dev.yidafu.aqua.common.graphql.generated.SystemConfigGroup
import dev.yidafu.aqua.common.graphql.generated.SystemConfigVo
import dev.yidafu.aqua.ops.systemconfig.mapper.SystemConfigMapper
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

/**
 * 系统参数查询 Resolver(管理后台,PERMISSION_SYSTEM_CONFIG_READ)
 * 与 PRD 11-system-settings.md §F-SYS-001 保持一致(按参数组浏览,仅超级管理员可见,R-SYS-03)。
 */
@Controller
class AdminSystemConfigQueryResolver(
  private val systemConfigQueryService: SystemConfigQueryService,
) {
  /**
   * 系统参数列表(按参数组返回,未配置组返回空列表)
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_SYSTEM_CONFIG_READ')")
  fun systemConfigs(@Argument group: SystemConfigGroup): List<SystemConfigVo> {
    val domainGroup = SystemSettingGroup.valueOf(group.name)
    return systemConfigQueryService.listByGroup(domainGroup).map { SystemConfigMapper.map(it) }
  }
}
