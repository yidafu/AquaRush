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

package dev.yidafu.aqua.delivery.domain.repository

import dev.yidafu.aqua.common.domain.model.DeliveryTrajectoryModel
import dev.yidafu.aqua.common.domain.table.DeliveryTrajectoriesTable
import dev.yidafu.aqua.trade.common.exposed.TradePgTestSupport
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.LocalDateTime

/**
 * 配送轨迹仓储 Exposed 实现测试(Testcontainers 真实 PG):
 * save 双分支/saveAll 单事务/排序与时间范围口径/实时位置。
 */
class DeliveryTrajectoryRepositoryImplTest :
  FunSpec({
    val repo = DeliveryTrajectoryRepositoryImpl(TradePgTestSupport.database)

    beforeTest { TradePgTestSupport.reset() }

    test("save 新增回填雪花 id,同数据可被查询") {
      val saved =
        repo.save(
          DeliveryTrajectoryModel(
            deliveryWorkerId = 101L,
            orderId = 2001L,
            longitude = 116.4,
            latitude = 39.9,
            accuracy = 5.0,
            reportedAt = LocalDateTime.of(2026, 3, 1, 8, 0, 0),
          ),
        )
      saved.id.shouldNotBeNull()
      repo.findByDeliveryWorkerIdAndOrderIdOrderByReportedAtAsc(101L, 2001L) shouldHaveSize 1
    }

    test("save 更新已有实体走 update 分支") {
      val id =
        seedTrajectory(
          workerId = 101L,
          orderId = 2001L,
          longitude = 116.4,
          latitude = 39.9,
          reportedAt = LocalDateTime.of(2026, 3, 1, 8, 0, 0),
        )
      val loaded =
        DeliveryTrajectoryModel(
          id = id,
          deliveryWorkerId = 101L,
          orderId = 2001L,
          longitude = 116.5,
          latitude = 39.8,
          accuracy = 3.0,
          reportedAt = LocalDateTime.of(2026, 3, 1, 8, 0, 10),
        )
      repo.save(loaded)

      val latest = repo.findFirstByDeliveryWorkerIdOrderByReportedAtDesc(101L).shouldNotBeNull()
      latest.longitude shouldBe 116.5
      latest.accuracy shouldBe 3.0
      // 不重复插入
      repo.findByDeliveryWorkerIdOrderByReportedAtAsc(101L) shouldHaveSize 1
    }

    test("saveAll 单事务批量新增,全部回填 id;空列表 no-op") {
      val points =
        (1..3).map { i ->
          DeliveryTrajectoryModel(
            deliveryWorkerId = 101L,
            orderId = 2001L,
            longitude = 116.0 + i,
            latitude = 39.0 + i,
            reportedAt = LocalDateTime.of(2026, 3, 1, 8, 0, i),
          )
        }
      val saved = repo.saveAll(points)
      saved shouldHaveSize 3
      saved.forEach { it.id.shouldNotBeNull() }
      repo.findByDeliveryWorkerIdOrderByReportedAtAsc(101L) shouldHaveSize 3

      // 再次 saveAll 走 update 分支(不重复插入)
      repo.saveAll(saved) shouldHaveSize 3
      repo.findByDeliveryWorkerIdOrderByReportedAtAsc(101L) shouldHaveSize 3

      repo.saveAll(emptyList()) shouldHaveSize 0
    }

    test("按订单查询:reportedAt 升序 + 同秒 id 破平") {
      val t = LocalDateTime.of(2026, 3, 1, 8, 0, 0)
      val a = seedTrajectory(workerId = 101L, orderId = 2001L, reportedAt = t, longitude = 1.0)
      val b = seedTrajectory(workerId = 101L, orderId = 2001L, reportedAt = t, longitude = 2.0)
      val c = seedTrajectory(workerId = 101L, orderId = 2001L, reportedAt = t.plusSeconds(1), longitude = 3.0)
      seedTrajectory(workerId = 999L, orderId = 3001L, reportedAt = t, longitude = 4.0)

      repo
        .findByDeliveryWorkerIdAndOrderIdOrderByReportedAtAsc(101L, 2001L)
        .map { it.id } shouldContainExactly listOf(a, b, c)
      repo.findByDeliveryWorkerIdAndOrderIdOrderByReportedAtAsc(101L, 999L) shouldHaveSize 0
    }

    test("时间范围查询:[start, end] 闭区间口径,reportedAt 升序") {
      val base = LocalDateTime.of(2026, 3, 1, 8, 0, 0)
      val p0 = seedTrajectory(workerId = 101L, orderId = 2001L, reportedAt = base)
      val p1 = seedTrajectory(workerId = 101L, orderId = 2001L, reportedAt = base.plusSeconds(30))
      val p2 = seedTrajectory(workerId = 101L, orderId = 2001L, reportedAt = base.plusSeconds(60))
      seedTrajectory(workerId = 101L, orderId = 2001L, reportedAt = base.plusSeconds(90))

      // 闭区间 [base, base+60]:start/end 均命中,界外不命中
      val rows = repo.findByDeliveryWorkerIdAndReportedAtBetweenOrderByReportedAtAsc(101L, base, base.plusSeconds(60))
      rows.map { it.id } shouldContainExactly listOf(p0, p1, p2)

      val single = repo.findByDeliveryWorkerIdAndReportedAtBetweenOrderByReportedAtAsc(101L, base, base)
      single.map { it.id } shouldContainExactly listOf(p0)
    }

    test("全量轨迹查询升序;findFirst 取最新,无数据返回 null") {
      val base = LocalDateTime.of(2026, 3, 1, 8, 0, 0)
      val p0 = seedTrajectory(workerId = 101L, orderId = 2001L, reportedAt = base)
      val p1 = seedTrajectory(workerId = 101L, orderId = 2001L, reportedAt = base.plusSeconds(5))
      seedTrajectory(workerId = 102L, orderId = 2002L, reportedAt = base.plusSeconds(9))

      repo
        .findByDeliveryWorkerIdOrderByReportedAtAsc(101L)
        .map { it.id } shouldContainExactly listOf(p0, p1)
      repo.findFirstByDeliveryWorkerIdOrderByReportedAtDesc(101L).shouldNotBeNull().id shouldBe p1
      repo.findFirstByDeliveryWorkerIdOrderByReportedAtDesc(888L).shouldBeNull()
    }
  })

/** 直接插行的种子构造(轨迹表无软删列)。 */
private fun seedTrajectory(
  workerId: Long,
  orderId: Long,
  reportedAt: LocalDateTime,
  longitude: Double = 116.4,
  latitude: Double = 39.9,
): Long {
  val id = TradePgTestSupport.id()
  transaction(TradePgTestSupport.database) {
    DeliveryTrajectoriesTable.insert {
      it[DeliveryTrajectoriesTable.id] = id
      it[DeliveryTrajectoriesTable.deliveryWorkerId] = workerId
      it[DeliveryTrajectoriesTable.orderId] = orderId
      it[DeliveryTrajectoriesTable.longitude] = longitude
      it[DeliveryTrajectoriesTable.latitude] = latitude
      it[DeliveryTrajectoriesTable.reportedAt] = reportedAt
      it[DeliveryTrajectoriesTable.createdAt] = reportedAt
    }
  }
  return id
}
