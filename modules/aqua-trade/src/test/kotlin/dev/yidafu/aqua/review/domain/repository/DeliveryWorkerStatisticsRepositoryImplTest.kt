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

package dev.yidafu.aqua.review.domain.repository

import dev.yidafu.aqua.common.domain.model.DeliveryWorkerStatisticsModel
import dev.yidafu.aqua.common.domain.table.DeliveryWorkerStatisticsTable
import dev.yidafu.aqua.trade.common.exposed.TradePgTestSupport
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.data.domain.PageRequest
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 送水员评价统计仓储 Exposed 实现测试(Testcontainers 真实 PG,替代原 QueryDSL mockk 测试):
 * save 双分支(lastUpdated 写入口径)/软删可见性/排行榜排序/评分区间/计数/整体均分。
 *
 * 测试表由 SchemaUtils 创建、无 last_updated 触发器——断言以写入值为准。
 */
class DeliveryWorkerStatisticsRepositoryImplTest :
  FunSpec({
    val repo = DeliveryWorkerStatisticsRepositoryImpl(TradePgTestSupport.database)

    beforeTest { TradePgTestSupport.reset() }

    test("save 新增回填雪花 id 并显式写入 lastUpdated,findByDeliveryWorkerId 可查") {
      val saved =
        repo.save(
          DeliveryWorkerStatisticsModel(
            deliveryWorkerId = 5L,
            averageRating = BigDecimal("4.50"),
            totalReviews = 10,
            fiveStarReviews = 6,
            fourStarReviews = 4,
          ),
        )
      saved.id.shouldNotBeNull()

      val found = repo.findByDeliveryWorkerId(5L).shouldNotBeNull()
      found.averageRating shouldBe BigDecimal("4.50")
      found.totalReviews shouldBe 10

      // INSERT 显式写入 lastUpdated(表无列默认)
      val row =
        transaction(TradePgTestSupport.database) {
          DeliveryWorkerStatisticsTable
            .selectAll()
            .where { DeliveryWorkerStatisticsTable.deliveryWorkerId eq 5L }
            .single()
        }
      row[DeliveryWorkerStatisticsTable.lastUpdated] shouldBe saved.lastUpdated
      repo.findByDeliveryWorkerId(6L).shouldBeNull()
    }

    test("save 更新已有实体走 update 分支,不重复插入") {
      val id =
        seedStatistics(
          workerId = 5L,
          averageRating = "3.00",
          totalReviews = 4,
        )
      val loaded = repo.findByDeliveryWorkerId(5L).shouldNotBeNull()
      loaded.updateStatistics(5)
      repo.save(loaded)

      val reloaded = repo.findByDeliveryWorkerId(5L).shouldNotBeNull()
      reloaded.id shouldBe id
      reloaded.totalReviews shouldBe 5
      reloaded.fiveStarReviews shouldBe 1
      repo.countRows() shouldBe 1
    }

    test("软删统计不可见:findByDeliveryWorkerId 过滤") {
      seedStatistics(workerId = 5L, averageRating = "4.00", totalReviews = 3, deleted = true)
      val keep = seedStatistics(workerId = 6L, averageRating = "4.50", totalReviews = 3)

      repo.findByDeliveryWorkerId(5L).shouldBeNull()
      repo.findByDeliveryWorkerId(6L).shouldNotBeNull().id shouldBe keep
    }

    test("排行榜:rating 主排序(totalReviews>=minReviews 过滤),lastUpdated/id 破平") {
      // a 最高分;b 次之;c 评论数不足被过滤;d 软删不进结果
      val a =
        seedStatistics(
          workerId = 1L,
          averageRating = "4.90",
          totalReviews = 20,
          lastUpdated = LocalDateTime.of(2026, 3, 2, 10, 0),
        )
      val b =
        seedStatistics(
          workerId = 2L,
          averageRating = "4.50",
          totalReviews = 15,
          lastUpdated = LocalDateTime.of(2026, 3, 3, 10, 0),
        )
      seedStatistics(workerId = 3L, averageRating = "5.00", totalReviews = 0)
      seedStatistics(workerId = 4L, averageRating = "5.00", totalReviews = 99, deleted = true)

      val page = repo.findDeliveryWorkersRanking("rating", 1, PageRequest.of(0, 10))
      page.totalElements shouldBe 2L
      page.content.map { it.id } shouldContainExactly listOf(a, b)

      // 分页不重不漏
      val p0 = repo.findDeliveryWorkersRanking("rating", 1, PageRequest.of(0, 1))
      p0.content.map { it.id } shouldContainExactly listOf(a)
      val p1 = repo.findDeliveryWorkersRanking("rating", 1, PageRequest.of(1, 1))
      p1.content.map { it.id } shouldContainExactly listOf(b)
    }

    test("排行榜:reviews 主排序按 totalReviews 降序") {
      val more = seedStatistics(workerId = 1L, averageRating = "4.00", totalReviews = 30)
      val less = seedStatistics(workerId = 2L, averageRating = "5.00", totalReviews = 10)

      val page = repo.findDeliveryWorkersRanking("reviews", 1, PageRequest.of(0, 10))
      page.content.map { it.id } shouldContainExactly listOf(more, less)
    }

    test("评分区间:闭区间过滤 + maxReviews 上界 + 软删过滤") {
      val hit = seedStatistics(workerId = 1L, averageRating = "4.00", totalReviews = 10)
      seedStatistics(workerId = 2L, averageRating = "3.00", totalReviews = 10) // 低于 [4.0,4.5] 下界
      seedStatistics(workerId = 3L, averageRating = "5.00", totalReviews = 10) // 高于上界
      seedStatistics(workerId = 4L, averageRating = "4.50", totalReviews = 0) // 低于 minReviews
      seedStatistics(workerId = 5L, averageRating = "4.20", totalReviews = 50) // 高于 maxReviews
      seedStatistics(workerId = 6L, averageRating = "4.30", totalReviews = 10, deleted = true)

      // [4.0,4.5] 闭区间:minReviews=1 过滤 totalReviews=0,软删不进结果
      repo
        .findDeliveryWorkersByRatingRange(4.0, 4.5, 1, null, "rating", PageRequest.of(0, 10))
        .totalElements shouldBe 2L

      // maxReviews=20 上界:totalReviews=50 的行被排除
      repo
        .findDeliveryWorkersByRatingRange(4.0, 4.5, 1, 20, "rating", PageRequest.of(0, 10))
        .content
        .map { it.id } shouldContainExactly listOf(hit)

      // 宽区间 [3.0,5.0]:hit/低分/高分/超上界评论数共 4 行命中
      repo
        .findDeliveryWorkersByRatingRange(3.0, 5.0, 1, null, "reviews", PageRequest.of(0, 10))
        .totalElements shouldBe 4L

      repo
        .findDeliveryWorkersByRatingRange(4.9, 5.0, 1, null, "rating", PageRequest.of(0, 10))
        .totalElements shouldBe 1L
    }

    test("countDeliveryWorkersByMinRating:均分下界+评论数下界,软删过滤") {
      seedStatistics(workerId = 1L, averageRating = "4.00", totalReviews = 10)
      seedStatistics(workerId = 2L, averageRating = "4.50", totalReviews = 5)
      seedStatistics(workerId = 3L, averageRating = "3.00", totalReviews = 10)
      seedStatistics(workerId = 4L, averageRating = "4.50", totalReviews = 10, deleted = true)

      repo.countDeliveryWorkersByMinRating(4.0, 1) shouldBe 2L
      repo.countDeliveryWorkersByMinRating(4.0, 10) shouldBe 1L
      repo.countDeliveryWorkersByMinRating(5.0, 1) shouldBe 0L
    }

    test("findOverallAverageRating:返回均分,无匹配行返回 null,软删不计入") {
      repo.findOverallAverageRating(1).shouldBeNull()

      seedStatistics(workerId = 1L, averageRating = "4.00", totalReviews = 10)
      seedStatistics(workerId = 2L, averageRating = "5.00", totalReviews = 10)
      seedStatistics(workerId = 3L, averageRating = "2.00", totalReviews = 0)
      seedStatistics(workerId = 4L, averageRating = "1.00", totalReviews = 10, deleted = true)

      // 4.00 与 5.00 均值(2 的倍数,无精度歧义)
      repo.findOverallAverageRating(1)!!.toDouble() shouldBe 4.5
      // minReviews=10 过滤掉 totalReviews=0 的行
      repo.findOverallAverageRating(10)!!.toDouble() shouldBe 4.5
      repo.findOverallAverageRating(100).shouldBeNull()
    }
  })

private fun DeliveryWorkerStatisticsRepositoryImpl.countRows(): Long =
  transaction(TradePgTestSupport.database) { DeliveryWorkerStatisticsTable.selectAll().count() }

/** 直接插行的种子构造(含软删开关与 lastUpdated)。 */
private fun seedStatistics(
  workerId: Long,
  averageRating: String,
  totalReviews: Int,
  lastUpdated: LocalDateTime = LocalDateTime.of(2026, 3, 1, 10, 0),
  deleted: Boolean = false,
): Long {
  val id = TradePgTestSupport.id()
  transaction(TradePgTestSupport.database) {
    DeliveryWorkerStatisticsTable.insert {
      it[DeliveryWorkerStatisticsTable.id] = id
      it[DeliveryWorkerStatisticsTable.deliveryWorkerId] = workerId
      it[DeliveryWorkerStatisticsTable.averageRating] = BigDecimal(averageRating)
      it[DeliveryWorkerStatisticsTable.totalReviews] = totalReviews
      it[DeliveryWorkerStatisticsTable.oneStarReviews] = 0
      it[DeliveryWorkerStatisticsTable.twoStarReviews] = 0
      it[DeliveryWorkerStatisticsTable.threeStarReviews] = 0
      it[DeliveryWorkerStatisticsTable.fourStarReviews] = 0
      it[DeliveryWorkerStatisticsTable.fiveStarReviews] = 0
      it[DeliveryWorkerStatisticsTable.lastUpdated] = lastUpdated
      it[DeliveryWorkerStatisticsTable.createdAt] = LocalDateTime.of(2026, 1, 1, 10, 0)
      it[DeliveryWorkerStatisticsTable.updatedAt] = LocalDateTime.of(2026, 1, 1, 10, 0)
      if (deleted) it[DeliveryWorkerStatisticsTable.isDeleted] = true
    }
  }
  return id
}
