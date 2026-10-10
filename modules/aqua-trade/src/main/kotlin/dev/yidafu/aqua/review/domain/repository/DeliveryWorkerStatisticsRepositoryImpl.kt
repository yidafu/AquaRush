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

package dev.yidafu.aqua.review.domain.repository

import dev.yidafu.aqua.common.domain.model.DeliveryWorkerStatisticsModel
import dev.yidafu.aqua.common.domain.table.DeliveryWorkerStatisticsTable
import dev.yidafu.aqua.common.id.SnowflakeIdGenerator
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.avg
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 送水员评价统计仓储实现(JPA→Exposed 迁移 P4,原 QueryDSL 实现整体改写为 Exposed DSL)。
 * 每个查询经入口 selectAll()/select() 自动过滤 is_deleted(语义对齐 @SoftDelete);
 * count 使用 Exposed `count()`(替代原 QueryDSL 已废弃的 `fetchCount()`);
 * 聚合口径(平均分/计数条件)与原 QueryDSL 逐条对齐。
 *
 * last_updated:INSERT 显式写入(DDL 无列默认);UPDATE 不写——生产库由触发器刷新
 * (测试表由 SchemaUtils 创建、无触发器,测试仅断言写入值)。
 */
@Repository
class DeliveryWorkerStatisticsRepositoryImpl(
  private val database: Database,
) : DeliveryWorkerStatisticsRepository {
  private fun toModel(row: ResultRow): DeliveryWorkerStatisticsModel =
    DeliveryWorkerStatisticsModel(
      id = row[DeliveryWorkerStatisticsTable.id],
      deliveryWorkerId = row[DeliveryWorkerStatisticsTable.deliveryWorkerId],
      averageRating = row[DeliveryWorkerStatisticsTable.averageRating],
      totalReviews = row[DeliveryWorkerStatisticsTable.totalReviews],
      oneStarReviews = row[DeliveryWorkerStatisticsTable.oneStarReviews],
      twoStarReviews = row[DeliveryWorkerStatisticsTable.twoStarReviews],
      threeStarReviews = row[DeliveryWorkerStatisticsTable.threeStarReviews],
      fourStarReviews = row[DeliveryWorkerStatisticsTable.fourStarReviews],
      fiveStarReviews = row[DeliveryWorkerStatisticsTable.fiveStarReviews],
      lastUpdated = row[DeliveryWorkerStatisticsTable.lastUpdated],
      createdAt = row[DeliveryWorkerStatisticsTable.createdAt],
      updatedAt = row[DeliveryWorkerStatisticsTable.updatedAt],
      deletedAt = row[DeliveryWorkerStatisticsTable.deletedAt],
      deletedBy = row[DeliveryWorkerStatisticsTable.deletedBy],
    )

  override fun findByDeliveryWorkerId(deliveryWorkerId: Long): DeliveryWorkerStatisticsModel? =
    transaction(database) {
      DeliveryWorkerStatisticsTable
        .selectAll()
        .andWhere { DeliveryWorkerStatisticsTable.deliveryWorkerId eq deliveryWorkerId }
        .limit(1)
        .firstOrNull()
    }?.let(::toModel)

  override fun save(entity: DeliveryWorkerStatisticsModel): DeliveryWorkerStatisticsModel =
    transaction(database) {
      val id = entity.id ?: SnowflakeIdGenerator.getInstance().generate().also { entity.id = it }
      val now = LocalDateTime.now()
      val exists =
        DeliveryWorkerStatisticsTable
          .selectAllIncludingDeleted()
          .where { DeliveryWorkerStatisticsTable.id eq id }
          .limit(1)
          .any()
      if (exists) {
        DeliveryWorkerStatisticsTable.update({ DeliveryWorkerStatisticsTable.id eq id }) {
          it[deliveryWorkerId] = entity.deliveryWorkerId
          it[averageRating] = entity.averageRating
          it[totalReviews] = entity.totalReviews
          it[oneStarReviews] = entity.oneStarReviews
          it[twoStarReviews] = entity.twoStarReviews
          it[threeStarReviews] = entity.threeStarReviews
          it[fourStarReviews] = entity.fourStarReviews
          it[fiveStarReviews] = entity.fiveStarReviews
          it[updatedAt] = now
          // last_updated 由生产库触发器刷新,UPDATE 不写(测试表无触发器,保持写入值)
        }
      } else {
        DeliveryWorkerStatisticsTable.insert {
          it[DeliveryWorkerStatisticsTable.id] = id
          it[deliveryWorkerId] = entity.deliveryWorkerId
          it[averageRating] = entity.averageRating
          it[totalReviews] = entity.totalReviews
          it[oneStarReviews] = entity.oneStarReviews
          it[twoStarReviews] = entity.twoStarReviews
          it[threeStarReviews] = entity.threeStarReviews
          it[fourStarReviews] = entity.fourStarReviews
          it[fiveStarReviews] = entity.fiveStarReviews
          // INSERT 必须显式设置 last_updated(H2/PG 均无列默认)
          it[lastUpdated] = entity.lastUpdated
          it[DeliveryWorkerStatisticsTable.createdAt] = entity.createdAt
          it[updatedAt] = entity.updatedAt
        }
      }
      entity
    }

  // ===== DeliveryWorkerStatisticsRepositoryCustom =====

  /**
   * 动态排序的公共实现:主排序(averageRating 或 totalReviews 降序)+ lastUpdated 降序 + id 降序破平,
   * count 与 content 同事务。
   */
  private fun pagedStats(
    condition: Op<Boolean>,
    primarySort: Pair<Expression<*>, SortOrder>,
    pageable: Pageable,
  ): Page<DeliveryWorkerStatisticsModel> {
    val (content, total) =
      transaction(database) {
        val content =
          DeliveryWorkerStatisticsTable
            .selectAll()
            .andWhere { condition }
            .orderBy(
              primarySort,
              DeliveryWorkerStatisticsTable.lastUpdated to SortOrder.DESC,
              DeliveryWorkerStatisticsTable.id to SortOrder.DESC,
            ).offset(pageable.offset)
            .limit(pageable.pageSize)
            .toList()
            .map(::toModel)
        content to DeliveryWorkerStatisticsTable.selectAll().andWhere { condition }.count()
      }
    return PageImpl(content, pageable, total)
  }

  override fun findDeliveryWorkersRanking(
    sortBy: String,
    minReviews: Int,
    pageable: Pageable,
  ): Page<DeliveryWorkerStatisticsModel> {
    val primarySort =
      if (sortBy.lowercase() == "reviews") {
        DeliveryWorkerStatisticsTable.totalReviews to SortOrder.DESC
      } else {
        DeliveryWorkerStatisticsTable.averageRating to SortOrder.DESC
      }
    return pagedStats(DeliveryWorkerStatisticsTable.totalReviews greaterEq minReviews, primarySort, pageable)
  }

  override fun findDeliveryWorkersByRatingRange(
    minRating: Double,
    maxRating: Double,
    minReviews: Int,
    maxReviews: Int?,
    sortBy: String,
    pageable: Pageable,
  ): Page<DeliveryWorkerStatisticsModel> {
    val minRatingBg = BigDecimal.valueOf(minRating)
    val maxRatingBg = BigDecimal.valueOf(maxRating)
    var condition =
      (DeliveryWorkerStatisticsTable.averageRating greaterEq minRatingBg) and
        (DeliveryWorkerStatisticsTable.averageRating lessEq maxRatingBg) and
        (DeliveryWorkerStatisticsTable.totalReviews greaterEq minReviews)
    maxReviews?.let { condition = condition and (DeliveryWorkerStatisticsTable.totalReviews lessEq it) }
    val primarySort =
      if (sortBy.lowercase() == "reviews") {
        DeliveryWorkerStatisticsTable.totalReviews to SortOrder.DESC
      } else {
        DeliveryWorkerStatisticsTable.averageRating to SortOrder.DESC
      }
    return pagedStats(condition, primarySort, pageable)
  }

  override fun countDeliveryWorkersByMinRating(
    minRating: Double,
    minReviews: Int,
  ): Long =
    transaction(database) {
      DeliveryWorkerStatisticsTable
        .selectAll()
        .andWhere {
          (DeliveryWorkerStatisticsTable.averageRating greaterEq BigDecimal.valueOf(minRating)) and
            (DeliveryWorkerStatisticsTable.totalReviews greaterEq minReviews)
        }.count()
    }

  override fun findOverallAverageRating(minReviews: Int): Double? {
    val avgExpr = DeliveryWorkerStatisticsTable.averageRating.avg()
    return transaction(database) {
      DeliveryWorkerStatisticsTable
        .select(avgExpr)
        .andWhere { DeliveryWorkerStatisticsTable.totalReviews greaterEq minReviews }
        .firstOrNull()
        ?.get(avgExpr)
    }?.toDouble()
  }
}
