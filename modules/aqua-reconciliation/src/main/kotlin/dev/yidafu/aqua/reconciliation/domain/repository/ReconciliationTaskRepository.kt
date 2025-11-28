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

package dev.yidafu.aqua.reconciliation.domain.repository

import dev.yidafu.aqua.reconciliation.domain.model.ReconciliationTask
import dev.yidafu.aqua.reconciliation.domain.model.enums.TaskStatus
import dev.yidafu.aqua.reconciliation.domain.model.enums.TaskType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * 对账任务仓库接口
 */
@Repository
interface ReconciliationTaskRepository : JpaRepository<ReconciliationTask, Long> {

    /**
     * 根据任务ID查找任务
     */
    fun findByTaskId(taskId: String): ReconciliationTask?

    /**
     * 根据任务状态查找任务
     */
    fun findByStatus(status: TaskStatus): List<ReconciliationTask>

    /**
     * 根据任务类型和状态查找任务
     */
    fun findByTaskTypeAndStatus(taskType: TaskType, status: TaskStatus): List<ReconciliationTask>

    /**
     * 查找正在运行的任务
     */
    fun findByStatusIn(statuses: List<TaskStatus>): List<ReconciliationTask>

    /**
     * 根据任务日期范围查找任务
     */
    @Query("SELECT t FROM ReconciliationTask t WHERE t.taskDate BETWEEN :startDate AND :endDate ORDER BY t.taskDate DESC")
    fun findByTaskDateBetween(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<ReconciliationTask>

    /**
     * 根据任务类型和日期范围查找任务
     */
    @Query("SELECT t FROM ReconciliationTask t WHERE t.taskType = :taskType AND t.taskDate BETWEEN :startDate AND :endDate ORDER BY t.taskDate DESC")
    fun findByTaskTypeAndTaskDateBetween(
        @Param("taskType") taskType: TaskType,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<ReconciliationTask>

    /**
     * 统计任务数量
     */
    @Query("SELECT COUNT(t) FROM ReconciliationTask t WHERE t.taskType = :taskType AND t.status = :status")
    fun countByTaskTypeAndStatus(
        @Param("taskType") taskType: TaskType,
        @Param("status") status: TaskStatus
    ): Long

    /**
     * 查找最新的任务
     */
    fun findFirstByOrderByTaskDateDesc(): ReconciliationTask?

    /**
     * 检查是否有正在运行的任务
     */
    fun existsByStatus(status: TaskStatus): Boolean
}
