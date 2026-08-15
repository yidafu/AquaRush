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
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DomainModelFactoriesTest {
  @Test
  fun `OrderItemModel create should compute total price`() {
    val item =
      OrderItemModel.create(
        orderId = 1L,
        productId = 2L,
        quantity = 3,
        unitPriceCents = 1000L,
        productSnapshot = "{}",
      )
    assertEquals(3000L, item.totalPriceCents)
    assertEquals(3, item.quantity)
    assertEquals("{}", item.productSnapshot)
  }

  @Test
  fun `OrderItemModel unitPrice and totalPrice should convert to yuan`() {
    val item = OrderItemModel.create(1L, 2L, 2, 150L, "{}")
    assertEquals(java.math.BigDecimal("1.50"), item.unitPrice)
    assertEquals(java.math.BigDecimal("3.00"), item.totalPrice)
  }

  @Test
  fun `MessageHistoryModel createSuccess should set sent status`() {
    val message = MessageHistoryModel.createSuccess(1L, "ORDER", "tpl-1", "content", "wx-1")
    assertEquals(MessageStatus.SENT, message.status)
    assertEquals("wx-1", message.wxMessageId)
    assertNotNull(message.sentAt)
    assertEquals(0, message.retryCount)
  }

  @Test
  fun `MessageHistoryModel createFailure should set failed status`() {
    val message = MessageHistoryModel.createFailure(1L, "ORDER", "tpl-1", "content", "error", retryCount = 2)
    assertEquals(MessageStatus.FAILED, message.status)
    assertEquals("error", message.errorMessage)
    assertEquals(2, message.retryCount)
    assertEquals(null, message.wxMessageId)
  }

  @Test
  fun `MessageHistoryModel createPending should set pending status`() {
    val message = MessageHistoryModel.createPending(1L, "ORDER", "tpl-1", "content")
    assertEquals(MessageStatus.PENDING, message.status)
    assertEquals(null, message.errorMessage)
  }

  @Test
  fun `ReconciliationTaskModel createPaymentTask should set fields`() {
    val date = LocalDateTime.of(2025, 1, 1, 0, 0)
    val task = ReconciliationTaskModel.createPaymentTask(date)
    assertEquals(ReconciliationTaskType.PAYMENT, task.taskType)
    assertEquals(ReconciliationTaskStatus.PENDING, task.status)
    assertEquals(date, task.taskDate)
    assertTrue(task.taskId.isNotBlank())
  }

  @Test
  fun `ReconciliationTaskModel createRefundTask should set fields`() {
    val task = ReconciliationTaskModel.createRefundTask(LocalDateTime.now())
    assertEquals(ReconciliationTaskType.REFUND, task.taskType)
  }

  @Test
  fun `ReconciliationTaskModel createSettlementTask should set fields`() {
    val task = ReconciliationTaskModel.createSettlementTask(LocalDateTime.now())
    assertEquals(ReconciliationTaskType.SETTLEMENT, task.taskType)
  }

  @Test
  fun `ReconciliationDiscrepancyModel createMissingRecord should set fields`() {
    val record =
      ReconciliationDiscrepancyModel.createMissingRecord(
        taskId = "t1",
        sourceSystem = SourceSystem.INTERNAL,
        recordId = "r1",
        recordDetails = mapOf("a" to 1),
      )
    assertEquals(DiscrepancyType.MISSING, record.discrepancyType)
    assertEquals(SourceSystem.INTERNAL, record.sourceSystem)
    assertEquals("r1", record.recordId)
    assertEquals(mapOf("a" to 1), record.recordDetails)
  }

  @Test
  fun `ReconciliationDiscrepancyModel createMismatchRecord should set type`() {
    val record = ReconciliationDiscrepancyModel.createMismatchRecord("t1", SourceSystem.WECHAT, "r1", emptyMap())
    assertEquals(DiscrepancyType.MISMATCH, record.discrepancyType)
  }

  @Test
  fun `ReconciliationDiscrepancyModel createExtraRecord should set type`() {
    val record = ReconciliationDiscrepancyModel.createExtraRecord("t1", SourceSystem.INTERNAL, "r1", emptyMap())
    assertEquals(DiscrepancyType.EXTRA, record.discrepancyType)
  }

  @Test
  fun `ReconciliationReportModel createSummaryReport should set type`() {
    val report = ReconciliationReportModel.createSummaryReport("t1", mapOf("total" to 1))
    assertEquals("SUMMARY", report.reportType)
    assertEquals(mapOf("total" to 1), report.reportData)
  }

  @Test
  fun `ReconciliationReportModel createDetailReport should set type`() {
    val report = ReconciliationReportModel.createDetailReport("t1", emptyMap())
    assertEquals("DETAIL", report.reportType)
  }

  @Test
  fun `ReconciliationReportModel createExcelReport should set file info`() {
    val report = ReconciliationReportModel.createExcelReport("t1", emptyMap(), "report.xlsx", "/tmp/report.xlsx", 1024L)
    assertEquals("EXCEL_EXPORT", report.reportType)
    assertEquals("report.xlsx", report.fileName)
    assertEquals("/tmp/report.xlsx", report.filePath)
    assertEquals(1024L, report.fileSize)
  }

  @Test
  fun `DeliveryWorkerStatisticsModel updateStatistics should accumulate ratings`() {
    val stats = DeliveryWorkerStatisticsModel(deliveryWorkerId = 1L)
    stats.updateStatistics(5)
    stats.updateStatistics(5)
    stats.updateStatistics(1)
    assertEquals(3, stats.totalReviews)
    assertEquals(2, stats.fiveStarReviews)
    assertEquals(1, stats.oneStarReviews)
    assertEquals(java.math.BigDecimal("3.67"), stats.averageRating)
  }

  @Test
  fun `DeliveryWorkerStatisticsModel updateStatistics for each rating value`() {
    val stats = DeliveryWorkerStatisticsModel(deliveryWorkerId = 1L)
    stats.updateStatistics(1)
    stats.updateStatistics(2)
    stats.updateStatistics(3)
    stats.updateStatistics(4)
    stats.updateStatistics(5)
    assertEquals(5, stats.totalReviews)
    assertEquals(mapOf(1 to 1, 2 to 1, 3 to 1, 4 to 1, 5 to 1), stats.getRatingDistribution())
    assertEquals(java.math.BigDecimal("3.00"), stats.averageRating)
  }

  @Test
  fun `DeliveryWorkerStatisticsModel default average rating should be zero`() {
    val stats = DeliveryWorkerStatisticsModel()
    assertEquals(java.math.BigDecimal.ZERO, stats.averageRating)
    assertEquals(mapOf(1 to 0, 2 to 0, 3 to 0, 4 to 0, 5 to 0), stats.getRatingDistribution())
  }

  @Test
  fun `SystemSettingKeys should expose key constants`() {
    assertEquals("payment_timeout_minutes", SystemSettingKeys.PAYMENT_TIMEOUT_MINUTES)
    assertEquals("low_stock_threshold", SystemSettingKeys.LOW_STOCK_THRESHOLD)
    assertEquals("auto_confirm_receipt_days", SystemSettingKeys.AUTO_CONFIRM_RECEIPT_DAYS)
    assertTrue(SystemSettingKeys.DELIVERY_SLOT_MORNING_ENABLED.startsWith("delivery_slot"))
  }
}
