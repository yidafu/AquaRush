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

package dev.yidafu.aqua.logging.partition.domain

import dev.yidafu.aqua.logging.partition.domain.model.LogPartitionInfo
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 日志分区规则(纯函数,可单测;F-SYS-006)
 * 与 PRD 11-system-settings.md §F-SYS-006 保持一致:超期判定与可备份判定。
 */
object LogPartitionRules {
  /**
   * 是否超期:分区上界(含月末)早于「now - 保留天数」即视为超期数据。
   * @param now 判定基准时间(注入便于测试)
   */
  fun isExpired(
    partition: LogPartitionInfo,
    retentionDays: Int,
    now: LocalDateTime = LocalDateTime.now(),
  ): Boolean {
    val toBound = partition.toBound ?: return false
    return !toBound.isAfter(now.minusDays(retentionDays.toLong()))
  }

  /**
   * 是否可备份:历史月(非当前月)&& 非 DEFAULT && 已超期。
   * 已备份(存在 SUCCEEDED 记录)由调用方结合记录表判断。
   */
  fun isBackupable(
    partition: LogPartitionInfo,
    retentionDays: Int,
    now: LocalDateTime = LocalDateTime.now(),
  ): Boolean = !partition.isCurrentMonth && !partition.isDefault && isExpired(partition, retentionDays, now)

  /**
   * 由分区名解析年份 yyyy(冷表按年份组织,如 api_logs_2026_01 → 2026)
   */
  fun yearOfPartition(partitionName: String): Int? = monthOfPartition(partitionName)?.take(4)?.toIntOrNull()

  /**
   * 由分区名解析月份 yyyy-MM(如 api_logs_2026_01 → 2026-01);非月度分区(如 DEFAULT)返回 null
   */
  fun monthOfPartition(partitionName: String): String? {
    val match = MONTH_PATTERN.matchEntire(partitionName) ?: return null
    val y = match.groupValues[1].toIntOrNull() ?: return null
    val m = match.groupValues[2].toIntOrNull() ?: return null
    return if (m in 1..12) "%04d-%02d".format(y, m) else null
  }

  private val MONTH_PATTERN = Regex("^.*_(\\d{4})_(\\d{2})$")

  /**
   * 分区月份(如 2026-01)对应的首日
   */
  fun firstDayOfMonth(month: String): LocalDate {
    val (y, m) = month.split("-").map { it.toInt() }
    return LocalDate.of(y, m, 1)
  }
}
