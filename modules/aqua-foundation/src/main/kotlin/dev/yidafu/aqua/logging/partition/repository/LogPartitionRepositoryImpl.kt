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

import dev.yidafu.aqua.logging.partition.domain.LogPartitionRules
import dev.yidafu.aqua.logging.partition.domain.model.LogPartitionInfo
import dev.yidafu.aqua.logging.partition.domain.model.enums.LogTableType
import org.postgresql.PGConnection
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.query
import org.springframework.jdbc.datasource.DataSourceUtils
import org.springframework.stereotype.Repository
import java.io.BufferedOutputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.GZIPOutputStream

/**
 * 日志分区元数据/操作 Repository 实现(PostgreSQL)
 * 日志表按月分区,备份直接操作分区表:分区导出(COPY)、并入年度冷表、DROP 分区、事务级 advisory lock。
 */
@Repository
class LogPartitionRepositoryImpl(
  private val jdbcTemplate: JdbcTemplate,
) : LogPartitionRepository {
  private val logger = LoggerFactory.getLogger(LogPartitionRepositoryImpl::class.java)

  /** 冷表 schema(按年份组织,如 log_archive.api_logs_2026) */
  private val coldSchema = "log_archive"

  override fun isPartitioned(logTable: LogTableType): Boolean {
    val relkind =
      jdbcTemplate.queryForObject(
        "SELECT relkind FROM pg_class WHERE relname = ? AND relnamespace = 'public'::regnamespace",
        String::class.java,
        logTable.tableName,
      ) ?: return false
    return relkind == "p"
  }

  override fun listPartitions(logTable: LogTableType): List<LogPartitionInfo> {
    val rows =
      jdbcTemplate.query(
        """
        SELECT c.relname AS partition_name,
               pg_get_expr(c.relpartbound, c.oid) AS bound,
               c.reltuples::bigint AS row_count,
               pg_total_relation_size(c.oid) AS size_bytes
        FROM pg_inherits i
        JOIN pg_class c ON c.oid = i.inhrelid
        JOIN pg_class p ON p.oid = i.inhparent
        WHERE p.relname = ? AND p.relkind = 'p'
        ORDER BY c.relname
        """.trimIndent(),
        { rs, _ ->
          val name = rs.getString("partition_name")
          val bound = rs.getString("bound")
          val month = LogPartitionRules.monthOfPartition(name)
          val fromTo = parseBound(bound)
          LogPartitionInfo(
            logTable = logTable,
            partitionName = name,
            partitionMonth = month,
            rowCount = rs.getLong("row_count"),
            sizeBytes = rs.getLong("size_bytes"),
            isDefault = bound == null,
            isCurrentMonth = month == LocalDateTime.now().format(monthFmt),
            fromBound = fromTo?.first,
            toBound = fromTo?.second,
          )
        },
        logTable.tableName,
      )
    return rows
  }

  override fun createMonthPartition(
    logTable: LogTableType,
    yearMonth: String,
  ) {
    val (from, to) = monthRange(yearMonth)
    val partitionName = partitionName(logTable, yearMonth)
    jdbcTemplate.execute(
      "CREATE TABLE IF NOT EXISTS $partitionName PARTITION OF ${logTable.tableName} " +
        "FOR VALUES FROM ('$from') TO ('$to')",
    )
  }

  override fun ensureColdYearTable(
    logTable: LogTableType,
    year: Int,
  ) {
    // 幂等补建冷表 schema(迁移建过一次;防止 schema 被重置/删除后备份失败)
    jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS $coldSchema")
    val coldTable = coldTableName(logTable, year)
    jdbcTemplate.execute(
      "CREATE TABLE IF NOT EXISTS $coldTable (LIKE public.${logTable.tableName} INCLUDING ALL)",
    )
  }

  override fun appendToColdTable(
    logTable: LogTableType,
    year: Int,
    partitionName: String,
  ): Int {
    val coldTable = coldTableName(logTable, year)
    val inserted =
      jdbcTemplate.update("INSERT INTO $coldTable SELECT * FROM $partitionName")
    logger.info("Appended {} rows from {} into cold table {}", inserted, partitionName, coldTable)
    return inserted
  }

  override fun exportPartitionToGzip(
    partitionName: String,
    targetFile: Path,
  ): GzipExportResult {
    Files.createDirectories(targetFile.parent)
    val sha256 = MessageDigest.getInstance("SHA-256")
    val counter = CountingOutputStream()
    Files.newOutputStream(targetFile).use { raw ->
      GZIPOutputStream(BufferedOutputStream(TeeingOutputStream(raw, sha256, counter))).use { gzip ->
        val connection = DataSourceUtils.getConnection(requireNotNull(jdbcTemplate.dataSource))
        try {
          val pg = connection.unwrap(PGConnection::class.java)
          val sql = "COPY $partitionName TO STDOUT WITH (FORMAT csv, HEADER true)"
          pg.copyAPI.copyOut(sql, gzip)
        } finally {
          DataSourceUtils.releaseConnection(connection, jdbcTemplate.dataSource)
        }
      }
    }
    val checksum = sha256.digest().joinToString("") { "%02x".format(it) }
    logger.info("Exported partition {} to {} ({} bytes, sha256 {})", partitionName, targetFile, counter.count, checksum)
    return GzipExportResult(sizeBytes = counter.count, sha256 = checksum)
  }

  override fun dropPartition(partitionName: String) {
    jdbcTemplate.execute("DROP TABLE $partitionName")
    logger.info("Dropped partition {}", partitionName)
  }

  override fun tryAcquireBackupLock(partitionName: String): Boolean {
    val acquired =
      jdbcTemplate.queryForObject(
        "SELECT pg_try_advisory_xact_lock(hashtext(?))",
        Boolean::class.java,
        "aquarush:log-backup:$partitionName",
      ) ?: false
    logger.debug("Advisory lock for {} acquired: {}", partitionName, acquired)
    return acquired
  }

  // ==================== helpers ====================

  private val monthFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

  /** 分区名:{table}_{yyyy}_{MM} */
  private fun partitionName(
    logTable: LogTableType,
    yearMonth: String,
  ): String = "${logTable.tableName}_${yearMonth.replace("-", "_")}"

  /** 冷表名:log_archive.{table}_{yyyy} */
  private fun coldTableName(
    logTable: LogTableType,
    year: Int,
  ): String = "$coldSchema.${logTable.tableName}_$year"

  /** 月份 [yyyy-MM-01 00:00:00, 下月 00:00:00) */
  private fun monthRange(yearMonth: String): Pair<String, String> {
    val first = LogPartitionRules.firstDayOfMonth(yearMonth)
    val from = first.atStartOfDay()
    val to = first.plusMonths(1).atStartOfDay()
    return from.format(timestampFmt) to to.format(timestampFmt)
  }

  /** 解析 pg_get_expr(relpartbound) 形如 FOR VALUES FROM ('2026-01-01 00:00:00') TO ('2026-02-01 00:00:00') */
  private fun parseBound(bound: String?): Pair<LocalDateTime, LocalDateTime>? {
    if (bound == null) return null
    val match = boundPattern.matchEntire(bound) ?: return null
    val from = LocalDateTime.parse(match.groupValues[1], timestampFmt)
    val to = LocalDateTime.parse(match.groupValues[2], timestampFmt)
    return from to to
  }

  private val timestampFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
  private val boundPattern = Regex("^FOR VALUES FROM \\('([^']+)'\\) TO \\('([^']+)'\\)$")

  /** 双输出流:同时写目标流、SHA-256 摘要与字节计数 */
  private class TeeingOutputStream(
    private val target: OutputStream,
    private val digest: MessageDigest,
    private val counter: CountingOutputStream,
  ) : OutputStream() {
    override fun write(b: Int) {
      target.write(b)
      digest.update(b.toByte())
      counter.count++
    }

    override fun write(
      b: ByteArray,
      off: Int,
      len: Int,
    ) {
      target.write(b, off, len)
      digest.update(b, off, len)
      counter.count += len
    }
  }

  private class CountingOutputStream {
    var count: Long = 0
  }
}
