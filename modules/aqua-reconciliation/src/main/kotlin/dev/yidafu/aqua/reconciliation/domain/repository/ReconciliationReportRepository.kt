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

import dev.yidafu.aqua.reconciliation.domain.model.ReconciliationReport
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * 对账报表仓库接口
 */
@Repository
interface ReconciliationReportRepository : JpaRepository<ReconciliationReport, Long> {

    /**
     * 根据任务ID查找报表
     */
    fun findByTaskId(taskId: String): List<ReconciliationReport>

    /**
     * 根据任务ID和报表类型查找报表
     */
    fun findByTaskIdAndReportType(taskId: String, reportType: String): ReconciliationReport?

    /**
     * 根据报表类型查找报表
     */
    fun findByReportType(reportType: String): List<ReconciliationReport>

    /**
     * 根据生成时间范围查找报表
     */
    @Query("SELECT r FROM ReconciliationReport r WHERE r.generatedAt BETWEEN :startDate AND :endDate ORDER BY r.generatedAt DESC")
    fun findByGeneratedAtBetween(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<ReconciliationReport>

    /**
     * 查找最新的报表
     */
    fun findFirstByOrderByGeneratedAtDesc(): ReconciliationReport?

    /**
     * 删除旧报表
     */
    @Query("DELETE FROM ReconciliationReport r WHERE r.generatedAt < :beforeDate")
    fun deleteReportsBefore(@Param("beforeDate") beforeDate: LocalDateTime): Int

    /**
     * 统计报表数量
     */
    @Query("SELECT COUNT(r) FROM ReconciliationReport r WHERE r.taskId = :taskId AND r.reportType = :reportType")
    fun countByTaskIdAndReportType(
        @Param("taskId") taskId: String,
        @Param("reportType") reportType: String
    ): Long
}
