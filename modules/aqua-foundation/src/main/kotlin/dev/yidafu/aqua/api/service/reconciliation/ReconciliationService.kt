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

package dev.yidafu.aqua.api.service.reconciliation

import dev.yidafu.aqua.common.domain.model.ReconciliationDiscrepancyModel
import dev.yidafu.aqua.common.domain.model.ReconciliationReportModel
import dev.yidafu.aqua.common.domain.model.ReconciliationTaskModel
import dev.yidafu.aqua.common.domain.model.enums.DiscrepancyStatus
import dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskStatus
import dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskType
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
   * 对账任务分页查询(后台微信资金对账页):任务类型/状态/对账日期范围均可选
   */
  fun getReconciliationTasksPaged(
    taskType: ReconciliationTaskType?,
    status: ReconciliationTaskStatus?,
    dateFrom: LocalDate?,
    dateTo: LocalDate?,
    pageable: Pageable,
  ): Page<ReconciliationTaskModel>

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
   * 对账差异分页查询(后台微信资金对账页):任务ID/解决状态均可选
   */
  fun getDiscrepanciesPaged(
    taskId: String?,
    status: DiscrepancyStatus?,
    pageable: Pageable,
  ): Page<ReconciliationDiscrepancyModel>

  /**
   * 获取未解决的差异
   */
  fun getUnresolvedDiscrepancies(): List<ReconciliationDiscrepancyModel>

  /**
   * 获取对账报表列表(按任务,可选报表类型过滤)
   */
  fun getReports(
    taskId: String,
    reportType: String?,
  ): List<ReconciliationReportModel>

  /**
   * 解决对账差异
   */
  fun resolveDiscrepancy(
    discrepancyId: Long,
    resolutionNotes: String,
    resolvedBy: String,
  ): Boolean
}
