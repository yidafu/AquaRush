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
import dev.yidafu.aqua.analytics.statistics.domain.repository.DailyMarketingFactRepository
import dev.yidafu.aqua.analytics.statistics.domain.repository.DailyMarketingUsageFactRepository
import dev.yidafu.aqua.analytics.statistics.domain.repository.DailyOrderFactRepository
import dev.yidafu.aqua.analytics.statistics.domain.repository.DailyProductFactRepository
import dev.yidafu.aqua.analytics.statistics.domain.repository.DailyRefundFactRepository
import dev.yidafu.aqua.analytics.statistics.domain.repository.DailyRevenueFactRepository
import dev.yidafu.aqua.analytics.statistics.domain.repository.DailyUserFactRepository
import dev.yidafu.aqua.analytics.statistics.domain.repository.StatisticsSnapshotAggregationRepository
import dev.yidafu.aqua.common.domain.model.DailyMarketingFactModel
import dev.yidafu.aqua.common.domain.model.DailyMarketingUsageFactModel
import dev.yidafu.aqua.common.domain.model.DailyOrderFactModel
import dev.yidafu.aqua.common.domain.model.DailyProductFactModel
import dev.yidafu.aqua.common.domain.model.DailyRefundFactModel
import dev.yidafu.aqua.common.domain.model.DailyRevenueFactModel
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
 * (F-STA-007 扩展:营销日事实/营销使用日事实与既有事实表同一事务写入;
 * 营收事实表扩展:rebuildDate 七表同写 + 新表一次性回补)
 */
class StatisticsSnapshotServiceImplTest :
  FunSpec({
    var orderFactRepository: DailyOrderFactRepository = mockk()
    var productFactRepository: DailyProductFactRepository = mockk()
    var userFactRepository: DailyUserFactRepository = mockk()
    var refundFactRepository: DailyRefundFactRepository = mockk()
    var revenueFactRepository: DailyRevenueFactRepository = mockk()
    var marketingFactRepository: DailyMarketingFactRepository = mockk()
    var marketingUsageFactRepository: DailyMarketingUsageFactRepository = mockk()
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
        revenueFactRepository,
        marketingFactRepository,
        marketingUsageFactRepository,
        aggregationRepository,
        txManager,
        config,
      ).also { service = it }
    }

    /**
     * 营销事实聚合与落库 stub:
     * relaxed mock 无法为泛型 save(S) 生成返回值(relaxed 回退 Any → 代理层强转 ClassCastException),
     * 必须显式 stub save/saveAll 与聚合方法;saveAll 泛型返回 List 可 relaxed,这里统一显式 stub 保持流程干净。
     */
    fun stubMarketingAggregates(date: LocalDate) {
      every { aggregationRepository.aggregateMarketingFacts(any()) } returns
        DailyMarketingFactModel(statDate = date, marketingOrderCount = 3L, couponBenefitCents = 600L)
      every { aggregationRepository.aggregateMarketingUsageFacts(any()) } returns
        listOf(
          DailyMarketingUsageFactModel(
            statDate = date,
            kind = "COUPON_TEMPLATE",
            refId = 1L,
            orderCount = 2L,
            benefitCents = 600L,
            usageCount = 2L,
          ),
        )
      every { marketingFactRepository.save(any()) } returns DailyMarketingFactModel()
      every { marketingUsageFactRepository.saveAll(any<List<DailyMarketingUsageFactModel>>()) } returns emptyList()
    }

    beforeTest {
      orderFactRepository = mockk(relaxed = true)
      productFactRepository = mockk(relaxed = true)
      userFactRepository = mockk(relaxed = true)
      refundFactRepository = mockk(relaxed = true)
      revenueFactRepository = mockk(relaxed = true)
      marketingFactRepository = mockk(relaxed = true)
      marketingUsageFactRepository = mockk(relaxed = true)
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
      stubMarketingAggregates(LocalDate.of(2025, 1, 1))

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
      stubMarketingAggregates(LocalDate.now().minusDays(1))

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
      stubMarketingAggregates(LocalDate.now().minusDays(2))

      service.executeIncrementalSnapshot()

      val yesterday = LocalDate.now().minusDays(1)
      val dayBefore = LocalDate.now().minusDays(2)
      verify(exactly = 1) { orderFactRepository.deleteByStatDateIn(listOf(dayBefore)) }
      verify(exactly = 1) { orderFactRepository.deleteByStatDateIn(listOf(yesterday)) }
    }

    test("rebuildDate is delete-then-insert idempotent across all seven fact tables") {
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
      every { aggregationRepository.aggregateRevenueFacts(any()) } returns
        listOf(DailyRevenueFactModel(statDate = LocalDate.now().minusDays(1), orderCount = 1L, amountCents = 100L))
      stubMarketingAggregates(LocalDate.now().minusDays(1))

      service.executeIncrementalSnapshot()

      val yesterday = LocalDate.now().minusDays(1)
      val dayBefore = LocalDate.now().minusDays(2)
      // 七表全部 delete-then-insert(同一事务;增量窗口 [前天, 昨天] 两日期)
      verify(exactly = 1) { orderFactRepository.deleteByStatDateIn(listOf(dayBefore)) }
      verify(exactly = 1) { orderFactRepository.deleteByStatDateIn(listOf(yesterday)) }
      verify(exactly = 1) { productFactRepository.deleteByStatDateIn(listOf(dayBefore)) }
      verify(exactly = 1) { productFactRepository.deleteByStatDateIn(listOf(yesterday)) }
      verify(exactly = 1) { userFactRepository.deleteByStatDateIn(listOf(dayBefore)) }
      verify(exactly = 1) { userFactRepository.deleteByStatDateIn(listOf(yesterday)) }
      verify(exactly = 1) { refundFactRepository.deleteByStatDateIn(listOf(dayBefore)) }
      verify(exactly = 1) { refundFactRepository.deleteByStatDateIn(listOf(yesterday)) }
      verify(exactly = 1) { revenueFactRepository.deleteByStatDateIn(listOf(dayBefore)) }
      verify(exactly = 1) { revenueFactRepository.deleteByStatDateIn(listOf(yesterday)) }
      verify(exactly = 1) { marketingFactRepository.deleteByStatDateIn(listOf(dayBefore)) }
      verify(exactly = 1) { marketingFactRepository.deleteByStatDateIn(listOf(yesterday)) }
      verify(exactly = 1) { marketingUsageFactRepository.deleteByStatDateIn(listOf(dayBefore)) }
      verify(exactly = 1) { marketingUsageFactRepository.deleteByStatDateIn(listOf(yesterday)) }
      verify(exactly = 2) { orderFactRepository.saveAll(any<List<DailyOrderFactModel>>()) }
      verify(exactly = 2) { productFactRepository.saveAll(any<List<DailyProductFactModel>>()) }
      verify(exactly = 2) { userFactRepository.saveAll(any<List<DailyUserFactModel>>()) }
      verify(exactly = 2) { refundFactRepository.saveAll(any<List<DailyRefundFactModel>>()) }
      verify(exactly = 2) { revenueFactRepository.saveAll(any<List<DailyRevenueFactModel>>()) }
      verify(exactly = 2) { marketingFactRepository.save(any()) }
      verify(exactly = 2) { marketingUsageFactRepository.saveAll(any<List<DailyMarketingUsageFactModel>>()) }
    }

    test("new revenue fact table is backfilled once on first run after upgrade") {
      buildService()
      every { orderFactRepository.count() } returns 100L
      // 营收事实表为空(新增表),订单事实已存在 → 触发一次性回补(旧日期不在增量窗口内)
      every { revenueFactRepository.count() } returns 0L
      every { orderFactRepository.findFirstByOrderByStatDateAsc() } returns
        DailyOrderFactModel(statDate = LocalDate.now().minusDays(10))
      every { orderFactRepository.findFirstByOrderByStatDateDesc() } returns
        DailyOrderFactModel(statDate = LocalDate.now().minusDays(1))
      every { aggregationRepository.aggregateRevenueFacts(any()) } returns emptyList()

      service.executeIncrementalSnapshot()

      // 回补 [最早订单事实日, 昨天] 全部日期(10 天) + 增量窗口(前天/昨天)重复重建 → 12 次
      verify(atLeast = 12) { revenueFactRepository.deleteByStatDateIn(any()) }
      verify(atLeast = 12) { aggregationRepository.aggregateRevenueFacts(any()) }
    }

    test("backfillHistory rebuilds each date in range") {
      buildService()
      val start = LocalDate.of(2025, 1, 1)
      val end = LocalDate.of(2025, 1, 3)
      stubMarketingAggregates(start)

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
