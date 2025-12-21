/*
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

package dev.yidafu.aqua.api.service

import dev.yidafu.aqua.common.domain.model.ReconciliationDiscrepancyModel
import dev.yidafu.aqua.common.domain.model.ReconciliationTaskModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDate
import java.util.concurrent.CompletableFuture

/**
 * 对账服务接口
 */
interface ReconciliationService {
  /**
   * 创建支付对账任务
   */
  fun createPaymentReconciliationTask(date: LocalDate): ReconciliationTaskModel

  /**
   * 创建退款对账任务
   */
  fun createRefundReconciliationTask(date: LocalDate): ReconciliationTaskModel

  /**
   * 创建结算对账任务
   */
  fun createSettlementReconciliationTask(date: LocalDate): ReconciliationTaskModel

  /**
   * 执行支付对账任务
   */
  fun executePaymentReconciliation(task: ReconciliationTaskModel): CompletableFuture<ReconciliationTaskModel>

  /**
   * 执行退款对账任务
   */
  fun executeRefundReconciliation(task: ReconciliationTaskModel): CompletableFuture<ReconciliationTaskModel>

  /**
   * 执行结算对账任务
   */
  fun executeSettlementReconciliation(task: ReconciliationTaskModel): CompletableFuture<ReconciliationTaskModel>

  /**
   * 查询对账任务
   */
  fun getReconciliationTasks(pageable: Pageable): Page<ReconciliationTaskModel>

  /**
   * 根据日期查询对账任务
   */
  fun getReconciliationTasksByDateRange(
    startDate: LocalDate,
    endDate: LocalDate,
  ): List<ReconciliationTaskModel>

  /**
   * 获取对账任务详情
   */
  fun getReconciliationTask(taskId: String): ReconciliationTaskModel?

  /**
   * 检查是否有正在运行的任务
   */
  fun hasRunningTask(): Boolean

  /**
   * 获取对账差异列表
   */
  fun getDiscrepancies(taskId: String): List<ReconciliationDiscrepancyModel>

  /**
   * 获取未解决的差异
   */
  fun getUnresolvedDiscrepancies(): List<ReconciliationDiscrepancyModel>

  /**
   * 解决对账差异
   */
  fun resolveDiscrepancy(
    discrepancyId: Long,
    resolutionNotes: String,
    resolvedBy: String,
  ): Boolean
}
