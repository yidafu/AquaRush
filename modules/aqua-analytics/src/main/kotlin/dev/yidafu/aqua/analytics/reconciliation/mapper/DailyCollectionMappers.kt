package dev.yidafu.aqua.analytics.reconciliation.mapper

import dev.yidafu.aqua.analytics.reconciliation.resolvers.MyTodayCollectionResult
import dev.yidafu.aqua.common.domain.model.DailyCollectionRecordModel
import dev.yidafu.aqua.common.domain.model.DailyReconciliationModel
import dev.yidafu.aqua.common.graphql.generated.DailyCollectionRecord
import dev.yidafu.aqua.common.graphql.generated.DailyReconciliation
import dev.yidafu.aqua.common.graphql.generated.DailyReconciliationStatus
import dev.yidafu.aqua.common.graphql.generated.MyTodayCollectionVo
import dev.yidafu.aqua.common.util.JsonHelper
import tech.mappie.api.ObjectMappie

object MyTodayCollectionResultMapper : ObjectMappie<MyTodayCollectionResult, MyTodayCollectionVo>()

/**
 * Mapper from JPA entity DailyCollectionRecordModel to GraphQL type DailyCollectionRecord
 * 处理类型转换（LocalDate -> String, Long? -> Long）
 */
object DailyCollectionRecordModelMapper : ObjectMappie<DailyCollectionRecordModel, DailyCollectionRecord>() {
  override fun map(from: DailyCollectionRecordModel): DailyCollectionRecord =
    mapping {
      // 处理 id: Long? -> Long
      to::id fromValue (from.id ?: 0L)
      // 处理 collectionDate: LocalDate -> String
      to::collectionDate fromValue from.collectionDate.toString()
    }
}

/**
 * Mapper from JPA entity DailyReconciliationModel to GraphQL type DailyReconciliation
 * 处理类型转换（LocalDate -> String, Long? -> Long, 状态枚举跨包名, reportData Map -> ObjectNode）
 */
object DailyReconciliationModelMapper : ObjectMappie<DailyReconciliationModel, DailyReconciliation>() {
  override fun map(from: DailyReconciliationModel): DailyReconciliation {
    val reportDataNode = from.reportData?.let { JsonHelper().createObjectNode(it) }
    return mapping {
      to::id fromValue (from.id ?: 0L)
      to::reconciliationDate fromValue from.reconciliationDate.toString()
      to::status fromValue DailyReconciliationStatus.valueOf(from.status.name)
      to::reportData fromValue reportDataNode
    }
  }
}
