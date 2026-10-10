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

import dev.yidafu.aqua.analytics.reconciliation.resolvers.MyTodayCollectionResult
import dev.yidafu.aqua.common.domain.model.DailyCollectionRecordModel
import dev.yidafu.aqua.common.domain.model.DailyReconciliationModel
import dev.yidafu.aqua.common.graphql.generated.DailyCollectionRecordVo
import dev.yidafu.aqua.common.graphql.generated.DailyCollectionStatus
import dev.yidafu.aqua.common.graphql.generated.DailyReconciliationStatus
import dev.yidafu.aqua.common.graphql.generated.DailyReconciliationVo
import dev.yidafu.aqua.common.graphql.generated.MyTodayCollectionVo
import dev.yidafu.aqua.common.util.JsonHelper

/**
 * 配送员今日收款预计算结果 → GraphQL MyTodayCollectionVo(原 MyTodayCollectionResultMapper)
 * status 为领域枚举 → 生成枚举同名枚举 when 穷举互转
 */
fun MyTodayCollectionResult.toGraphQL(): MyTodayCollectionVo =
  MyTodayCollectionVo(
    orderCount = orderCount,
    orderAmountCents = orderAmountCents,
    cashAmountCents = cashAmountCents,
    waterTicketCount = waterTicketCount,
    qrCodeAmountCents = qrCodeAmountCents,
    differenceAmountCents = differenceAmountCents,
    status = status?.let { it.toGraphQL() },
    calculatedCashAmountCents = calculatedCashAmountCents,
    calculatedOrderAmountCents = calculatedOrderAmountCents,
    calculatedQrCodeAmountCents = calculatedQrCodeAmountCents,
    calculatedWaterTicketCount = calculatedWaterTicketCount,
  )

/**
 * Mapper from JPA entity DailyCollectionRecordModel to GraphQL type DailyCollectionRecordVo
 * 处理类型转换（LocalDate -> String, Long? -> Long, 枚举跨包名）
 */
fun DailyCollectionRecordModel.toGraphQL(): DailyCollectionRecordVo =
  DailyCollectionRecordVo(
    // 处理 id: Long? -> Long
    id = id ?: 0L,
    deliveryWorkerId = deliveryWorkerId,
    // 处理 collectionDate: LocalDate -> String
    collectionDate = collectionDate.toString(),
    cashAmountCents = cashAmountCents,
    waterTicketCount = waterTicketCount,
    qrCodeAmountCents = qrCodeAmountCents,
    orderCount = orderCount,
    orderAmountCents = orderAmountCents,
    differenceAmountCents = differenceAmountCents,
    calculatedCashAmountCents = calculatedCashAmountCents,
    calculatedQrCodeAmountCents = calculatedQrCodeAmountCents,
    calculatedWaterTicketCount = calculatedWaterTicketCount,
    calculatedOrderAmountCents = calculatedOrderAmountCents,
    // 处理枚举跨包名:领域枚举 → GraphQL 生成枚举(同名常量)
    status = status.toGraphQL(),
    confirmedAt = confirmedAt,
    confirmedBy = confirmedBy,
    notes = notes,
    rejectedAt = rejectedAt,
    rejectedBy = rejectedBy,
    rejectReason = rejectReason,
    createdAt = createdAt,
    updatedAt = updatedAt,
  )

/**
 * Mapper from JPA entity DailyReconciliationModel to GraphQL type DailyReconciliationVo
 * 处理类型转换（LocalDate -> String, Long? -> Long, 状态枚举跨包名, reportData Map -> ObjectNode）
 */
fun DailyReconciliationModel.toGraphQL(): DailyReconciliationVo =
  DailyReconciliationVo(
    // 处理 id: Long? -> Long
    id = id ?: 0L,
    reconciliationDate = reconciliationDate.toString(),
    totalCashAmountCents = totalCashAmountCents,
    totalWaterTicketCount = totalWaterTicketCount,
    totalQrCodeAmountCents = totalQrCodeAmountCents,
    totalOrderCount = totalOrderCount,
    totalOrderAmountCents = totalOrderAmountCents,
    totalCollectionAmountCents = totalCollectionAmountCents,
    discrepancyAmountCents = discrepancyAmountCents,
    // 处理枚举跨包名:领域枚举 → GraphQL 生成枚举(同名常量)
    status = status.toGraphQL(),
    // 处理 reportData: Map? -> ObjectNode?
    reportData = reportData?.let { JsonHelper().createObjectNode(it) },
    reviewedAt = reviewedAt,
    reviewedBy = reviewedBy,
    reviewNotes = reviewNotes,
    createdAt = createdAt,
    updatedAt = updatedAt,
  )

private fun dev.yidafu.aqua.common.domain.model.enums.DailyCollectionStatus.toGraphQL(): DailyCollectionStatus =
  when (this) {
    dev.yidafu.aqua.common.domain.model.enums.DailyCollectionStatus.DRAFT -> DailyCollectionStatus.DRAFT
    dev.yidafu.aqua.common.domain.model.enums.DailyCollectionStatus.SUBMITTED -> DailyCollectionStatus.SUBMITTED
    dev.yidafu.aqua.common.domain.model.enums.DailyCollectionStatus.CONFIRMED -> DailyCollectionStatus.CONFIRMED
    dev.yidafu.aqua.common.domain.model.enums.DailyCollectionStatus.DISPUTED -> DailyCollectionStatus.DISPUTED
  }

private fun dev.yidafu.aqua.common.domain.model.enums.DailyReconciliationStatus.toGraphQL(): DailyReconciliationStatus =
  when (this) {
    dev.yidafu.aqua.common.domain.model.enums.DailyReconciliationStatus.PENDING -> DailyReconciliationStatus.PENDING
    dev.yidafu.aqua.common.domain.model.enums.DailyReconciliationStatus.MATCHED -> DailyReconciliationStatus.MATCHED
    dev.yidafu.aqua.common.domain.model.enums.DailyReconciliationStatus.DISCREPANCY -> DailyReconciliationStatus.DISCREPANCY
    dev.yidafu.aqua.common.domain.model.enums.DailyReconciliationStatus.REVIEWED -> DailyReconciliationStatus.REVIEWED
  }
