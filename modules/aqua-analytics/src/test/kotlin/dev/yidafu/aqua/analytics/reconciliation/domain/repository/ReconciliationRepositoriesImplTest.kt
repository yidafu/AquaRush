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

package dev.yidafu.aqua.analytics.reconciliation.domain.repository

import dev.yidafu.aqua.analytics.common.exposed.AnalyticsPgTestSupport.reset
import dev.yidafu.aqua.common.domain.model.ReconciliationDiscrepancyModel
import dev.yidafu.aqua.common.domain.model.ReconciliationReportModel
import dev.yidafu.aqua.common.domain.model.ReconciliationTaskModel
import dev.yidafu.aqua.common.domain.model.enums.DiscrepancyStatus
import dev.yidafu.aqua.common.domain.model.enums.DiscrepancyType
import dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskStatus
import dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskType
import dev.yidafu.aqua.common.domain.model.enums.SourceSystem
import dev.yidafu.aqua.common.domain.table.ReconciliationTasksTable
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.data.domain.PageRequest
import tools.jackson.databind.ObjectMapper
import java.time.LocalDateTime

/**
 * 对账域三仓储 Exposed 实现的真实 PG 测试(JPA→Exposed 迁移 P3):
 * save upsert / 软删查询过滤 / 分页 / 清理类软删。
 */
class ReconciliationRepositoriesImplTest :
  FunSpec({
    val database = dev.yidafu.aqua.analytics.common.exposed.AnalyticsPgTestSupport.database
    val objectMapper = ObjectMapper()
    val taskRepo = ReconciliationTaskRepositoryImpl(database)
    val reportRepo = ReconciliationReportRepositoryImpl(database, objectMapper)
    val discrepancyRepo = ReconciliationDiscrepancyRepositoryImpl(database, objectMapper)
    val now = LocalDateTime.of(2026, 10, 1, 12, 0)

    beforeTest { reset() }

    test("task save-insert→find→save-update(upsert),软删后查询不可见") {
      val entity =
        ReconciliationTaskModel().apply {
          taskId = "wxpay-20261001"
          taskType = ReconciliationTaskType.PAYMENT
          status = ReconciliationTaskStatus.RUNNING
          taskDate = now
          totalRecords = 10
          matchedRecords = 8
          unmatchedRecords = 2
        }
      val saved = taskRepo.save(entity)
      if (saved.id == null) throw AssertionError("expected snowflake id backfilled")

      taskRepo.findByTaskId("wxpay-20261001")!!.status shouldBe ReconciliationTaskStatus.RUNNING

      // update:状态推进(JPA merge 语义)
      saved.status = ReconciliationTaskStatus.SUCCESS
      taskRepo.save(saved)
      taskRepo.findByTaskId("wxpay-20261001")!!.status shouldBe ReconciliationTaskStatus.SUCCESS

      taskRepo.existsByStatus(ReconciliationTaskStatus.SUCCESS) shouldBe true
      taskRepo.findByStatusIn(listOf(ReconciliationTaskStatus.SUCCESS)) shouldHaveSize 1
      taskRepo.countByTaskTypeAndStatus(ReconciliationTaskType.PAYMENT, ReconciliationTaskStatus.SUCCESS) shouldBe 1L
      taskRepo.findFirstByOrderByTaskDateDesc().shouldNotBeNullTask()

      // 分页
      val page = taskRepo.findTasksPaged(ReconciliationTaskType.PAYMENT, null, null, null, PageRequest.of(0, 10))
      page.totalElements shouldBe 1L

      // 软删物理不可见(无 deleteById 入口,由清理任务 softDelete 演示)
      transaction(database) {
        ReconciliationTasksTable.softDelete { ReconciliationTasksTable.taskId eq "wxpay-20261001" }
      }
      taskRepo.findByTaskId("wxpay-20261001").shouldBeNullTask()
    }

    test("report save/findByGeneratedAtBetween/deleteReportsBefore 软删") {
      val entity =
        ReconciliationReportModel().apply {
          taskId = "wxpay-20261001"
          reportType = "SUMMARY"
          reportData = mapOf("total" to 10, "matched" to 8)
          generatedAt = now
        }
      reportRepo.save(entity)

      reportRepo.findByTaskId("wxpay-20261001") shouldHaveSize 1
      reportRepo.findByTaskIdAndReportType("wxpay-20261001", "SUMMARY")!!.reportData shouldBe mapOf("total" to 10, "matched" to 8)
      reportRepo.countByTaskIdAndReportType("wxpay-20261001", "SUMMARY") shouldBe 1L

      // 删除边界之后的行保留、之前的软删
      reportRepo.deleteReportsBefore(now.plusMinutes(1)) shouldBe 1
      reportRepo.findByTaskId("wxpay-20261001") shouldHaveSize 0
    }

    test("discrepancy save/findById(Optional)/deleteResolvedBefore") {
      val entity =
        ReconciliationDiscrepancyModel.createMissingRecord(
          taskId = "wxpay-20261001",
          sourceSystem = SourceSystem.WECHAT,
          recordId = "r1",
          recordDetails = mapOf("amount" to 100),
        )
      discrepancyRepo.save(entity)

      val found = discrepancyRepo.findById(entity.id!!).orElseThrow()
      found.discrepancyType shouldBe DiscrepancyType.MISSING
      found.sourceSystem shouldBe SourceSystem.WECHAT
      found.recordDetails shouldBe mapOf("amount" to 100)

      discrepancyRepo.countUnresolvedByTaskId("wxpay-20261001") shouldBe 1L
      discrepancyRepo.findByStatus("UNRESOLVED") shouldHaveSize 1
      discrepancyRepo
        .findDiscrepanciesPaged("wxpay-20261001", null, DiscrepancyStatus.UNRESOLVED, PageRequest.of(0, 10))
        .totalElements shouldBe 1L
      discrepancyRepo
        .findDiscrepanciesPaged("wxpay-20261001", DiscrepancyType.MISSING, DiscrepancyStatus.UNRESOLVED, PageRequest.of(0, 10))
        .totalElements shouldBe 1L
      discrepancyRepo
        .findDiscrepanciesPaged("wxpay-20261001", DiscrepancyType.MISMATCH, DiscrepancyStatus.UNRESOLVED, PageRequest.of(0, 10))
        .totalElements shouldBe 0L

      found.status = DiscrepancyStatus.RESOLVED
      found.resolvedAt = now
      discrepancyRepo.save(found)
      discrepancyRepo.deleteResolvedBefore(now.plusMinutes(1)) shouldBe 1
      discrepancyRepo.findByTaskId("wxpay-20261001") shouldHaveSize 0
    }
  })

private fun ReconciliationTaskModel?.shouldNotBeNullTask(): ReconciliationTaskModel = this ?: throw AssertionError("expected non-null")

private fun ReconciliationTaskModel?.shouldBeNullTask() {
  this.shouldBeNull()
}
