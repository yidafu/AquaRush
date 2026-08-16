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

package dev.yidafu.aqua.analytics.reconciliation.scheduler

import dev.yidafu.aqua.analytics.reconciliation.service.DailyCollectionService
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.FunSpec
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate

/**
 * 每日收银对账调度器单元测试
 */
class DailyCollectionSchedulerTest :
  FunSpec({
    var dailyCollectionService: DailyCollectionService = mockk()
    var scheduler: DailyCollectionScheduler = mockk()

    beforeTest {
      dailyCollectionService = mockk(relaxed = true)
      scheduler = DailyCollectionScheduler(dailyCollectionService)
    }
    test("daily collection reconciliation executes for yesterday") {
      scheduler.executeDailyCollectionReconciliation()

      val yesterday = LocalDate.now().minusDays(1)
      verify(exactly = 1) { dailyCollectionService.executeDailyReconciliation(yesterday) }
    }
    test("daily collection reconciliation swallows exceptions") {
      io.mockk.every { dailyCollectionService.executeDailyReconciliation(any()) } throws RuntimeException("boom")

      shouldNotThrowAny { scheduler.executeDailyCollectionReconciliation() }
    }
  })
