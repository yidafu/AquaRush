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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package dev.yidafu.aqua.logging.partition.repository

import dev.yidafu.aqua.logging.partition.domain.LogPartitionRules
import dev.yidafu.aqua.logging.partition.domain.model.LogPartitionInfo
import dev.yidafu.aqua.logging.partition.domain.model.enums.LogTableType
import org.jetbrains.exposed.v1.core.VarCharColumnType
import org.jetbrains.exposed.v1.core.statements.StatementType
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.postgresql.PGConnection
import org.slf4j.LoggerFactory
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
 *
 * 事务性(Exposed 迁移 P5 收口):主事务管理器是 Exposed `SpringTransactionManager`,本类**全部**
 * 数据访问都走 Exposed —— `transaction(database) { exec(...) }` 会加入 `@Transactional` 已开启的
 * 同一个事务(同一 Database 时 Exposed 不新开事务),因此
 * `LogBackupTransactionService.doBackup` 的
 * 「advisory xact lock → 导出 → 并入冷表 → DROP 分区 → 落库」是原子的。
 * 这里不再使用 Spring `JdbcTemplate` / `DataSourceUtils`,也就不再依赖「事务连接被绑定到
 * DataSource 的 `ConnectionHolder`」这一隐式约定 —— 事务边界由 Exposed 一处决定。
 *
 * `COPY ... TO STDOUT` 只能用 JDBC `CopyManager`,因此这一处从**当前 Exposed 事务**取原始连接
 * (`Transaction.connection.connection`):COPY 与同事务的 DDL/DML 落在同一条连接、同一个事务里。
 */
@Repository
class LogPartitionRepositoryImpl(
  private val database: Database,
) : LogPartitionRepository {
  private val logger = LoggerFactory.getLogger(LogPartitionRepositoryImpl::class.java)

  /** 冷表 schema(按年份组织,如 log_archive.api_logs_2026) */
  private val coldSchema = "log_archive"

  override fun isPartitioned(logTable: LogTableType): Boolean =
    transaction(database) {
      exec(
        "SELECT relkind FROM pg_class WHERE relname = ? AND relnamespace = 'public'::regnamespace",
        listOf(VarCharColumnType() to logTable.tableName),
      ) { rs -> if (rs.next()) rs.getString(1) else null }
    } == "p"

  override fun listPartitions(logTable: LogTableType): List<LogPartitionInfo> =
    transaction(database) {
      exec(
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
        listOf(VarCharColumnType() to logTable.tableName),
      ) { rs ->
        buildList {
          while (rs.next()) {
            val name = rs.getString("partition_name")
            val bound = rs.getString("bound")
            val month = LogPartitionRules.monthOfPartition(name)
            val fromTo = parseBound(bound)
            add(
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
              ),
            )
          }
        }
      } ?: emptyList()
    }

  override fun createMonthPartition(
    logTable: LogTableType,
    yearMonth: String,
  ) {
    val (from, to) = monthRange(yearMonth)
    val partitionName = partitionName(logTable, yearMonth)
    transaction(database) {
      exec(
        "CREATE TABLE IF NOT EXISTS $partitionName PARTITION OF ${logTable.tableName} " +
          "FOR VALUES FROM ('$from') TO ('$to')",
      )
    }
  }

  override fun ensureColdYearTable(
    logTable: LogTableType,
    year: Int,
  ) {
    val coldTable = coldTableName(logTable, year)
    transaction(database) {
      // 幂等补建冷表 schema(迁移建过一次;防止 schema 被重置/删除后备份失败)
      exec("CREATE SCHEMA IF NOT EXISTS $coldSchema")
      exec("CREATE TABLE IF NOT EXISTS $coldTable (LIKE public.${logTable.tableName} INCLUDING ALL)")
    }
  }

  override fun appendToColdTable(
    logTable: LogTableType,
    year: Int,
    partitionName: String,
  ): Int {
    val coldTable = coldTableName(logTable, year)
    // RETURNING 1 逐行计数:与 JdbcTemplate.update 的受影响行数语义一致(而不是查 count(*))
    val inserted =
      transaction(database) {
        // RETURNING 逐行计数(与 JdbcTemplate.update 的受影响行数同义)。
        // 必须显式声明 StatementType.SELECT:Exposed 默认按 DML 走 executeUpdate,
        // 而 PG 对带 RETURNING 且「预期无结果集」的调用会报「传回预期之外的结果」。
        exec(
          "INSERT INTO $coldTable SELECT * FROM $partitionName RETURNING 1",
          emptyList(),
          StatementType.SELECT,
        ) { rs ->
          var rows = 0
          while (rs.next()) rows++
          rows
        } ?: 0
      }
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
        transaction(database) {
          // Exposed 事务持有原始 JDBC 连接(ExposedConnection.connection);COPY 走它,
          // 从而与同事务的 DDL/DML 同连接、同事务
          val connection = this.connection.connection as java.sql.Connection
          val pg = connection.unwrap(PGConnection::class.java)
          val sql = "COPY $partitionName TO STDOUT WITH (FORMAT csv, HEADER true)"
          pg.copyAPI.copyOut(sql, gzip)
        }
      }
    }
    val checksum = sha256.digest().joinToString("") { "%02x".format(it) }
    logger.info("Exported partition {} to {} ({} bytes, sha256 {})", partitionName, targetFile, counter.count, checksum)
    return GzipExportResult(sizeBytes = counter.count, sha256 = checksum)
  }

  override fun dropPartition(partitionName: String) {
    transaction(database) {
      exec("DROP TABLE $partitionName")
    }
    logger.info("Dropped partition {}", partitionName)
  }

  override fun tryAcquireBackupLock(partitionName: String): Boolean {
    val acquired =
      transaction(database) {
        exec(
          "SELECT pg_try_advisory_xact_lock(hashtext(?))",
          listOf(VarCharColumnType() to "aquarush:log-backup:$partitionName"),
        ) { rs -> if (rs.next()) rs.getBoolean(1) else false }
      } ?: false
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
