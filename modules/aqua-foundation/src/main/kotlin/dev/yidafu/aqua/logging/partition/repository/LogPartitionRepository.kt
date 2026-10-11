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

package dev.yidafu.aqua.logging.partition.repository

import dev.yidafu.aqua.logging.partition.domain.model.LogPartitionInfo
import dev.yidafu.aqua.logging.partition.domain.model.enums.LogTableType
import java.nio.file.Path

/**
 * 日志分区元数据/操作 Repository(F-SYS-006)
 * 仅 PostgreSQL 实现(日志表按月分区,备份直接操作分区表)。
 */
interface LogPartitionRepository {
  /**
   * 热表是否为分区父表(relkind='p')
   */
  fun isPartitioned(logTable: LogTableType): Boolean

  /**
   * 热表全部月度分区(含 DEFAULT 兜底分区,恒按分区名排序)
   */
  fun listPartitions(logTable: LogTableType): List<LogPartitionInfo>

  /**
   * 为热表创建指定月份分区(幂等:CREATE TABLE IF NOT EXISTS ... PARTITION OF),yearMonth 形如 2026-01。
   * DEFAULT 兜底分区已有该月数据时自动暂存腾挪后再建(否则 PG 拒绝,当月分区永远建不出来)。
   */
  fun createMonthPartition(
    logTable: LogTableType,
    yearMonth: String,
  )

  /**
   * 确保按年份组织的冷表存在(懒建 log_archive.{table}_{year},结构与热表一致,LIKE INCLUDING ALL)
   */
  fun ensureColdYearTable(
    logTable: LogTableType,
    year: Int,
  )

  /**
   * 将分区数据并入年度冷表(INSERT INTO ... SELECT),返回并入行数(精确,作为备份记录 rowCount)
   */
  fun appendToColdTable(
    logTable: LogTableType,
    year: Int,
    partitionName: String,
  ): Int

  /**
   * 将分区数据流式导出为 gzip CSV 文件(COPY ... TO STDOUT),返回 SHA-256 与文件字节数
   */
  fun exportPartitionToGzip(
    partitionName: String,
    targetFile: Path,
  ): GzipExportResult

  /**
   * 直接删除分区(DROP TABLE {partition},数据离线前已并入冷表并导出文件)
   */
  fun dropPartition(partitionName: String)

  /**
   * 会话级事务 advisory lock(防自动任务与手动操作同分区并发备份),成功返回 true
   */
  fun tryAcquireBackupLock(partitionName: String): Boolean
}

/**
 * gzip 导出结果
 */
data class GzipExportResult(
  /** 文件字节数 */
  val sizeBytes: Long,
  /** SHA-256 十六进制 */
  val sha256: String,
)
