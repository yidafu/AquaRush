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

import dev.yidafu.aqua.common.domain.model.ReviewModel
import dev.yidafu.aqua.common.domain.table.ReviewsTable
import dev.yidafu.aqua.common.domain.table.activeOnly
import dev.yidafu.aqua.common.id.SnowflakeIdGenerator
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * 订单评价仓储实现(JPA→Exposed 迁移 P4,原 QueryDSL 实现整体改写为 Exposed DSL)。
 * 每个查询经 [activeOnly] 过滤 is_deleted(语义对齐 @SoftDelete);
 * 分页 count 与 content 在同一个 [transaction] 内执行;
 * count 使用 Exposed `count()`(替代原 QueryDSL 已废弃的 `fetchCount()`)。
 */
@Repository
class ReviewRepositoryImpl(
  private val database: Database,
) : ReviewRepository {
  private fun toModel(row: ResultRow): ReviewModel =
    ReviewModel(
      id = row[ReviewsTable.id],
      orderId = row[ReviewsTable.orderId],
      userId = row[ReviewsTable.userId],
      deliveryWorkerId = row[ReviewsTable.deliveryWorkerId],
      rating = row[ReviewsTable.rating],
      comment = row[ReviewsTable.comment],
      isAnonymous = row[ReviewsTable.isAnonymous],
      createdAt = row[ReviewsTable.createdAt],
      updatedAt = row[ReviewsTable.updatedAt],
      deletedAt = row[ReviewsTable.deletedAt],
      deletedBy = row[ReviewsTable.deletedBy],
    )

  override fun findByOrderId(orderId: Long): ReviewModel? =
    transaction(database) {
      ReviewsTable
        .selectAll()
        .where { ReviewsTable.activeOnly { ReviewsTable.orderId eq orderId } }
        .limit(1)
        .firstOrNull()
    }?.let(::toModel)

  override fun existsByOrderId(orderId: Long): Boolean =
    transaction(database) {
      ReviewsTable
        .selectAll()
        .where { ReviewsTable.activeOnly { ReviewsTable.orderId eq orderId } }
        .limit(1)
        .any()
    }

  override fun findByUserIdOrderByCreatedAtDesc(
    userId: Long,
    pageable: Pageable,
  ): Page<ReviewModel> {
    val condition: Op<Boolean> = ReviewsTable.userId eq userId
    val (content, total) =
      transaction(database) {
        val content =
          ReviewsTable
            .selectAll()
            .where { ReviewsTable.activeOnly { condition } }
            // 固定 createdAt 降序(对齐原方法名 OrderByCreatedAtDesc),id 降序破平防跨页重复
            .orderBy(ReviewsTable.createdAt to SortOrder.DESC, ReviewsTable.id to SortOrder.DESC)
            .offset(pageable.offset)
            .limit(pageable.pageSize)
            .toList()
            .map(::toModel)
        content to ReviewsTable.selectAll().where { ReviewsTable.activeOnly { condition } }.count()
      }
    return PageImpl(content, pageable, total)
  }

  override fun save(entity: ReviewModel): ReviewModel =
    transaction(database) {
      val id = entity.id ?: SnowflakeIdGenerator.getInstance().generate().also { entity.id = it }
      val now = LocalDateTime.now()
      val exists =
        ReviewsTable
          .selectAll()
          .where { ReviewsTable.id eq id }
          .limit(1)
          .any()
      if (exists) {
        ReviewsTable.update({ ReviewsTable.id eq id }) {
          it[orderId] = entity.orderId
          it[userId] = entity.userId
          it[deliveryWorkerId] = entity.deliveryWorkerId
          it[rating] = entity.rating
          it[comment] = entity.comment
          it[isAnonymous] = entity.isAnonymous
          it[updatedAt] = now
        }
      } else {
        ReviewsTable.insert {
          it[ReviewsTable.id] = id
          it[orderId] = entity.orderId
          it[userId] = entity.userId
          it[deliveryWorkerId] = entity.deliveryWorkerId
          it[rating] = entity.rating
          it[comment] = entity.comment
          it[isAnonymous] = entity.isAnonymous
          it[ReviewsTable.createdAt] = entity.createdAt
          it[updatedAt] = entity.updatedAt
        }
      }
      entity
    }

  // ===== ReviewRepositoryCustom =====

  override fun findReviewsWithFilters(
    deliveryWorkerId: Long?,
    minRating: Int?,
    maxRating: Int?,
    dateFrom: LocalDateTime?,
    dateTo: LocalDateTime?,
    userId: Long?,
    pageable: Pageable,
  ): Page<ReviewModel> {
    var condition: Op<Boolean> = Op.TRUE
    deliveryWorkerId?.let { condition = condition and (ReviewsTable.deliveryWorkerId eq it) }
    minRating?.let { condition = condition and (ReviewsTable.rating greaterEq it) }
    maxRating?.let { condition = condition and (ReviewsTable.rating lessEq it) }
    dateFrom?.let { condition = condition and (ReviewsTable.createdAt greaterEq it) }
    dateTo?.let { condition = condition and (ReviewsTable.createdAt lessEq it) }
    userId?.let { condition = condition and (ReviewsTable.userId eq it) }
    val (content, total) =
      transaction(database) {
        val content =
          ReviewsTable
            .selectAll()
            .where { ReviewsTable.activeOnly { condition } }
            // 对齐原 QueryDSL orderBy(createdAt desc),id 降序破平防跨页重复
            .orderBy(ReviewsTable.createdAt to SortOrder.DESC, ReviewsTable.id to SortOrder.DESC)
            .offset(pageable.offset)
            .limit(pageable.pageSize)
            .toList()
            .map(::toModel)
        content to ReviewsTable.selectAll().where { ReviewsTable.activeOnly { condition } }.count()
      }
    return PageImpl(content, pageable, total)
  }
}
