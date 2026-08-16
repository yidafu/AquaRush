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

package dev.yidafu.aqua.logging.partition.service.impl

import dev.yidafu.aqua.common.domain.model.LogBackupRecordModel
import dev.yidafu.aqua.logging.partition.domain.model.enums.LogBackupStatus
import dev.yidafu.aqua.logging.partition.domain.model.enums.LogTableType
import dev.yidafu.aqua.logging.partition.repository.LogPartitionRepository
import dev.yidafu.aqua.logging.partition.service.LogBackupService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.YearMonth

class LogPartitionMaintenanceServiceImplTest :
  FunSpec({
    var partitionRepository: LogPartitionRepository = mockk()
    var backupService: LogBackupService = mockk()
    var service: LogPartitionMaintenanceServiceImpl = mockk()

    beforeTest {
      partitionRepository = mockk(relaxed = true)
      backupService = mockk(relaxed = true)
      service = LogPartitionMaintenanceServiceImpl(partitionRepository, backupService)
      every { partitionRepository.isPartitioned(any()) } returns true
    }

    test("ensureFuturePartitions creates partitions for current and next two months for all tables") {
      val now = YearMonth.now()
      service.ensureFuturePartitions()

      LogTableType.entries.forEach { table ->
        verify { partitionRepository.createMonthPartition(table, now.toString()) }
        verify { partitionRepository.createMonthPartition(table, now.plusMonths(1).toString()) }
        verify { partitionRepository.createMonthPartition(table, now.plusMonths(2).toString()) }
      }
    }

    test("ensureFuturePartitions skips non-partitioned tables") {
      every { partitionRepository.isPartitioned(LogTableType.BUSINESS_LOG) } returns false
      service.ensureFuturePartitions()
      verify(exactly = 0) { partitionRepository.createMonthPartition(LogTableType.BUSINESS_LOG, any()) }
    }

    test("backupExpiredPartitions delegates to backup service") {
      val record =
        LogBackupRecordModel(
          id = 1L,
          logTable = LogTableType.API_LOG,
          partitionName = "api_logs_2026_01",
          partitionMonth = "2026-01",
          coldTableName = "log_archive.api_logs_2026",
          status = LogBackupStatus.SUCCEEDED,
        )
      every { backupService.backupExpiredPartitions(LogTableType.API_LOG, 90, "system") } returns listOf(record)

      val done = service.backupExpiredPartitions(LogTableType.API_LOG, 90)

      done shouldBe listOf(record)
      verify { backupService.backupExpiredPartitions(LogTableType.API_LOG, 90, "system") }
    }
  })
