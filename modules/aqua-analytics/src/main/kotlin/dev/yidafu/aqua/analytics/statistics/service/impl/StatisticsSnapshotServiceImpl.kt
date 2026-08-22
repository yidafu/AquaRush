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

import dev.yidafu.aqua.analytics.statistics.StatisticsTime
import dev.yidafu.aqua.analytics.statistics.config.StatisticsSnapshotConfig
import dev.yidafu.aqua.analytics.statistics.domain.repository.DailyMarketingFactRepository
import dev.yidafu.aqua.analytics.statistics.domain.repository.DailyMarketingUsageFactRepository
import dev.yidafu.aqua.analytics.statistics.domain.repository.DailyOrderFactRepository
import dev.yidafu.aqua.analytics.statistics.domain.repository.DailyProductFactRepository
import dev.yidafu.aqua.analytics.statistics.domain.repository.DailyRefundFactRepository
import dev.yidafu.aqua.analytics.statistics.domain.repository.DailyUserFactRepository
import dev.yidafu.aqua.analytics.statistics.domain.repository.StatisticsSnapshotAggregationRepository
import dev.yidafu.aqua.analytics.statistics.service.StatisticsSnapshotService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate

/**
 * 统计快照服务实现
 * 夜间增量窗口 [昨天 - incrementalDays + 1, 昨天](默认昨天/前天),前天重算以吸收「昨天才完成」的状态迁移;
 * 缺口自愈:(上次数仓日期, 昨天] 缺失日期回补(有界,不超过 maxGapBackfillDays)。
 * 首启(事实表全空)从最早源数据日期一次性回填至昨天,之后永久增量。
 */
@Service
class StatisticsSnapshotServiceImpl(
  private val orderFactRepository: DailyOrderFactRepository,
  private val productFactRepository: DailyProductFactRepository,
  private val userFactRepository: DailyUserFactRepository,
  private val refundFactRepository: DailyRefundFactRepository,
  private val marketingFactRepository: DailyMarketingFactRepository,
  private val marketingUsageFactRepository: DailyMarketingUsageFactRepository,
  private val aggregationRepository: StatisticsSnapshotAggregationRepository,
  transactionManager: PlatformTransactionManager,
  private val config: StatisticsSnapshotConfig,
) : StatisticsSnapshotService {
  private val logger = LoggerFactory.getLogger(StatisticsSnapshotServiceImpl::class.java)

  private val transactionTemplate: TransactionTemplate = TransactionTemplate(transactionManager)

  override fun executeIncrementalSnapshot() {
    val today = LocalDate.now(StatisticsTime.STATS_ZONE)
    val yesterday = today.minusDays(1)

    if (isFirstRun()) {
      logger.info("统计快照事实表为空,执行首次回填(一次性全量)")
      val earliest = aggregationRepository.findEarliestSourceDate()
      if (earliest == null || earliest.isAfter(yesterday)) {
        logger.info("无源数据,跳过首次回填")
        return
      }
      backfillHistory(earliest, yesterday)
      return
    }

    val incrementalStart = yesterday.minusDays((config.incrementalDays - 1).toLong())
    val lastSnapshotDate = orderFactRepository.findFirstByOrderByStatDateDesc()?.statDate

    // 缺口自愈:上次数仓日期之后到昨天的缺失日期(有界回补,防长时间故障后一次性全量重算)
    val gapStart = lastSnapshotDate?.plusDays(1)?.takeIf { it <= yesterday }
    val windowStart =
      listOfNotNull(incrementalStart, gapStart)
        .minOrNull()
        ?.coerceAtLeast(yesterday.minusDays((config.maxGapBackfillDays - 1).toLong()))
        ?: incrementalStart

    if (windowStart.isAfter(yesterday)) {
      logger.info("统计快照无待处理日期")
      return
    }

    val targetDates = dateRange(windowStart, yesterday)
    logger.info("开始执行统计快照增量聚合: {} (lastSnapshot={})", targetDates, lastSnapshotDate)
    targetDates.forEach { date ->
      try {
        rebuildDate(date)
        logger.info("统计快照完成: {}", date)
      } catch (e: Exception) {
        logger.error("统计快照失败: {}", date, e)
      }
    }
  }

  override fun backfillHistory(
    startDate: LocalDate,
    endDate: LocalDate,
  ) {
    require(!startDate.isAfter(endDate)) { "回填起止日期非法: $startDate > $endDate" }
    dateRange(startDate, endDate).forEach { date ->
      try {
        rebuildDate(date)
      } catch (e: Exception) {
        logger.error("统计快照回填失败: {}", date, e)
      }
    }
  }

  /**
   * 重建单日快照:delete-then-insert 幂等;六张事实表同一事务,保证一致性。
   */
  private fun rebuildDate(date: LocalDate) {
    transactionTemplate.execute {
      orderFactRepository.deleteByStatDateIn(listOf(date))
      productFactRepository.deleteByStatDateIn(listOf(date))
      userFactRepository.deleteByStatDateIn(listOf(date))
      refundFactRepository.deleteByStatDateIn(listOf(date))
      marketingFactRepository.deleteByStatDateIn(listOf(date))
      marketingUsageFactRepository.deleteByStatDateIn(listOf(date))

      orderFactRepository.saveAll(aggregationRepository.aggregateOrderFacts(date))
      productFactRepository.saveAll(aggregationRepository.aggregateProductFacts(date))
      userFactRepository.saveAll(listOf(aggregationRepository.aggregateUserFact(date)))
      refundFactRepository.saveAll(aggregationRepository.aggregateRefundFacts(date))
      marketingFactRepository.save(aggregationRepository.aggregateMarketingFacts(date))
      marketingUsageFactRepository.saveAll(aggregationRepository.aggregateMarketingUsageFacts(date))
    }
  }

  private fun isFirstRun(): Boolean = orderFactRepository.count() == 0L

  private fun dateRange(
    start: LocalDate,
    end: LocalDate,
  ): List<LocalDate> =
    generateSequence(start) { it.plusDays(1) }
      .takeWhile { !it.isAfter(end) }
      .toList()
}
