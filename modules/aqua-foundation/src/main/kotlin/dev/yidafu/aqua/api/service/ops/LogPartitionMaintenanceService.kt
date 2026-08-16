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
 * 日志分区维护服务接口(F-SYS-006)
 * 分区预建与自动备份(仅 admin 服务调度调用)。
 */
interface LogPartitionMaintenanceService {
  /**
   * 为三类日志热表补建未来月度分区(当前月 / +1 / +2 月,幂等)
   */
  fun ensureFuturePartitions(monthsAhead: Int = 2)

  /**
   * 自动备份指定日志表全部超期分区(转储冷表 + 导出文件;单分区失败不影响其余)
   * @return 本次成功备份的记录列表
   */
  fun backupExpiredPartitions(
    logTable: LogTableType,
    retentionDays: Int,
    operatorName: String = SYSTEM_OPERATOR,
  ): List<LogBackupRecordModel>

  companion object {
    /** 自动备份操作人标识 */
    const val SYSTEM_OPERATOR = "system"
  }
}
