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

package dev.yidafu.aqua.analytics.statistics.service.impl

import dev.yidafu.aqua.analytics.statistics.config.StatisticsSnapshotConfig
import dev.yidafu.aqua.analytics.statistics.domain.repository.DailyOrderFactRepository
import dev.yidafu.aqua.analytics.statistics.domain.repository.DailyProductFactRepository
import dev.yidafu.aqua.analytics.statistics.domain.repository.DailyRefundFactRepository
import dev.yidafu.aqua.analytics.statistics.domain.repository.DailyUserFactRepository
import dev.yidafu.aqua.analytics.statistics.domain.repository.StatisticsSnapshotAggregationRepository
import dev.yidafu.aqua.common.domain.model.DailyOrderFactModel
import dev.yidafu.aqua.common.domain.model.DailyProductFactModel
import dev.yidafu.aqua.common.domain.model.DailyRefundFactModel
import dev.yidafu.aqua.common.domain.model.DailyUserFactModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.transaction.PlatformTransactionManager
import java.time.LocalDate

/**
 * 统计快照服务单元测试:增量窗口(昨天/前天)、缺口自愈、首次回填、delete-then-insert 幂等
 */
class StatisticsSnapshotServiceImplTest :
  FunSpec({
    var orderFactRepository: DailyOrderFactRepository = mockk()
    var productFactRepository: DailyProductFactRepository = mockk()
    var userFactRepository: DailyUserFactRepository = mockk()
    var refundFactRepository: DailyRefundFactRepository = mockk()
    var aggregationRepository: StatisticsSnapshotAggregationRepository = mockk()
    var service: StatisticsSnapshotServiceImpl = mockk()

    fun buildService(config: StatisticsSnapshotConfig = StatisticsSnapshotConfig()): StatisticsSnapshotServiceImpl {
      // relaxed mock 的 getTransaction 返回假 TransactionStatus,TransactionTemplate 仅透传,不校验
      val txManager = mockk<PlatformTransactionManager>(relaxed = true)
      return StatisticsSnapshotServiceImpl(
        orderFactRepository,
        productFactRepository,
        userFactRepository,
        refundFactRepository,
        aggregationRepository,
        txManager,
        config,
      ).also { service = it }
    }

    beforeTest {
      orderFactRepository = mockk(relaxed = true)
      productFactRepository = mockk(relaxed = true)
      userFactRepository = mockk(relaxed = true)
      refundFactRepository = mockk(relaxed = true)
      aggregationRepository = mockk(relaxed = true)
    }

    test("first run backfills from earliest source date to yesterday once") {
      buildService()
      every { orderFactRepository.count() } returns 0L
      every { aggregationRepository.findEarliestSourceDate() } returns LocalDate.of(2025, 1, 1)
      every { aggregationRepository.aggregateOrderFacts(any()) } returns emptyList()
      every { aggregationRepository.aggregateProductFacts(any()) } returns emptyList()
      every { aggregationRepository.aggregateUserFact(any()) } returns DailyUserFactModel(statDate = LocalDate.of(2025, 1, 1))
      every { aggregationRepository.aggregateRefundFacts(any()) } returns emptyList()

      service.executeIncrementalSnapshot()

      // 从最早源日期到昨天的每一天都重建(2025-01-01 .. 昨天)
      verify(atLeast = 1) { orderFactRepository.deleteByStatDateIn(any()) }
      verify(atLeast = 1) { aggregationRepository.aggregateOrderFacts(any()) }
    }

    test("incremental snapshot aggregates only yesterday and day-before-yesterday") {
      buildService()
      every { orderFactRepository.count() } returns 100L
      every { orderFactRepository.findFirstByOrderByStatDateDesc() } returns
        DailyOrderFactModel(statDate = LocalDate.now().minusDays(1))
      every { aggregationRepository.aggregateOrderFacts(any()) } returns emptyList()
      every { aggregationRepository.aggregateProductFacts(any()) } returns emptyList()
      every { aggregationRepository.aggregateUserFact(any()) } returns DailyUserFactModel(statDate = LocalDate.now())
      every { aggregationRepository.aggregateRefundFacts(any()) } returns emptyList()

      service.executeIncrementalSnapshot()

      val yesterday = LocalDate.now().minusDays(1)
      val dayBefore = LocalDate.now().minusDays(2)
      // 只重建昨天与前天(增量,不重复全量)
      verify(exactly = 1) { orderFactRepository.deleteByStatDateIn(listOf(dayBefore)) }
      verify(exactly = 1) { orderFactRepository.deleteByStatDateIn(listOf(yesterday)) }
      verify(exactly = 2) { aggregationRepository.aggregateOrderFacts(any()) }
    }

    test("gap self-heal backfills missing dates between last snapshot and yesterday") {
      buildService()
      every { orderFactRepository.count() } returns 100L
      // 上次快照停在 3 天前 → 缺口 [today-2, yesterday],与增量窗口并集仍只算最近两天
      every { orderFactRepository.findFirstByOrderByStatDateDesc() } returns
        DailyOrderFactModel(statDate = LocalDate.now().minusDays(3))
      every { aggregationRepository.aggregateOrderFacts(any()) } returns emptyList()
      every { aggregationRepository.aggregateProductFacts(any()) } returns emptyList()
      every { aggregationRepository.aggregateUserFact(any()) } returns DailyUserFactModel(statDate = LocalDate.now())
      every { aggregationRepository.aggregateRefundFacts(any()) } returns emptyList()

      service.executeIncrementalSnapshot()

      val yesterday = LocalDate.now().minusDays(1)
      val dayBefore = LocalDate.now().minusDays(2)
      verify(exactly = 1) { orderFactRepository.deleteByStatDateIn(listOf(dayBefore)) }
      verify(exactly = 1) { orderFactRepository.deleteByStatDateIn(listOf(yesterday)) }
    }

    test("rebuildDate is delete-then-insert idempotent across all four fact tables") {
      buildService()
      every { orderFactRepository.count() } returns 100L
      every { orderFactRepository.findFirstByOrderByStatDateDesc() } returns
        DailyOrderFactModel(statDate = LocalDate.now().minusDays(1))
      every { aggregationRepository.aggregateOrderFacts(any()) } returns
        listOf(DailyOrderFactModel(statDate = LocalDate.now().minusDays(1), status = "COMPLETED", orderCount = 1L, amountCents = 100L))
      every { aggregationRepository.aggregateProductFacts(any()) } returns
        listOf(
          DailyProductFactModel(
            statDate = LocalDate.now().minusDays(1),
            status = "COMPLETED",
            productId = 1L,
            quantity = 2L,
            revenueCents = 100L,
          ),
        )
      every { aggregationRepository.aggregateUserFact(any()) } returns
        DailyUserFactModel(statDate = LocalDate.now().minusDays(1), newUserCount = 1L, loginUserCount = 2L)
      every { aggregationRepository.aggregateRefundFacts(any()) } returns
        listOf(
          DailyRefundFactModel(
            statDate = LocalDate.now().minusDays(1),
            status = "COMPLETED",
            refundType = "ORDER_REFUND",
            refundCount = 1L,
            refundAmountCents = 50L,
          ),
        )

      service.executeIncrementalSnapshot()

      val yesterday = LocalDate.now().minusDays(1)
      val dayBefore = LocalDate.now().minusDays(2)
      // 四表全部 delete-then-insert(同一事务;增量窗口 [前天, 昨天] 两日期)
      verify(exactly = 1) { orderFactRepository.deleteByStatDateIn(listOf(dayBefore)) }
      verify(exactly = 1) { orderFactRepository.deleteByStatDateIn(listOf(yesterday)) }
      verify(exactly = 1) { productFactRepository.deleteByStatDateIn(listOf(dayBefore)) }
      verify(exactly = 1) { productFactRepository.deleteByStatDateIn(listOf(yesterday)) }
      verify(exactly = 1) { userFactRepository.deleteByStatDateIn(listOf(dayBefore)) }
      verify(exactly = 1) { userFactRepository.deleteByStatDateIn(listOf(yesterday)) }
      verify(exactly = 1) { refundFactRepository.deleteByStatDateIn(listOf(dayBefore)) }
      verify(exactly = 1) { refundFactRepository.deleteByStatDateIn(listOf(yesterday)) }
      verify(exactly = 2) { orderFactRepository.saveAll(any<List<DailyOrderFactModel>>()) }
      verify(exactly = 2) { productFactRepository.saveAll(any<List<DailyProductFactModel>>()) }
      verify(exactly = 2) { userFactRepository.saveAll(any<List<DailyUserFactModel>>()) }
      verify(exactly = 2) { refundFactRepository.saveAll(any<List<DailyRefundFactModel>>()) }
    }

    test("backfillHistory rebuilds each date in range") {
      buildService()
      val start = LocalDate.of(2025, 1, 1)
      val end = LocalDate.of(2025, 1, 3)

      service.backfillHistory(start, end)

      verify(exactly = 3) { aggregationRepository.aggregateOrderFacts(any()) }
      verify(exactly = 3) { orderFactRepository.deleteByStatDateIn(any()) }
      verify(exactly = 3) { orderFactRepository.saveAll(any<List<DailyOrderFactModel>>()) }
    }

    test("backfillHistory rejects inverted range") {
      buildService()
      val start = LocalDate.of(2025, 1, 3)
      val end = LocalDate.of(2025, 1, 1)

      var thrown = false
      try {
        service.backfillHistory(start, end)
      } catch (e: IllegalArgumentException) {
        thrown = true
      }
      thrown shouldBe true
    }
  })
