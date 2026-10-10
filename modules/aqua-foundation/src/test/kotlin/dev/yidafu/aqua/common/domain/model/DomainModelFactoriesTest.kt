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

package dev.yidafu.aqua.common.domain.model

import dev.yidafu.aqua.common.domain.model.enums.DiscrepancyType
import dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskStatus
import dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskType
import dev.yidafu.aqua.common.domain.model.enums.SourceSystem
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class DomainModelFactoriesTest :
  FunSpec({

    test("OrderItemModel create should compute total price") {
      val item =
        OrderItemModel.create(
          orderId = 1L,
          productId = 2L,
          quantity = 3,
          unitPriceCents = 1000L,
          productSnapshot = "{}",
        )
      item.totalPriceCents shouldBe 3000L
      item.quantity shouldBe 3
      item.productSnapshot shouldBe "{}"
    }
    test("OrderItemModel unitPrice and totalPrice should convert to yuan") {
      val item = OrderItemModel.create(1L, 2L, 2, 150L, "{}")
      item.unitPrice shouldBe java.math.BigDecimal("1.50")
      item.totalPrice shouldBe java.math.BigDecimal("3.00")
    }
    test("MessageHistoryModel createSuccess should set sent status") {
      val message = MessageHistoryModel.createSuccess(1L, "ORDER", "tpl-1", "content", "wx-1")
      message.status shouldBe MessageStatus.SENT
      message.wxMessageId shouldBe "wx-1"
      message.sentAt.shouldNotBeNull()
      message.retryCount shouldBe 0
    }
    test("MessageHistoryModel createFailure should set failed status") {
      val message = MessageHistoryModel.createFailure(1L, "ORDER", "tpl-1", "content", "error", retryCount = 2)
      message.status shouldBe MessageStatus.FAILED
      message.errorMessage shouldBe "error"
      message.retryCount shouldBe 2
      message.wxMessageId shouldBe null
    }
    test("MessageHistoryModel createPending should set pending status") {
      val message = MessageHistoryModel.createPending(1L, "ORDER", "tpl-1", "content")
      message.status shouldBe MessageStatus.PENDING
      message.errorMessage shouldBe null
    }
    test("ReconciliationTaskModel createPaymentTask should set fields") {
      val date = LocalDateTime.of(2025, 1, 1, 0, 0)
      val task = ReconciliationTaskModel.createPaymentTask(date)
      task.taskType shouldBe ReconciliationTaskType.PAYMENT
      task.status shouldBe ReconciliationTaskStatus.PENDING
      task.taskDate shouldBe date
      task.taskId.isNotBlank() shouldBe true
    }
    test("ReconciliationTaskModel createRefundTask should set fields") {
      val task = ReconciliationTaskModel.createRefundTask(LocalDateTime.now())
      task.taskType shouldBe ReconciliationTaskType.REFUND
    }
    test("ReconciliationTaskModel createSettlementTask should set fields") {
      val task = ReconciliationTaskModel.createSettlementTask(LocalDateTime.now())
      task.taskType shouldBe ReconciliationTaskType.SETTLEMENT
    }
    test("ReconciliationDiscrepancyModel createMissingRecord should set fields") {
      val record =
        ReconciliationDiscrepancyModel.createMissingRecord(
          taskId = "t1",
          sourceSystem = SourceSystem.INTERNAL,
          recordId = "r1",
          recordDetails = mapOf("a" to 1),
        )
      record.discrepancyType shouldBe DiscrepancyType.MISSING
      record.sourceSystem shouldBe SourceSystem.INTERNAL
      record.recordId shouldBe "r1"
      record.recordDetails shouldBe mapOf("a" to 1)
    }
    test("ReconciliationDiscrepancyModel createMismatchRecord should set type") {
      val record = ReconciliationDiscrepancyModel.createMismatchRecord("t1", SourceSystem.WECHAT, "r1", emptyMap())
      record.discrepancyType shouldBe DiscrepancyType.MISMATCH
    }
    test("ReconciliationDiscrepancyModel createExtraRecord should set type") {
      val record = ReconciliationDiscrepancyModel.createExtraRecord("t1", SourceSystem.INTERNAL, "r1", emptyMap())
      record.discrepancyType shouldBe DiscrepancyType.EXTRA
    }
    test("ReconciliationReportModel createSummaryReport should set type") {
      val report = ReconciliationReportModel.createSummaryReport("t1", mapOf("total" to 1))
      report.reportType shouldBe "SUMMARY"
      report.reportData shouldBe mapOf("total" to 1)
    }
    test("ReconciliationReportModel createDetailReport should set type") {
      val report = ReconciliationReportModel.createDetailReport("t1", emptyMap())
      report.reportType shouldBe "DETAIL"
    }
    test("ReconciliationReportModel createExcelReport should set file info") {
      val report = ReconciliationReportModel.createExcelReport("t1", emptyMap(), "report.xlsx", "/tmp/report.xlsx", 1024L)
      report.reportType shouldBe "EXCEL_EXPORT"
      report.fileName shouldBe "report.xlsx"
      report.filePath shouldBe "/tmp/report.xlsx"
      report.fileSize shouldBe 1024L
    }
    test("DeliveryWorkerStatisticsModel updateStatistics should accumulate ratings") {
      val stats = DeliveryWorkerStatisticsModel(deliveryWorkerId = 1L)
      stats.updateStatistics(5)
      stats.updateStatistics(5)
      stats.updateStatistics(1)
      stats.totalReviews shouldBe 3
      stats.fiveStarReviews shouldBe 2
      stats.oneStarReviews shouldBe 1
      stats.averageRating shouldBe java.math.BigDecimal("3.67")
    }
    test("DeliveryWorkerStatisticsModel updateStatistics for each rating value") {
      val stats = DeliveryWorkerStatisticsModel(deliveryWorkerId = 1L)
      stats.updateStatistics(1)
      stats.updateStatistics(2)
      stats.updateStatistics(3)
      stats.updateStatistics(4)
      stats.updateStatistics(5)
      stats.totalReviews shouldBe 5
      stats.getRatingDistribution() shouldBe mapOf(1 to 1, 2 to 1, 3 to 1, 4 to 1, 5 to 1)
      stats.averageRating shouldBe java.math.BigDecimal("3.00")
    }
    test("DeliveryWorkerStatisticsModel default average rating should be zero") {
      val stats = DeliveryWorkerStatisticsModel()
      stats.averageRating shouldBe java.math.BigDecimal.ZERO
      stats.getRatingDistribution() shouldBe mapOf(1 to 0, 2 to 0, 3 to 0, 4 to 0, 5 to 0)
    }
    test("SystemSettingKeys should expose key constants") {
      SystemSettingKeys.PAYMENT_TIMEOUT_MINUTES shouldBe "payment_timeout_minutes"
      SystemSettingKeys.LOW_STOCK_THRESHOLD shouldBe "low_stock_threshold"
      SystemSettingKeys.DELIVERY_SLOT_MORNING_ENABLED.startsWith("delivery_slot") shouldBe true
    }
  })
