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

import dev.yidafu.aqua.common.domain.model.DailyCollectionRecordModel
import dev.yidafu.aqua.common.domain.model.enums.DailyCollectionStatus
import dev.yidafu.aqua.common.domain.table.DailyCollectionRecordsTable
import dev.yidafu.aqua.trade.common.exposed.TradePgTestSupport
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.data.domain.PageRequest
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 每日收款记录仓储 Exposed 实现测试(Testcontainers 真实 PG):
 * save 双分支/软删可见性/deleteAll 软删语义/分页 id 破平/日期与送水工筛选。
 */
class DailyCollectionRecordRepositoryImplTest :
  FunSpec({
    val repo = DailyCollectionRecordRepositoryImpl(TradePgTestSupport.database)

    beforeTest { TradePgTestSupport.reset() }

    test("save 新增回填雪花 id,findById 可查") {
      val saved =
        repo.save(
          DailyCollectionRecordModel(
            deliveryWorkerId = 101L,
            collectionDate = LocalDate.of(2026, 3, 1),
            cashAmountCents = 1000L,
            waterTicketCount = 2,
            qrCodeAmountCents = 500L,
            status = DailyCollectionStatus.DRAFT,
          ),
        )
      saved.id.shouldNotBeNull()
      repo
        .findById(saved.id!!)
        .orElse(null)
        .shouldNotBeNull()
        .cashAmountCents shouldBe 1000L
    }

    test("save 更新已有实体走 update 分支") {
      val id =
        seedCollection(
          workerId = 101L,
          date = LocalDate.of(2026, 3, 1),
          cashCents = 100L,
          status = DailyCollectionStatus.DRAFT,
        )
      val loaded = repo.findById(id).orElse(null).shouldNotBeNull()
      loaded.cashAmountCents = 999L
      loaded.status = DailyCollectionStatus.SUBMITTED
      repo.save(loaded)

      val reloaded = repo.findById(id).orElse(null).shouldNotBeNull()
      reloaded.cashAmountCents shouldBe 999L
      reloaded.status shouldBe DailyCollectionStatus.SUBMITTED
      // 不重复插入
      repo.findAll(PageRequest.of(0, 100)) shouldHaveSize 1
    }

    test("软删记录不可见:findById/findAll/findByCollectionDate 均过滤") {
      val keep = seedCollection(workerId = 101L, date = LocalDate.of(2026, 3, 1))
      val removed = seedCollection(workerId = 102L, date = LocalDate.of(2026, 3, 1), deleted = true)

      repo.findById(keep).isPresent shouldBe true
      repo.findById(removed).isPresent shouldBe false
      repo.findAll(PageRequest.of(0, 100)) shouldHaveSize 1
      repo.findByCollectionDate(LocalDate.of(2026, 3, 1)) shouldHaveSize 1
    }

    test("findAll 按 id 升序;deleteAll 置 is_deleted 而非物理删除") {
      val a = seedCollection(workerId = 101L, date = LocalDate.of(2026, 3, 1))
      val b = seedCollection(workerId = 102L, date = LocalDate.of(2026, 3, 2))

      repo.findAll(PageRequest.of(0, 100)).content.map { it.id } shouldContainExactly listOf(a, b)

      repo.deleteAll()
      repo.findAll(PageRequest.of(0, 100)) shouldHaveSize 0
      // 物理行保留,仅软删标记
      val physical =
        transaction(TradePgTestSupport.database) {
          DailyCollectionRecordsTable.selectAll().count()
        }
      physical shouldBe 2L
      transaction(TradePgTestSupport.database) {
        DailyCollectionRecordsTable
          .selectAll()
          .where { DailyCollectionRecordsTable.isDeleted eq true }
          .count()
      } shouldBe 2L
    }

    test("findAll 分页:id 升序破平,count 与 content 同口径,软删不计入 total") {
      val a = seedCollection(workerId = 101L, date = LocalDate.of(2026, 3, 1))
      val b = seedCollection(workerId = 101L, date = LocalDate.of(2026, 3, 2))
      val c = seedCollection(workerId = 101L, date = LocalDate.of(2026, 3, 3))
      seedCollection(workerId = 999L, date = LocalDate.of(2026, 3, 4), deleted = true)

      val page0 = repo.findAll(PageRequest.of(0, 2))
      page0.totalElements shouldBe 3L
      page0.content.map { it.id } shouldContainExactly listOf(a, b)
      val page1 = repo.findAll(PageRequest.of(1, 2))
      page1.content.map { it.id } shouldContainExactly listOf(c)
    }

    test("按日期查询:列表升序 + 分页,软删不进结果") {
      val date = LocalDate.of(2026, 3, 1)
      val a = seedCollection(workerId = 101L, date = date, cashCents = 100L)
      seedCollection(workerId = 102L, date = date, cashCents = 200L, deleted = true)
      val c = seedCollection(workerId = 103L, date = date, cashCents = 300L)

      repo.findByCollectionDate(date).map { it.id } shouldContainExactly listOf(a, c)

      val page = repo.findByCollectionDate(date, PageRequest.of(0, 1))
      page.totalElements shouldBe 2L
      page.content.map { it.id } shouldContainExactly listOf(a)

      repo.findByCollectionDate(date.plusDays(1)) shouldHaveSize 0
    }

    test("日期区间查询:闭区间口径,软删过滤") {
      val d1 = LocalDate.of(2026, 3, 1)
      val a = seedCollection(workerId = 101L, date = d1)
      val b = seedCollection(workerId = 101L, date = d1.plusDays(1))
      seedCollection(workerId = 101L, date = d1.plusDays(2), deleted = true)
      val d = seedCollection(workerId = 101L, date = d1.plusDays(3))

      repo
        .findByCollectionDateBetween(d1, d1.plusDays(3))
        .map { it.id } shouldContainExactly listOf(a, b, d)
      repo.findByCollectionDateBetween(d1, d1) shouldHaveSize 1
    }

    test("按送水工分页:过滤该送水工,id 升序破平") {
      val a = seedCollection(workerId = 101L, date = LocalDate.of(2026, 3, 1))
      val b = seedCollection(workerId = 101L, date = LocalDate.of(2026, 3, 2))
      seedCollection(workerId = 102L, date = LocalDate.of(2026, 3, 1))

      val page = repo.findByDeliveryWorkerId(101L, PageRequest.of(0, 10))
      page.totalElements shouldBe 2L
      page.content.map { it.id } shouldContainExactly listOf(a, b)
      repo.findByDeliveryWorkerId(777L, PageRequest.of(0, 10)).totalElements shouldBe 0L
    }

    test("送水工+日期:单条与分页,软删过滤") {
      val date = LocalDate.of(2026, 3, 1)
      val a = seedCollection(workerId = 101L, date = date, cashCents = 100L)
      seedCollection(workerId = 101L, date = date, deleted = true)

      repo.findByDeliveryWorkerIdAndCollectionDate(101L, date).shouldNotBeNull().id shouldBe a
      repo.findByDeliveryWorkerIdAndCollectionDate(102L, date).shouldBeNull()

      val page = repo.findByDeliveryWorkerIdAndCollectionDate(101L, date, PageRequest.of(0, 10))
      page.totalElements shouldBe 1L
      page.content.map { it.id } shouldContainExactly listOf(a)
    }

    test("save 可更新已软删行(存在判定不过滤软删),更新后仍不可见") {
      val id = seedCollection(workerId = 101L, date = LocalDate.of(2026, 3, 1), deleted = true)
      val entity =
        DailyCollectionRecordModel(
          id = id,
          deliveryWorkerId = 101L,
          collectionDate = LocalDate.of(2026, 3, 1),
          status = DailyCollectionStatus.DRAFT,
        )
      repo.save(entity)
      repo.findById(id).isPresent shouldBe false
      repo.findAll(PageRequest.of(0, 100)) shouldHaveSize 0
    }
  })

/** 直接插行的种子构造(含软删开关)。 */
private fun seedCollection(
  workerId: Long,
  date: LocalDate,
  cashCents: Long = 0L,
  status: DailyCollectionStatus = DailyCollectionStatus.DRAFT,
  deleted: Boolean = false,
): Long {
  val id = TradePgTestSupport.id()
  transaction(TradePgTestSupport.database) {
    DailyCollectionRecordsTable.insert {
      it[DailyCollectionRecordsTable.id] = id
      it[DailyCollectionRecordsTable.deliveryWorkerId] = workerId
      it[DailyCollectionRecordsTable.collectionDate] = date
      it[DailyCollectionRecordsTable.cashAmountCents] = cashCents
      it[DailyCollectionRecordsTable.waterTicketCount] = 0
      it[DailyCollectionRecordsTable.qrCodeAmountCents] = 0L
      it[DailyCollectionRecordsTable.status] = status.name
      it[DailyCollectionRecordsTable.createdAt] = LocalDateTime.of(2026, 1, 1, 10, 0)
      it[DailyCollectionRecordsTable.updatedAt] = LocalDateTime.of(2026, 1, 1, 10, 0)
      if (deleted) it[DailyCollectionRecordsTable.isDeleted] = true
    }
  }
  return id
}
