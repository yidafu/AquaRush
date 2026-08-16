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

package dev.yidafu.aqua.api.service.ops

import dev.yidafu.aqua.common.domain.model.LogBackupRecordModel
import dev.yidafu.aqua.logging.partition.domain.model.enums.LogTableType

/**
 * 日志分区离线备份服务接口(管理后台,PERMISSION_SYSTEM_CONFIG_WRITE)
 * 与 PRD 11-system-settings.md §F-SYS-006 保持一致:超期分区「转储冷表 + 导出数据文件」。
 */
interface LogPartitionMutationService {
  /**
   * 备份指定超期月分区(转储冷表 + 导出 gzip 文件;成功后分区从热表移除)
   * @param retentionDays 该表保留天数(超期判定)
   */
  fun backup(
    logTable: LogTableType,
    partitionName: String,
    operatorId: Long?,
    operatorName: String,
    retentionDays: Int,
  ): LogBackupRecordModel
}
