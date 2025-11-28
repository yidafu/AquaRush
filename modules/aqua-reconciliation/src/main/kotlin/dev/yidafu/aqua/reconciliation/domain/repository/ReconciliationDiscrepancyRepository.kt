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

import dev.yidafu.aqua.reconciliation.domain.model.ReconciliationDiscrepancy
import dev.yidafu.aqua.reconciliation.domain.model.enums.DiscrepancyType
import dev.yidafu.aqua.reconciliation.domain.model.enums.SourceSystem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * 对账差异仓库接口
 */
@Repository
interface ReconciliationDiscrepancyRepository : JpaRepository<ReconciliationDiscrepancy, Long> {

    /**
     * 根据任务ID查找差异
     */
    fun findByTaskId(taskId: String): List<ReconciliationDiscrepancy>

    /**
     * 根据任务ID和状态查找差异
     */
    fun findByTaskIdAndStatus(taskId: String, status: String): List<ReconciliationDiscrepancy>

    /**
     * 根据差异类型查找差异
     */
    fun findByDiscrepancyType(discrepancyType: DiscrepancyType): List<ReconciliationDiscrepancy>

    /**
     * 根据源系统查找差异
     */
    fun findBySourceSystem(sourceSystem: SourceSystem): List<ReconciliationDiscrepancy>

    /**
     * 根据任务ID和差异类型查找差异
     */
    fun findByTaskIdAndDiscrepancyType(
        taskId: String,
        discrepancyType: DiscrepancyType
    ): List<ReconciliationDiscrepancy>

    /**
     * 统计未解决的差异数量
     */
    @Query("SELECT COUNT(d) FROM ReconciliationDiscrepancy d WHERE d.taskId = :taskId AND d.status = 'UNRESOLVED'")
    fun countUnresolvedByTaskId(@Param("taskId") taskId: String): Long

    /**
     * 统计各种差异类型的数量
     */
    @Query("SELECT d.discrepancyType, COUNT(d) FROM ReconciliationDiscrepancy d WHERE d.taskId = :taskId GROUP BY d.discrepancyType")
    fun countByDiscrepancyTypeGroup(@Param("taskId") taskId: String): List<Array<Any>>

    /**
     * 根据创建时间范围查找差异
     */
    @Query("SELECT d FROM ReconciliationDiscrepancy d WHERE d.createdAt BETWEEN :startDate AND :endDate ORDER BY d.createdAt DESC")
    fun findByCreatedAtBetween(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<ReconciliationDiscrepancy>

    /**
     * 查找所有未解决的差异
     */
    fun findByStatus(status: String = "UNRESOLVED"): List<ReconciliationDiscrepancy>

    /**
     * 删除已解决的旧差异
     */
    @Query("DELETE FROM ReconciliationDiscrepancy d WHERE d.status = 'RESOLVED' AND d.resolvedAt < :beforeDate")
    fun deleteResolvedBefore(@Param("beforeDate") beforeDate: LocalDateTime): Int
}
