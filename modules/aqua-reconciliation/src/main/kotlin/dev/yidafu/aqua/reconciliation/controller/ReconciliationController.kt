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

package dev.yidafu.aqua.reconciliation.controller

import dev.yidafu.aqua.api.common.ApiResponse
import dev.yidafu.aqua.reconciliation.domain.model.ReconciliationTask
import dev.yidafu.aqua.reconciliation.domain.model.enums.TaskStatus
import dev.yidafu.aqua.reconciliation.domain.model.enums.TaskType
import dev.yidafu.aqua.reconciliation.domain.repository.ReconciliationTaskRepository
import dev.yidafu.aqua.reconciliation.service.ReconciliationService
import dev.yidafu.aqua.reconciliation.domain.model.ReconciliationDiscrepancy
import dev.yidafu.aqua.reconciliation.domain.repository.ReconciliationDiscrepancyRepository
import dev.yidafu.aqua.reconciliation.service.ReconciliationService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.security.Principal

/**
 * 对账控制器
 */
@RestController
@RequestMapping("/api/reconciliation")
@PreAuthorize("hasRole('ADMIN')")
class ReconciliationController(
    private val reconciliationService: ReconciliationService,
    private val reconciliationTaskRepository: ReconciliationTaskRepository,
    private val reconciliationDiscrepancyRepository: ReconciliationDiscrepancyRepository
) {

    /**
     * 创建支付对账任务
     */
    @PostMapping("/payment-tasks")
    fun createPaymentReconciliationTask(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
    ): ApiResponse<ReconciliationTask> {
        return try {
            val task = reconciliationService.createPaymentReconciliationTask(date)
            ApiResponse.success(task)
        } catch (e: Exception) {
            ApiResponse.error("创建支付对账任务失败: ${e.message}")
        }
    }

    /**
     * 创建退款对账任务
     */
    @PostMapping("/refund-tasks")
    fun createRefundReconciliationTask(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
    ): ApiResponse<ReconciliationTask> {
        return try {
            val task = reconciliationService.createRefundReconciliationTask(date)
            ApiResponse.success(task)
        } catch (e: Exception) {
            ApiResponse.error("创建退款对账任务失败: ${e.message}")
        }
    }

    /**
     * 创建结算对账任务
     */
    @PostMapping("/settlement-tasks")
    fun createSettlementReconciliationTask(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
    ): ApiResponse<ReconciliationTask> {
        return try {
            val task = reconciliationService.createSettlementReconciliationTask(date)
            ApiResponse.success(task)
        } catch (e: Exception) {
            ApiResponse.error("创建结算对账任务失败: ${e.message}")
        }
    }

    /**
     * 获取对账任务列表
     */
    @GetMapping("/tasks")
    fun getReconciliationTasks(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate?
    ): ApiResponse<List<ReconciliationTask>> {
        return try {
            val tasks = if (startDate != null && endDate != null) {
                reconciliationService.getReconciliationTasksByDateRange(startDate!!, endDate!!)
            } else {
                reconciliationService.getReconciliationTasks()
            }
            ApiResponse.success(tasks)
        } catch (e: Exception) {
            ApiResponse.error("获取对账任务列表失败: ${e.message}")
        }
    }

    /**
     * 获取对账任务详情
     */
    @GetMapping("/tasks/{taskId}")
    fun getReconciliationTask(@PathVariable taskId: String): ApiResponse<ReconciliationTask> {
        return try {
            val task = reconciliationService.getReconciliationTask(taskId)
                ?: return ApiResponse.error("对账任务不存在")
            ApiResponse.success(task)
        } catch (e: Exception) {
            ApiResponse.error("获取对账任务详情失败: ${e.message}")
        }
    }

    /**
     * 检查是否有正在运行的任务
     */
    @GetMapping("/tasks/running")
    fun getRunningReconciliationTasks(): ApiResponse<Boolean> {
        return try {
            val hasRunning = reconciliationService.hasRunningTask()
            ApiResponse.success(hasRunning)
        } catch (e: Exception) {
            ApiResponse.error("检查运行任务失败: ${e.message}")
        }
    }

    /**
     * 获取对账差异列表
     */
    @GetMapping("/discrepancies")
    fun getReconciliationDiscrepancies(
        @RequestParam(required = false) taskId: String?
    ): ApiResponse<List<ReconciliationDiscrepancy>> {
        return try {
            val discrepancies = if (taskId != null) {
                reconciliationService.getDiscrepancies(taskId)
            } else {
                reconciliationService.getUnresolvedDiscrepancies()
            }
            ApiResponse.success(discrepancies)
        } catch (e: Exception) {
            ApiResponse.error("获取对账差异列表失败: ${e.message}")
        }
    }

    /**
     * 获取未解决的对账差异
     */
    @GetMapping("/discrepancies/unresolved")
    fun getUnresolvedDiscrepancies(): ApiResponse<List<ReconciliationDiscrepancy>> {
        return try {
            val discrepancies = reconciliationService.getUnresolvedDiscrepancies()
            ApiResponse.success(discrepancies)
        } catch (e: Exception) {
            ApiResponse.error("获取未解决差异列表失败: ${e.message}")
        }
    }

    /**
     * 解决对账差异
     */
    @PutMapping("/discrepancies/{id}/resolve")
    fun resolveDiscrepancy(
        @PathVariable id: Long,
        @RequestParam resolutionNotes: String
    ): ApiResponse<Unit> {
        return try {
            val currentUser = SecurityContextHolder.getContext().authentication.name
            val success = reconciliationService.resolveDiscrepancy(id, resolutionNotes, currentUser)
            if (success) {
                ApiResponse.success(Unit)
            } else {
                ApiResponse.error("对账差异不存在或已解决")
            }
        } catch (e: Exception) {
            ApiResponse.error("解决对账差异失败: ${e.message}")
        }
    }

    /**
     * 启动对账任务（手动触发）
     */
    @PostMapping("/tasks/trigger")
    fun triggerReconciliationTask(
        @RequestParam taskType: TaskType,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
    ): ApiResponse<ReconciliationTask> {
        return try {
            val task = when (taskType) {
                TaskType.PAYMENT -> reconciliationService.createPaymentReconciliationTask(date)
                TaskType.REFUND -> reconciliationService.createRefundReconciliationTask(date)
                TaskType.SETTLEMENT -> reconciliationService.createSettlementReconciliationTask(date)
            }
            ApiResponse.success(task)
        } catch (e: Exception) {
            ApiResponse.error("启动对账任务失败: ${e.message}")
        }
    }
}
