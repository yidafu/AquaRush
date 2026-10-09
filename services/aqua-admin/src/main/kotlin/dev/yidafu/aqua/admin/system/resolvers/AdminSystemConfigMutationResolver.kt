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

import dev.yidafu.aqua.api.service.ops.SystemConfigMutationService
import dev.yidafu.aqua.common.graphql.generated.SaveSystemConfigInput
import dev.yidafu.aqua.common.graphql.generated.SystemConfigVo
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.ops.systemconfig.mapper.toGraphQL
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

/**
 * 系统参数变更 Resolver(管理后台,PERMISSION_SYSTEM_CONFIG_WRITE)
 * 与 PRD 11-system-settings.md §R-SYS-01 保持一致:按声明类型/范围校验,即时生效,每次修改留审计日志;
 * 操作人取自登录主体(仅超级管理员)。
 */
@Controller
class AdminSystemConfigMutationResolver(
  private val systemConfigMutationService: SystemConfigMutationService,
) {
  private val logger = LoggerFactory.getLogger(AdminSystemConfigMutationResolver::class.java)

  /**
   * 批量保存系统参数
   */
  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_SYSTEM_CONFIG_WRITE')")
  fun saveSystemConfigs(
    @Argument @Valid input: List<SaveSystemConfigInput>,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): List<SystemConfigVo> {
    logger.info("Admin {} saving system configs: {}", principal.username, input.map { it.settingKey })
    val saved =
      systemConfigMutationService.saveSystemConfigs(
        adminId = principal.id,
        adminName = principal.username,
        items = input,
      )
    return saved.map { it.toGraphQL() }
  }

  /**
   * 恢复参数默认值
   */
  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_SYSTEM_CONFIG_WRITE')")
  fun restoreSystemConfig(
    @Argument settingKey: String,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): SystemConfigVo {
    logger.info("Admin {} restoring system config default: {}", principal.username, settingKey)
    val setting =
      systemConfigMutationService.restoreSystemConfig(
        settingKey = settingKey,
        adminId = principal.id,
        adminName = principal.username,
      )
    return setting.toGraphQL()
  }
}
