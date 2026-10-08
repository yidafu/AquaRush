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
import dev.yidafu.aqua.logging.partition.domain.model.enums.LogTableType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class LogPartitionRulesTest :
  FunSpec({
    test("monthOfPartition parses yyyy_MM suffix") {
      LogPartitionRules.monthOfPartition("api_logs_2026_01") shouldBe "2026-01"
      LogPartitionRules.monthOfPartition("business_logs_2026_12") shouldBe "2026-12"
      LogPartitionRules.monthOfPartition("api_logs_default").shouldBeNull()
      LogPartitionRules.monthOfPartition("api_logs_2026_13").shouldBeNull()
      LogPartitionRules.monthOfPartition("api_logs_2026_00").shouldBeNull()
    }
    test("yearOfPartition parses year") {
      LogPartitionRules.yearOfPartition("api_logs_2026_01") shouldBe 2026
      LogPartitionRules.yearOfPartition("user_action_logs_2025_11") shouldBe 2025
      LogPartitionRules.yearOfPartition("api_logs_default").shouldBeNull()
    }
    test("isExpired compares partition upper bound with now - retentionDays") {
      val now = LocalDateTime.of(2026, 8, 16, 12, 0)
      val expiredPartition =
        partition(toBound = now.minusDays(91))
      LogPartitionRules.isExpired(expiredPartition, retentionDays = 90, now = now) shouldBe true

      val notExpired = partition(toBound = now.minusDays(89))
      LogPartitionRules.isExpired(notExpired, retentionDays = 90, now = now) shouldBe false

      val exactlyAtCutoff = partition(toBound = now.minusDays(90))
      LogPartitionRules.isExpired(exactlyAtCutoff, retentionDays = 90, now = now) shouldBe true

      val defaultPartition = partition(toBound = null, isDefault = true)
      LogPartitionRules.isExpired(defaultPartition, retentionDays = 90, now = now) shouldBe false
    }
    test("isBackupable requires past month, not default and expired") {
      val now = LocalDateTime.of(2026, 8, 16, 12, 0)
      val expired = partition(toBound = now.minusDays(91))
      LogPartitionRules.isBackupable(expired, retentionDays = 90, now = now) shouldBe true

      val currentMonth = partition(toBound = now.minusDays(91), isCurrentMonth = true)
      LogPartitionRules.isBackupable(currentMonth, retentionDays = 90, now = now) shouldBe false

      val defaultPartition = partition(toBound = null, isDefault = true)
      LogPartitionRules.isBackupable(defaultPartition, retentionDays = 90, now = now) shouldBe false

      val notExpired = partition(toBound = now.minusDays(89))
      LogPartitionRules.isBackupable(notExpired, retentionDays = 90, now = now) shouldBe false
    }
  })

private fun partition(
  partitionName: String = "api_logs_2026_01",
  toBound: LocalDateTime?,
  isDefault: Boolean = false,
  isCurrentMonth: Boolean = false,
): LogPartitionInfo =
  LogPartitionInfo(
    logTable = LogTableType.API_LOG,
    partitionName = partitionName,
    partitionMonth = if (isDefault) null else "2026-01",
    rowCount = 100,
    sizeBytes = 1024,
    isDefault = isDefault,
    isCurrentMonth = isCurrentMonth,
    fromBound = toBound?.minusMonths(1),
    toBound = toBound,
  )
