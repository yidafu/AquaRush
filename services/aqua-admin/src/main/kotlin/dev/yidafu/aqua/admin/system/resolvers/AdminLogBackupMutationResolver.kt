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

import dev.yidafu.aqua.api.service.ops.LogPartitionMutationService
import dev.yidafu.aqua.api.service.ops.SystemConfigQueryService
import dev.yidafu.aqua.common.domain.model.enums.SystemSettingGroup
import dev.yidafu.aqua.common.graphql.generated.BackupLogPartitionInput
import dev.yidafu.aqua.common.graphql.generated.LogBackupRecord
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.logging.partition.mapper.toGraphQL
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import dev.yidafu.aqua.logging.partition.domain.model.enums.LogTableType as DomainLogTableType

/**
 * 日志离线备份 Resolver(管理后台,PERMISSION_SYSTEM_CONFIG_WRITE)
 * 与 PRD 11-system-settings.md §F-SYS-006 保持一致:超期分区「转储冷表 + 导出数据文件」,仅超级管理员。
 */
@Controller
class AdminLogBackupMutationResolver(
  private val logPartitionMutationService: LogPartitionMutationService,
  private val systemConfigQueryService: SystemConfigQueryService,
) {
  private val logger = LoggerFactory.getLogger(AdminLogBackupMutationResolver::class.java)

  /**
   * 离线备份指定超期日志分区
   */
  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_SYSTEM_CONFIG_WRITE')")
  fun backupLogPartition(
    @Argument @Valid input: BackupLogPartitionInput,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): LogBackupRecord {
    val table = input.logTable.toDomain()
    logger.info("Admin {} backing up log partition {} of {}", principal.username, input.partitionName, table.tableName)
    val record =
      logPartitionMutationService.backup(
        logTable = table,
        partitionName = input.partitionName,
        operatorId = principal.id,
        operatorName = principal.username,
        retentionDays = retentionDaysOf(table),
      )
    return record.toGraphQL()
  }

  /**
   * 该表保留天数(取自 DATA_MAINTENANCE 组参数,未配置按默认 90 天)
   */
  private fun retentionDaysOf(table: DomainLogTableType): Int {
    val settings = systemConfigQueryService.listByGroup(SystemSettingGroup.DATA_MAINTENANCE)
    return settings
      .firstOrNull { it.settingKey == table.retentionKey }
      ?.settingValue
      ?.toIntOrNull()
      ?: DEFAULT_RETENTION_DAYS
  }

  companion object {
    private const val DEFAULT_RETENTION_DAYS = 90
  }
}
