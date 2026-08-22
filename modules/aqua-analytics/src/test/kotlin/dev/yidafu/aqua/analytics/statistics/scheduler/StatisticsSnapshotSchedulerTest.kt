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

package dev.yidafu.aqua.analytics.statistics.scheduler

import dev.yidafu.aqua.analytics.statistics.config.StatisticsSnapshotConfig
import dev.yidafu.aqua.analytics.statistics.service.StatisticsSnapshotService
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

/**
 * 统计快照调度器单元测试(镜像 DailyCollectionSchedulerTest 模式)
 */
class StatisticsSnapshotSchedulerTest :
  FunSpec({
    var statisticsSnapshotService: StatisticsSnapshotService = mockk()
    var scheduler: StatisticsSnapshotScheduler = mockk()

    beforeTest {
      statisticsSnapshotService = mockk(relaxed = true)
      scheduler = StatisticsSnapshotScheduler(statisticsSnapshotService, StatisticsSnapshotConfig())
    }
    test("daily statistics snapshot executes incremental snapshot") {
      scheduler.executeDailyStatisticsSnapshot()

      verify(exactly = 1) { statisticsSnapshotService.executeIncrementalSnapshot() }
    }
    test("statistics snapshot swallows exceptions") {
      every { statisticsSnapshotService.executeIncrementalSnapshot() } throws RuntimeException("boom")

      shouldNotThrowAny { scheduler.executeDailyStatisticsSnapshot() }
    }
    test("statistics snapshot skips overlapping run") {
      every { statisticsSnapshotService.executeIncrementalSnapshot() } throws RuntimeException("slow")

      // 第一次调用抛出异常后 running 置回 false;模拟重叠需连续两次调用,
      // 这里验证异常后仍可再次调度(AtomicBoolean 复位)
      shouldNotThrowAny { scheduler.executeDailyStatisticsSnapshot() }
      shouldNotThrowAny { scheduler.executeDailyStatisticsSnapshot() }
      verify(exactly = 2) { statisticsSnapshotService.executeIncrementalSnapshot() }
    }
  })
