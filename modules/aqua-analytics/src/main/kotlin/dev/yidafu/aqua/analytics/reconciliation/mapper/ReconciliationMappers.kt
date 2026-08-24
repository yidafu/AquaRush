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
import dev.yidafu.aqua.common.graphql.generated.ReconciliationDiscrepancy
import dev.yidafu.aqua.common.graphql.generated.ReconciliationReport
import dev.yidafu.aqua.common.graphql.generated.ReconciliationTask
import dev.yidafu.aqua.common.graphql.generated.ReconciliationTaskStatus
import dev.yidafu.aqua.common.graphql.generated.ReconciliationTaskType
import dev.yidafu.aqua.common.graphql.generated.SourceSystem
import dev.yidafu.aqua.common.utils.JsonHelper
import tech.mappie.api.ObjectMappie
import java.time.LocalDateTime

/**
 * 微信资金对账实体 → GraphQL 类型映射(与 DailyCollectionMappers 同风格):
 * 处理 id Long? -> Long、枚举跨包名(域枚举 → 生成枚举)、recordDetails/reportData Map -> ObjectNode。
 */
object ReconciliationTaskModelMapper : ObjectMappie<ReconciliationTaskModel, ReconciliationTask>() {
  override fun map(from: ReconciliationTaskModel): ReconciliationTask =
    mapping {
      // 处理 id: Long? -> Long
      to::id fromValue (from.id ?: 0L)
      // 处理枚举跨包名:域枚举 → GraphQL 生成枚举(同名常量)
      to::taskType fromValue ReconciliationTaskType.valueOf(from.taskType.name)
      to::status fromValue ReconciliationTaskStatus.valueOf(from.status.name)
      // 处理 taskDate: LocalDateTime? -> LocalDateTime
      to::taskDate fromValue (from.taskDate ?: LocalDateTime.now())
    }
}

/**
 * 对账差异实体 → GraphQL 类型映射
 */
object ReconciliationDiscrepancyModelMapper : ObjectMappie<ReconciliationDiscrepancyModel, ReconciliationDiscrepancy>() {
  override fun map(from: ReconciliationDiscrepancyModel): ReconciliationDiscrepancy {
    val recordDetailsNode = from.recordDetails?.let { JsonHelper().createObjectNode(it) }
    return mapping {
      // 处理 id: Long? -> Long
      to::id fromValue (from.id ?: 0L)
      // 处理枚举跨包名:域枚举 → GraphQL 生成枚举(同名常量)
      to::discrepancyType fromValue DiscrepancyType.valueOf(from.discrepancyType.name)
      to::sourceSystem fromValue SourceSystem.valueOf(from.sourceSystem.name)
      to::status fromValue DiscrepancyStatus.valueOf(from.status.name)
      // 处理 recordDetails: Map? -> ObjectNode?
      to::recordDetails fromValue recordDetailsNode
    }
  }
}

/**
 * 对账报表实体 → GraphQL 类型映射
 */
object ReconciliationReportModelMapper : ObjectMappie<ReconciliationReportModel, ReconciliationReport>() {
  override fun map(from: ReconciliationReportModel): ReconciliationReport {
    val reportDataNode = JsonHelper().createObjectNode(from.reportData)
    return mapping {
      // 处理 id: Long? -> Long
      to::id fromValue (from.id ?: 0L)
      // 处理 reportData: Map -> ObjectNode
      to::reportData fromValue reportDataNode
    }
  }
}
