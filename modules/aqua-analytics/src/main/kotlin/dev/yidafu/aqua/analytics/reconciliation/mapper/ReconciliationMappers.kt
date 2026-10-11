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

package dev.yidafu.aqua.analytics.reconciliation.mapper

import dev.yidafu.aqua.common.domain.model.ReconciliationDiscrepancyModel
import dev.yidafu.aqua.common.domain.model.ReconciliationReportModel
import dev.yidafu.aqua.common.domain.model.ReconciliationTaskModel
import dev.yidafu.aqua.common.graphql.generated.DiscrepancyStatus
import dev.yidafu.aqua.common.graphql.generated.DiscrepancyType
import dev.yidafu.aqua.common.graphql.generated.ReconciliationDiscrepancyVo
import dev.yidafu.aqua.common.graphql.generated.ReconciliationReportVo
import dev.yidafu.aqua.common.graphql.generated.ReconciliationTaskStatus
import dev.yidafu.aqua.common.graphql.generated.ReconciliationTaskType
import dev.yidafu.aqua.common.graphql.generated.ReconciliationTaskVo
import dev.yidafu.aqua.common.graphql.generated.ReconciliationTriggerType
import dev.yidafu.aqua.common.graphql.generated.SourceSystem
import dev.yidafu.aqua.common.util.JsonHelper
import java.time.LocalDateTime

/**
 * 微信资金对账实体 → GraphQL 类型映射(原 ReconciliationTaskModelMapper):
 * 处理 id Long? -> Long、枚举跨包名(域枚举 → 生成枚举)、taskDate LocalDateTime? -> LocalDateTime。
 */
fun ReconciliationTaskModel.toGraphQL(): ReconciliationTaskVo =
  ReconciliationTaskVo(
    // 处理 id: Long? -> Long
    id = id ?: 0L,
    taskId = taskId,
    // 处理枚举跨包名:领域枚举 → GraphQL 生成枚举(同名常量)
    taskType = taskType.toGraphQL(),
    status = status.toGraphQL(),
    // 处理 taskDate: LocalDateTime? -> LocalDateTime
    taskDate = taskDate ?: LocalDateTime.now(),
    startTime = startTime,
    endTime = endTime,
    totalRecords = totalRecords,
    matchedRecords = matchedRecords,
    unmatchedRecords = unmatchedRecords,
    errorMessage = errorMessage,
    triggerType = triggerType.toGraphQL(),
    executedBy = executedBy,
    wechatTotalCount = wechatTotalCount,
    wechatTotalAmountCents = wechatTotalAmountCents,
    localTotalCount = localTotalCount,
    localTotalAmountCents = localTotalAmountCents,
    createdAt = createdAt,
    updatedAt = updatedAt,
  )

/**
 * 对账差异实体 → GraphQL 类型映射(原 ReconciliationDiscrepancyModelMapper)
 */
fun ReconciliationDiscrepancyModel.toGraphQL(): ReconciliationDiscrepancyVo =
  ReconciliationDiscrepancyVo(
    // 处理 id: Long? -> Long
    id = id ?: 0L,
    taskId = taskId,
    // 处理枚举跨包名:领域枚举 → GraphQL 生成枚举(同名常量)
    discrepancyType = discrepancyType.toGraphQL(),
    sourceSystem = sourceSystem.toGraphQL(),
    recordId = recordId,
    // 处理 recordDetails: Map? -> ObjectNode?
    recordDetails = recordDetails?.let { JsonHelper().createObjectNode(it) },
    status = status.toGraphQL(),
    resolutionNotes = resolutionNotes,
    resolvedBy = resolvedBy,
    resolvedAt = resolvedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
  )

/**
 * 对账报表实体 → GraphQL 类型映射(原 ReconciliationReportModelMapper)
 */
fun ReconciliationReportModel.toGraphQL(): ReconciliationReportVo =
  ReconciliationReportVo(
    // 处理 id: Long? -> Long
    id = id ?: 0L,
    taskId = taskId,
    reportType = reportType,
    // 处理 reportData: Map -> ObjectNode
    reportData = JsonHelper().createObjectNode(reportData),
    filePath = filePath,
    fileName = fileName,
    fileSize = fileSize,
    generatedAt = generatedAt,
  )

private fun dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskType.toGraphQL(): ReconciliationTaskType =
  when (this) {
    dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskType.PAYMENT -> ReconciliationTaskType.PAYMENT
    dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskType.REFUND -> ReconciliationTaskType.REFUND
    dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskType.SETTLEMENT -> ReconciliationTaskType.SETTLEMENT
  }

private fun dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskStatus.toGraphQL(): ReconciliationTaskStatus =
  when (this) {
    dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskStatus.PENDING -> ReconciliationTaskStatus.PENDING
    dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskStatus.RUNNING -> ReconciliationTaskStatus.RUNNING
    dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskStatus.SUCCESS -> ReconciliationTaskStatus.SUCCESS
    dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskStatus.FAILED -> ReconciliationTaskStatus.FAILED
    dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskStatus.SKIPPED -> ReconciliationTaskStatus.SKIPPED
  }

private fun dev.yidafu.aqua.common.domain.model.enums.DiscrepancyType.toGraphQL(): DiscrepancyType =
  when (this) {
    dev.yidafu.aqua.common.domain.model.enums.DiscrepancyType.MISSING -> DiscrepancyType.MISSING
    dev.yidafu.aqua.common.domain.model.enums.DiscrepancyType.MISMATCH -> DiscrepancyType.MISMATCH
    dev.yidafu.aqua.common.domain.model.enums.DiscrepancyType.EXTRA -> DiscrepancyType.EXTRA
  }

private fun dev.yidafu.aqua.common.domain.model.enums.SourceSystem.toGraphQL(): SourceSystem =
  when (this) {
    dev.yidafu.aqua.common.domain.model.enums.SourceSystem.INTERNAL -> SourceSystem.INTERNAL
    dev.yidafu.aqua.common.domain.model.enums.SourceSystem.WECHAT -> SourceSystem.WECHAT
  }

private fun dev.yidafu.aqua.common.domain.model.enums.DiscrepancyStatus.toGraphQL(): DiscrepancyStatus =
  when (this) {
    dev.yidafu.aqua.common.domain.model.enums.DiscrepancyStatus.UNRESOLVED -> DiscrepancyStatus.UNRESOLVED
    dev.yidafu.aqua.common.domain.model.enums.DiscrepancyStatus.RESOLVED -> DiscrepancyStatus.RESOLVED
    dev.yidafu.aqua.common.domain.model.enums.DiscrepancyStatus.IGNORED -> DiscrepancyStatus.IGNORED
  }

private fun dev.yidafu.aqua.common.domain.model.enums.ReconciliationTriggerType.toGraphQL(): ReconciliationTriggerType =
  when (this) {
    dev.yidafu.aqua.common.domain.model.enums.ReconciliationTriggerType.SCHEDULED -> ReconciliationTriggerType.SCHEDULED
    dev.yidafu.aqua.common.domain.model.enums.ReconciliationTriggerType.MANUAL -> ReconciliationTriggerType.MANUAL
  }
