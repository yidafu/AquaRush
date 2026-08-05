package dev.yidafu.aqua.analytics.reconciliation.mapper

import dev.yidafu.aqua.analytics.reconciliation.resolvers.MyTodayCollectionResult
import dev.yidafu.aqua.common.domain.model.DailyCollectionRecordModel
import dev.yidafu.aqua.common.graphql.generated.DailyCollectionRecord
import dev.yidafu.aqua.common.graphql.generated.MyTodayCollectionVo
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
