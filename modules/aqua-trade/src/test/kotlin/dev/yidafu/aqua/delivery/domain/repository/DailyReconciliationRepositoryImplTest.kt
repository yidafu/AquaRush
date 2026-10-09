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

import dev.yidafu.aqua.common.domain.model.DailyReconciliationModel
import dev.yidafu.aqua.common.domain.model.enums.DailyReconciliationStatus
import dev.yidafu.aqua.common.domain.table.DailyReconciliationsTable
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
import tools.jackson.databind.ObjectMapper
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 每日对账记录仓储 Exposed 实现测试(Testcontainers 真实 PG):
 * save 双分支/reportData JSON 回环/软删可见性/deleteAll 软删语义/分页 id 破平。
 */
class DailyReconciliationRepositoryImplTest :
  FunSpec({
    val objectMapper = ObjectMapper()
    val repo = DailyReconciliationRepositoryImpl(TradePgTestSupport.database, objectMapper)

    beforeTest { TradePgTestSupport.reset() }

    test("save 新增回填雪花 id,reportData JSON 回环") {
      val saved =
        repo.save(
          DailyReconciliationModel(
            reconciliationDate = LocalDate.of(2026, 3, 1),
            totalCashAmountCents = 1000L,
            totalWaterTicketCount = 2,
            totalQrCodeAmountCents = 500L,
            totalOrderCount = 3,
            totalOrderAmountCents = 1500L,
            totalCollectionAmountCents = 1500L,
            discrepancyAmountCents = 0L,
            status = DailyReconciliationStatus.MATCHED,
            reportData = mapOf("totalWorkers" to 1, "note" to "ok"),
          ),
        )
      saved.id.shouldNotBeNull()

      val loaded = repo.findById(saved.id!!).orElse(null).shouldNotBeNull()
      loaded.totalCashAmountCents shouldBe 1000L
      loaded.status shouldBe DailyReconciliationStatus.MATCHED
      // Jackson 反序列化数字类型不作 Int/Long 假设,统一按 Number 断言
      (loaded.reportData.shouldNotBeNull()["totalWorkers"] as Number).toInt() shouldBe 1
      loaded.reportData!!["note"] shouldBe "ok"
    }

    test("save 新增 reportData 为 null 时读回 null;更新分支改写 reportData") {
      val saved =
        repo.save(
          DailyReconciliationModel(
            reconciliationDate = LocalDate.of(2026, 3, 2),
            status = DailyReconciliationStatus.PENDING,
            reportData = null,
          ),
        )
      repo
        .findById(saved.id!!)
        .orElse(null)
        .shouldNotBeNull()
        .reportData
        .shouldBeNull()

      val loaded = repo.findById(saved.id!!).orElse(null).shouldNotBeNull()
      loaded.reportData = mapOf("flag" to true)
      loaded.status = DailyReconciliationStatus.REVIEWED
      repo.save(loaded)

      val reloaded = repo.findById(saved.id!!).orElse(null).shouldNotBeNull()
      reloaded.status shouldBe DailyReconciliationStatus.REVIEWED
      reloaded.reportData.shouldNotBeNull()["flag"] shouldBe true
      // 更新分支不重复插入
      repo.findAll() shouldHaveSize 1
    }

    test("软删对账不可可见:findById/findAll/findByReconciliationDate 均过滤") {
      val keep = seedReconciliation(date = LocalDate.of(2026, 3, 1))
      val removed = seedReconciliation(date = LocalDate.of(2026, 3, 2), deleted = true)

      repo.findById(keep).isPresent shouldBe true
      repo.findById(removed).isPresent shouldBe false
      repo.findByReconciliationDate(LocalDate.of(2026, 3, 2)).shouldBeNull()
      repo.findByReconciliationDate(LocalDate.of(2026, 3, 1)).shouldNotBeNull()
      repo.findAll() shouldHaveSize 1
    }

    test("findAll 按 id 升序;deleteAll 置 is_deleted 而非物理删除") {
      val a = seedReconciliation(date = LocalDate.of(2026, 3, 1))
      val b = seedReconciliation(date = LocalDate.of(2026, 3, 2))

      repo.findAll().map { it.id } shouldContainExactly listOf(a, b)

      repo.deleteAll()
      repo.findAll() shouldHaveSize 0
      val physical =
        transaction(TradePgTestSupport.database) {
          DailyReconciliationsTable.selectAll().count()
        }
      physical shouldBe 2L
      transaction(TradePgTestSupport.database) {
        DailyReconciliationsTable
          .selectAll()
          .where { DailyReconciliationsTable.isDeleted eq true }
          .count()
      } shouldBe 2L
    }

    test("findAll 分页:id 升序破平,软删不计入 total") {
      val a = seedReconciliation(date = LocalDate.of(2026, 3, 1))
      val b = seedReconciliation(date = LocalDate.of(2026, 3, 2))
      val c = seedReconciliation(date = LocalDate.of(2026, 3, 3))
      seedReconciliation(date = LocalDate.of(2026, 3, 4), deleted = true)

      val page0 = repo.findAll(PageRequest.of(0, 2))
      page0.totalElements shouldBe 3L
      page0.content.map { it.id } shouldContainExactly listOf(a, b)
      val page1 = repo.findAll(PageRequest.of(1, 2))
      page1.content.map { it.id } shouldContainExactly listOf(c)
    }

    test("日期区间查询:闭区间口径,软删过滤") {
      val d1 = LocalDate.of(2026, 3, 1)
      val a = seedReconciliation(date = d1)
      val b = seedReconciliation(date = d1.plusDays(1))
      seedReconciliation(date = d1.plusDays(2), deleted = true)
      val d = seedReconciliation(date = d1.plusDays(3))

      repo
        .findByReconciliationDateBetween(d1, d1.plusDays(3))
        .map { it.id } shouldContainExactly listOf(a, b, d)
      repo.findByReconciliationDateBetween(d1, d1) shouldHaveSize 1
      repo.findByReconciliationDateBetween(d1.plusDays(10), d1.plusDays(11)) shouldHaveSize 0
    }

    test("save 可更新已软删行(存在判定不过滤软删),更新后仍不可见") {
      val id = seedReconciliation(date = LocalDate.of(2026, 3, 5), deleted = true)
      val entity =
        DailyReconciliationModel(
          id = id,
          reconciliationDate = LocalDate.of(2026, 3, 5),
          status = DailyReconciliationStatus.PENDING,
        )
      repo.save(entity)
      repo.findById(id).isPresent shouldBe false
      repo.findAll() shouldHaveSize 0
    }
  })

/** 直接插行的种子构造(含软删开关与 reportData 原文)。 */
private fun seedReconciliation(
  date: LocalDate,
  deleted: Boolean = false,
  reportData: String? = null,
): Long {
  val id = TradePgTestSupport.id()
  transaction(TradePgTestSupport.database) {
    DailyReconciliationsTable.insert {
      it[DailyReconciliationsTable.id] = id
      it[DailyReconciliationsTable.reconciliationDate] = date
      it[DailyReconciliationsTable.totalCashAmountCents] = 0L
      it[DailyReconciliationsTable.totalWaterTicketCount] = 0
      it[DailyReconciliationsTable.totalQrCodeAmountCents] = 0L
      it[DailyReconciliationsTable.totalOrderCount] = 0
      it[DailyReconciliationsTable.totalOrderAmountCents] = 0L
      it[DailyReconciliationsTable.totalCollectionAmountCents] = 0L
      it[DailyReconciliationsTable.discrepancyAmountCents] = 0L
      it[DailyReconciliationsTable.status] = DailyReconciliationStatus.PENDING.name
      it[DailyReconciliationsTable.reportData] = reportData
      it[DailyReconciliationsTable.createdAt] = LocalDateTime.of(2026, 1, 1, 10, 0)
      it[DailyReconciliationsTable.updatedAt] = LocalDateTime.of(2026, 1, 1, 10, 0)
      if (deleted) it[DailyReconciliationsTable.isDeleted] = true
    }
  }
  return id
}
