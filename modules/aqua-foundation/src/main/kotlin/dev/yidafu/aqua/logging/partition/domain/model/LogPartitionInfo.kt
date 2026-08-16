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

package dev.yidafu.aqua.logging.partition.domain.model

import dev.yidafu.aqua.logging.partition.domain.model.enums.LogTableType
import java.time.LocalDateTime

/**
 * 日志表月度分区信息(来自 pg_inherits/pg_class,数据维护页展示,F-SYS-006)
 */
data class LogPartitionInfo(
  /** 所属日志表 */
  val logTable: LogTableType,
  /** 分区名,如 api_logs_2026_01;DEFAULT 分区名为 {table}_default */
  val partitionName: String,
  /** 分区月份 yyyy-MM;DEFAULT 分区为 null */
  val partitionMonth: String?,
  /** 行数估算(reltuples,展示用;备份时记录精确行数) */
  val rowCount: Long,
  /** 分区占用字节数(pg_total_relation_size) */
  val sizeBytes: Long,
  /** 是否为 DEFAULT 兜底分区 */
  val isDefault: Boolean,
  /** 是否为当前月分区(仍在写入,不可备份) */
  val isCurrentMonth: Boolean,
  /** 分区下界(created_at >= fromBound,恢复/边界解析用) */
  val fromBound: LocalDateTime?,
  /** 分区上界(created_at < toBound) */
  val toBound: LocalDateTime?,
)
