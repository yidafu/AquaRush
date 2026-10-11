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

package dev.yidafu.aqua.logging.partition.service

import dev.yidafu.aqua.common.domain.model.LogBackupRecordModel
import dev.yidafu.aqua.logging.partition.config.DataMaintenanceProperties
import dev.yidafu.aqua.logging.partition.domain.exception.BackupExecutionException
import dev.yidafu.aqua.logging.partition.domain.exception.LogPartitionAlreadyBackedUpException
import dev.yidafu.aqua.logging.partition.domain.exception.LogPartitionBackupFailedException
import dev.yidafu.aqua.logging.partition.domain.exception.LogPartitionBackupInProgressException
import dev.yidafu.aqua.logging.partition.domain.exception.LogPartitionNotBackupableException
import dev.yidafu.aqua.logging.partition.domain.exception.LogPartitionNotFoundException
import dev.yidafu.aqua.logging.partition.domain.model.LogPartitionInfo
import dev.yidafu.aqua.logging.partition.domain.model.enums.LogBackupStatus
import dev.yidafu.aqua.logging.partition.domain.model.enums.LogTableType
import dev.yidafu.aqua.logging.partition.domain.repository.LogBackupRecordRepository
import dev.yidafu.aqua.logging.partition.repository.LogPartitionRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime

class LogBackupServiceTest :
  FunSpec({
    var partitionRepository: LogPartitionRepository = mockk()
    var backupRecordRepository: LogBackupRecordRepository = mockk()
    var transactionService: LogBackupTransactionService = mockk()
    var backupDir: Path = mockk()
    var service: LogBackupService = mockk()

    val expiredPartition =
      LogPartitionInfo(
        logTable = LogTableType.API_LOG,
        partitionName = "aqua_api_logs_2026_01",
        partitionMonth = "2026-01",
        rowCount = 10,
        sizeBytes = 1024,
        isDefault = false,
        isCurrentMonth = false,
        fromBound = LocalDateTime.of(2026, 1, 1, 0, 0),
        toBound = LocalDateTime.of(2026, 2, 1, 0, 0),
      )

    beforeTest {
      partitionRepository = mockk(relaxed = true)
      backupRecordRepository = mockk(relaxed = true)
      transactionService = mockk(relaxed = true)
      backupDir = Files.createTempDirectory("log-backup-test")
      service =
        LogBackupService(
          partitionRepository,
          backupRecordRepository,
          transactionService,
          DataMaintenanceProperties(backupDir = backupDir.toString()),
        )
      every { partitionRepository.listPartitions(LogTableType.API_LOG) } returns listOf(expiredPartition)
      every { backupRecordRepository.existsByPartitionNameAndStatusIn(any(), any()) } returns false
      // Spring Data save 为泛型方法,relaxed mock 默认返回类型擦除值会抛 ClassCastException,改为返回入参
      every { backupRecordRepository.save(any<LogBackupRecordModel>()) } answers { firstArg() }
    }

    test("backup succeeds on valid expired partition") {
      val saved =
        LogBackupRecordModel(
          id = 1L,
          logTable = LogTableType.API_LOG,
          partitionName = "aqua_api_logs_2026_01",
          partitionMonth = "2026-01",
          coldTableName = "log_archive.aqua_api_logs_2026",
          status = LogBackupStatus.SUCCEEDED,
        )
      every { transactionService.doBackup(expiredPartition, 1L, "admin") } returns saved

      val result = service.backup(LogTableType.API_LOG, "aqua_api_logs_2026_01", 1L, "admin", retentionDays = 90)

      result shouldBe saved
      verify { transactionService.doBackup(expiredPartition, 1L, "admin") }
    }

    test("backup rejects unknown partition") {
      every { partitionRepository.listPartitions(LogTableType.API_LOG) } returns emptyList()
      shouldThrow<LogPartitionNotFoundException> {
        service.backup(LogTableType.API_LOG, "aqua_api_logs_2026_01", 1L, "admin", 90)
      }
    }

    test("backup rejects default partition") {
      val defaultPartition = expiredPartition.copy(partitionName = "aqua_api_logs_default", partitionMonth = null, isDefault = true)
      every { partitionRepository.listPartitions(LogTableType.API_LOG) } returns listOf(defaultPartition)
      shouldThrow<LogPartitionNotBackupableException> {
        service.backup(LogTableType.API_LOG, "aqua_api_logs_default", 1L, "admin", 90)
      }
    }

    test("backup rejects current month partition") {
      val current = expiredPartition.copy(isCurrentMonth = true)
      every { partitionRepository.listPartitions(LogTableType.API_LOG) } returns listOf(current)
      shouldThrow<LogPartitionNotBackupableException> {
        service.backup(LogTableType.API_LOG, "aqua_api_logs_2026_01", 1L, "admin", 90)
      }
    }

    test("backup rejects non-expired partition") {
      val fresh = expiredPartition.copy(toBound = LocalDateTime.now().plusDays(1))
      every { partitionRepository.listPartitions(LogTableType.API_LOG) } returns listOf(fresh)
      shouldThrow<LogPartitionNotBackupableException> {
        service.backup(LogTableType.API_LOG, "aqua_api_logs_2026_01", 1L, "admin", 90)
      }
    }

    test("backup rejects already backed up partition") {
      every { backupRecordRepository.existsByPartitionNameAndStatusIn(any(), any()) } returns true
      shouldThrow<LogPartitionAlreadyBackedUpException> {
        service.backup(LogTableType.API_LOG, "aqua_api_logs_2026_01", 1L, "admin", 90)
      }
    }

    test("backup failure before export deletes partial file and records FAILED") {
      every { transactionService.doBackup(any(), any(), any()) } throws BackupExecutionException(false, RuntimeException("copy failed"))
      val target = Path.of(backupDir.toString(), "aqua_api_logs_2026_01.csv.gz")
      Files.writeString(target, "partial")

      shouldThrow<LogPartitionBackupFailedException> {
        service.backup(LogTableType.API_LOG, "aqua_api_logs_2026_01", 1L, "admin", 90)
      }

      Files.exists(target) shouldBe false
      verify {
        backupRecordRepository.save(
          match<LogBackupRecordModel> {
            it.status == LogBackupStatus.FAILED &&
              it.partitionName == "aqua_api_logs_2026_01" &&
              it.errorMessage == "copy failed"
          },
        )
      }
    }

    test("backup failure after export keeps file and records FAILED with path") {
      every { transactionService.doBackup(any(), any(), any()) } throws BackupExecutionException(true, RuntimeException("append failed"))

      shouldThrow<LogPartitionBackupFailedException> {
        service.backup(LogTableType.API_LOG, "aqua_api_logs_2026_01", 1L, "admin", 90)
      }

      verify {
        backupRecordRepository.save(
          match<LogBackupRecordModel> {
            it.status == LogBackupStatus.FAILED &&
              it.filePath == Path.of(backupDir.toString(), "aqua_api_logs_2026_01.csv.gz").toString()
          },
        )
      }
    }

    test("in-progress conflict passes through without FAILED record") {
      every { transactionService.doBackup(any(), any(), any()) } throws LogPartitionBackupInProgressException("aqua_api_logs_2026_01")
      shouldThrow<LogPartitionBackupInProgressException> {
        service.backup(LogTableType.API_LOG, "aqua_api_logs_2026_01", 1L, "admin", 90)
      }
      verify(exactly = 0) { backupRecordRepository.save(any()) }
    }

    test("backupExpiredPartitions backs up expired and skips failures") {
      val expired2 =
        expiredPartition.copy(
          partitionName = "aqua_api_logs_2026_02",
          partitionMonth = "2026-02",
          toBound = LocalDateTime.of(2026, 3, 1, 0, 0),
        )
      every { partitionRepository.listPartitions(LogTableType.API_LOG) } returns listOf(expiredPartition, expired2)
      every { backupRecordRepository.existsByPartitionNameAndStatusIn("aqua_api_logs_2026_02", any()) } returns true // 已备份跳过
      every { transactionService.doBackup(expiredPartition, null, "system") } returns
        LogBackupRecordModel(
          id = 1L,
          logTable = LogTableType.API_LOG,
          partitionName = "aqua_api_logs_2026_01",
          partitionMonth = "2026-01",
          coldTableName = "log_archive.aqua_api_logs_2026",
          status = LogBackupStatus.SUCCEEDED,
        )

      val done = service.backupExpiredPartitions(LogTableType.API_LOG, 90, "system")

      done.map { it.partitionName } shouldBe listOf("aqua_api_logs_2026_01")
      verify(exactly = 1) { transactionService.doBackup(expiredPartition, null, "system") }
    }
  })
