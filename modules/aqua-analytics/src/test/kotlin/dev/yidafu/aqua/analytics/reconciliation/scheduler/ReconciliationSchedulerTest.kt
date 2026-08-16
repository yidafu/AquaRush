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

package dev.yidafu.aqua.analytics.reconciliation.scheduler

import dev.yidafu.aqua.api.service.reconciliation.ReconciliationService
import dev.yidafu.aqua.common.domain.model.ReconciliationTaskModel
import dev.yidafu.aqua.common.messaging.service.SimplifiedEventPublishService
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

/**
 * 对账调度器单元测试: 每日支付/退款/结算任务创建与跳过、异常兜底
 */
class ReconciliationSchedulerTest :
  FunSpec({
    var reconciliationService: ReconciliationService = mockk()
    var eventPublishService: SimplifiedEventPublishService = mockk()
    var scheduler: ReconciliationScheduler = mockk()

    beforeTest {
      reconciliationService = mockk(relaxed = true)
      eventPublishService = mockk(relaxed = true)
      scheduler = ReconciliationScheduler(reconciliationService, eventPublishService)
    }
    test("daily payment reconciliation creates task and publishes event") {
      val task = ReconciliationTaskModel()
      every { reconciliationService.createPaymentReconciliationTask(any()) } returns task
      every { reconciliationService.hasRunningTask() } returns false

      scheduler.executeDailyPaymentReconciliation()

      verify(exactly = 1) { reconciliationService.createPaymentReconciliationTask(any()) }
      verify { eventPublishService.publishDomainEvent("DAILY_RECONCILIATION_STARTED", task.taskId, any()) }
    }
    test("daily payment reconciliation skips when task already running") {
      val task = ReconciliationTaskModel()
      every { reconciliationService.createPaymentReconciliationTask(any()) } returns task
      every { reconciliationService.hasRunningTask() } returns true

      scheduler.executeDailyPaymentReconciliation()

      verify(exactly = 0) { eventPublishService.publishDomainEvent("DAILY_RECONCILIATION_STARTED", any(), any()) }
    }
    test("daily payment reconciliation swallows exceptions") {
      every { reconciliationService.createPaymentReconciliationTask(any()) } throws RuntimeException("boom")

      shouldNotThrowAny { scheduler.executeDailyPaymentReconciliation() }
    }
    test("daily refund reconciliation creates refund task") {
      val task = ReconciliationTaskModel()
      every { reconciliationService.createRefundReconciliationTask(any()) } returns task
      every { reconciliationService.hasRunningTask() } returns false

      scheduler.executeDailyRefundReconciliation()

      verify(exactly = 1) { reconciliationService.createRefundReconciliationTask(any()) }
      verify { eventPublishService.publishDomainEvent("DAILY_RECONCILIATION_STARTED", task.taskId, any()) }
    }
    test("daily refund reconciliation skips when running") {
      val task = ReconciliationTaskModel()
      every { reconciliationService.createRefundReconciliationTask(any()) } returns task
      every { reconciliationService.hasRunningTask() } returns true

      scheduler.executeDailyRefundReconciliation()

      verify(exactly = 0) { eventPublishService.publishDomainEvent("DAILY_RECONCILIATION_STARTED", any(), any()) }
    }
    test("daily settlement reconciliation creates settlement task") {
      val task = ReconciliationTaskModel()
      every { reconciliationService.createSettlementReconciliationTask(any()) } returns task
      every { reconciliationService.hasRunningTask() } returns false

      scheduler.executeDailySettlementReconciliation()

      verify(exactly = 1) { reconciliationService.createSettlementReconciliationTask(any()) }
      verify { eventPublishService.publishDomainEvent("DAILY_RECONCILIATION_STARTED", task.taskId, any()) }
    }
    test("cleanup old data does not throw") {
      shouldNotThrowAny { scheduler.cleanupOldReconciliationData() }
    }
  })
