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

import dev.yidafu.aqua.common.testsupport.PostgresTestContainer
import dev.yidafu.aqua.logging.partition.domain.model.enums.LogTableType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.nio.file.Files
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import java.util.zip.GZIPInputStream
import kotlin.concurrent.thread

/**
 * [LogPartitionRepositoryImpl] 的真实 PostgreSQL 集成测试(Testcontainers)。
 *
 * 覆盖 Exposed 化之后的分区运维链路:建分区(pg_class 可见/边界解析)、冷表补建与并入行数、
 * `COPY ... TO STDOUT` 导出(字节数 + sha256 + gzip/CSV 内容)、DROP 分区、事务级 advisory lock 互斥。
 *
 * 用独立库名 `aquafoundationlogs`(与 `FoundationPgTestSupport` 的 `aquafoundationtables` 隔离):
 * 这里需要 `api_logs` 是**真正的分区父表**,而 `SchemaUtils.create(ApiLogsTable)` 建出的是普通表。
 */
class LogPartitionRepositoryImplPgTest :
  FunSpec({
    val database = PostgresTestContainer.connect("aquafoundationlogs")
    val repository = LogPartitionRepositoryImpl(database)
    val partition = "api_logs_2026_03"

    fun sql(statement: String) {
      transaction(database) { exec(statement) }
    }

    fun <T> query(
      statement: String,
      transform: (java.sql.ResultSet) -> T,
    ): T? = transaction(database) { exec(statement, transform = transform) }

    fun reset() {
      sql("DROP SCHEMA IF EXISTS log_archive CASCADE")
      sql("DROP TABLE IF EXISTS api_logs CASCADE")
      sql(
        """
        CREATE TABLE api_logs (
          id bigint NOT NULL,
          method varchar(16) NOT NULL,
          path varchar(255) NOT NULL,
          created_at timestamp NOT NULL,
          PRIMARY KEY (id, created_at)
        ) PARTITION BY RANGE (created_at)
        """.trimIndent(),
      )
    }

    fun seedRows() {
      sql(
        """
        INSERT INTO api_logs (id, method, path, created_at)
        VALUES (1, 'GET', '/api/a', '2026-03-05 10:00:00'),
               (2, 'POST', '/api/b', '2026-03-06 11:00:00')
        """.trimIndent(),
      )
    }

    beforeTest { reset() }

    test("createMonthPartition 幂等建分区,listPartitions 解析边界与当月标记") {
      repository.isPartitioned(LogTableType.API_LOG).shouldBeTrue()

      repository.createMonthPartition(LogTableType.API_LOG, "2026-03")
      repository.createMonthPartition(LogTableType.API_LOG, "2026-03")

      val partitions = repository.listPartitions(LogTableType.API_LOG)
      partitions.shouldHaveSize(1)
      val info = partitions.single()
      info.partitionName shouldBe partition
      info.partitionMonth shouldBe "2026-03"
      info.fromBound shouldBe LocalDateTime.of(2026, 3, 1, 0, 0)
      info.toBound shouldBe LocalDateTime.of(2026, 4, 1, 0, 0)
      info.isDefault.shouldBeFalse()
    }

    test("createMonthPartition:DEFAULT 已有该月行时腾挪建分区,范围外行留在 DEFAULT") {
      // 复刻全新库首启:种子日志先落 DEFAULT,当月分区随后才预建(PG 默认拒绝此操作)
      sql("CREATE TABLE api_logs_default PARTITION OF api_logs DEFAULT")
      sql(
        """
        INSERT INTO api_logs (id, method, path, created_at)
        VALUES (1, 'GET', '/api/a', '2026-03-05 10:00:00'),
               (2, 'POST', '/api/b', '2026-03-06 11:00:00'),
               (3, 'GET', '/api/c', '2026-05-01 12:00:00')
        """.trimIndent(),
      )

      repository.createMonthPartition(LogTableType.API_LOG, "2026-03")
      // 分区已存在后的重复调用仍是无冲突的幂等 no-op
      repository.createMonthPartition(LogTableType.API_LOG, "2026-03")

      query("SELECT count(*) FROM api_logs_2026_03") { rs ->
        rs.next()
        rs.getInt(1)
      } shouldBe 2
      query("SELECT count(*) FROM api_logs_default") { rs ->
        rs.next()
        rs.getInt(1)
      } shouldBe 1
      repository.listPartitions(LogTableType.API_LOG).count { it.isDefault } shouldBe 1
    }

    test("ensureColdYearTable + appendToColdTable 返回真实插入行数") {
      repository.createMonthPartition(LogTableType.API_LOG, "2026-03")
      seedRows()

      repository.ensureColdYearTable(LogTableType.API_LOG, 2026)
      // 冷表已存在时重复补建不报错(IF NOT EXISTS)
      repository.ensureColdYearTable(LogTableType.API_LOG, 2026)

      repository.appendToColdTable(LogTableType.API_LOG, 2026, partition) shouldBe 2
      query("SELECT count(*) FROM log_archive.api_logs_2026") { rs ->
        rs.next()
        rs.getInt(1)
      } shouldBe 2

      // 同一年冷表聚合多个月份分区:第二条分区并入后累计 4 行
      repository.createMonthPartition(LogTableType.API_LOG, "2026-04")
      sql(
        """
        INSERT INTO api_logs (id, method, path, created_at)
        VALUES (3, 'GET', '/api/c', '2026-04-07 09:00:00'),
               (4, 'GET', '/api/d', '2026-04-08 09:00:00')
        """.trimIndent(),
      )
      repository.appendToColdTable(LogTableType.API_LOG, 2026, "api_logs_2026_04") shouldBe 2
      query("SELECT count(*) FROM log_archive.api_logs_2026") { rs ->
        rs.next()
        rs.getInt(1)
      } shouldBe 4
    }

    test("exportPartitionToGzip 导出 gzip CSV,字节数与 sha256 与文件一致") {
      repository.createMonthPartition(LogTableType.API_LOG, "2026-03")
      seedRows()
      val target = Files.createTempDirectory("log-partition-export").resolve("$partition.csv.gz")

      val result = repository.exportPartitionToGzip(partition, target)

      result.sizeBytes shouldBe Files.size(target)
      val fileDigest =
        MessageDigest
          .getInstance("SHA-256")
          .digest(Files.readAllBytes(target))
          .joinToString("") { "%02x".format(it) }
      result.sha256 shouldBe fileDigest

      val csv =
        GZIPInputStream(Files.newInputStream(target)).bufferedReader().use { it.readText() }
      csv shouldContain "id,method,path,created_at"
      csv shouldContain "/api/a"
      csv shouldContain "/api/b"
    }

    test("dropPartition 后分区从 listPartitions 消失") {
      repository.createMonthPartition(LogTableType.API_LOG, "2026-03")
      repository.listPartitions(LogTableType.API_LOG).shouldHaveSize(1)

      repository.dropPartition(partition)

      repository.listPartitions(LogTableType.API_LOG).shouldHaveSize(0)
      query("SELECT to_regclass('public.$partition')") { rs ->
        rs.next()
        rs.getString(1)
      } shouldBe null
    }

    test("tryAcquireBackupLock:同事务内可重入,并发事务互斥") {
      // 独立事务:拿到后提交即释放
      repository.tryAcquireBackupLock(partition).shouldBeTrue()

      val holding = CountDownLatch(1)
      val release = CountDownLatch(1)
      val holder =
        thread(name = "advisory-lock-holder") {
          transaction(database) {
            repository.tryAcquireBackupLock(partition).shouldBeTrue()
            holding.countDown()
            release.await(10, TimeUnit.SECONDS)
          }
        }
      try {
        holding.await(10, TimeUnit.SECONDS).shouldBeTrue()
        val contended = AtomicReference<Boolean>()
        transaction(database) {
          contended.set(repository.tryAcquireBackupLock(partition))
        }
        contended.get().shouldBeFalse()
      } finally {
        release.countDown()
        holder.join(10_000)
      }
    }
  })
