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

import dev.yidafu.aqua.api.service.ops.LogPartitionQueryService
import dev.yidafu.aqua.api.service.ops.SystemConfigQueryService
import dev.yidafu.aqua.common.domain.model.SystemSettingsModel
import dev.yidafu.aqua.common.domain.model.enums.SystemSettingGroup
import dev.yidafu.aqua.common.graphql.generated.DataMaintenanceOverviewVo
import dev.yidafu.aqua.common.graphql.generated.LogBackupRecordListInput
import dev.yidafu.aqua.common.graphql.generated.LogBackupRecordPage
import dev.yidafu.aqua.common.graphql.generated.LogTableType
import dev.yidafu.aqua.common.graphql.generated.PageInfoVo
import dev.yidafu.aqua.logging.partition.mapper.LogPartitionVoMapper
import dev.yidafu.aqua.logging.partition.mapper.toGraphQL
import dev.yidafu.aqua.ops.systemconfig.mapper.toGraphQL
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import dev.yidafu.aqua.logging.partition.domain.model.enums.LogTableType as DomainLogTableType

/**
 * 数据维护查询 Resolver(管理后台,PERMISSION_SYSTEM_CONFIG_READ)
 * 与 PRD 11-system-settings.md §F-SYS-006 保持一致:单请求返回保留策略 + 日志分区 + 离线备份记录。
 */
@Controller
class AdminDataMaintenanceQueryResolver(
  private val systemConfigQueryService: SystemConfigQueryService,
  private val logPartitionQueryService: LogPartitionQueryService,
) {
  /**
   * 数据维护页总览(保留策略参数 + 三类日志分区 + 备份记录分页)
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_SYSTEM_CONFIG_READ')")
  fun dataMaintenanceOverview(
    @Argument input: LogBackupRecordListInput?,
  ): DataMaintenanceOverviewVo {
    val settings = systemConfigQueryService.listByGroup(SystemSettingGroup.DATA_MAINTENANCE)
    val retentionDaysByTable = buildRetentionDays(settings)

    val partitions =
      DomainLogTableType.entries.flatMap { table ->
        val retentionDays = retentionDaysByTable.getValue(table)
        logPartitionQueryService.listPartitions(table).map { partition ->
          LogPartitionVoMapper.map(
            partition = partition,
            retentionDays = retentionDays,
            backedUp = logPartitionQueryService.isBackedUp(partition.partitionName),
          )
        }
      }

    val recordsPage =
      logPartitionQueryService.listBackupRecords(
        logTable = input?.logTable?.toDomain(),
        page = input?.page ?: 0,
        size = input?.size ?: 10,
      )

    return DataMaintenanceOverviewVo(
      settings = settings.map { it.toGraphQL() },
      partitions = partitions,
      backupRecords =
        LogBackupRecordPage(
          list = recordsPage.content.map { it.toGraphQL() },
          pageInfo =
            PageInfoVo(
              total = recordsPage.totalElements.toInt(),
              pageSize = recordsPage.size,
              pageNum = recordsPage.number,
              hasNext = recordsPage.hasNext(),
              hasPrevious = recordsPage.hasPrevious(),
              totalPages = recordsPage.totalPages,
            ),
        ),
    )
  }

  /**
   * DATA_MAINTENANCE 组参数 → 各日志表保留天数(未配置按默认 90 天)
   */
  private fun buildRetentionDays(settings: List<SystemSettingsModel>): Map<DomainLogTableType, Int> {
    val byKey = settings.associateBy { it.settingKey }
    return DomainLogTableType.entries.associateWith { table ->
      byKey[table.retentionKey]?.settingValue?.toIntOrNull() ?: DEFAULT_RETENTION_DAYS
    }
  }

  companion object {
    private const val DEFAULT_RETENTION_DAYS = 90
  }
}

/**
 * 生成的 GraphQL LogTableType → 域 LogTableType
 */
fun LogTableType.toDomain(): DomainLogTableType = DomainLogTableType.valueOf(name)
