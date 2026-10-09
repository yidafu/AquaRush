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

package dev.yidafu.aqua.ops.feedback.domain.repository

import dev.yidafu.aqua.common.converter.ArrayNodeConverter
import dev.yidafu.aqua.common.domain.model.FeedbackModel
import dev.yidafu.aqua.common.domain.model.enums.FeedbackStatus
import dev.yidafu.aqua.common.domain.model.enums.FeedbackType
import dev.yidafu.aqua.common.id.SnowflakeIdGenerator
import dev.yidafu.aqua.ops.common.exposed.pageQuery
import dev.yidafu.aqua.ops.feedback.domain.table.FeedbackTable
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.Optional

/**
 * 意见反馈列表筛选条件(取代原 `FeedbackSpecifications` 的 JPA Criteria)。
 * 与 PRD 12-operations-config.md §F-OPS-003 保持一致：类型/状态/用户/联系方式关键字/时间范围。
 * 排序固定为 created_at 倒序（与迁移前 `Sort.by(DESC, "createdAt")` 一致）。
 */
data class FeedbackFilter(
  val feedbackType: FeedbackType? = null,
  val status: FeedbackStatus? = null,
  val userId: Long? = null,
  val contactKeyword: String? = null,
  val dateFrom: LocalDateTime? = null,
  val dateTo: LocalDateTime? = null,
)

/**
 * 意见反馈仓库（Exposed 实现，F-OPS-003）。
 *
 * 软删/save/updatedAt 语义见 BannerRepository 类 KDoc。
 * `images` 列（DDL 类型 json）按字符串读写，序列化走 [ArrayNodeConverter]，
 * 与迁移前 JPA AttributeConverter 完全同一条路径。
 */
@Repository
class FeedbackRepository(
  private val database: Database,
) {
  fun findById(id: Long): Optional<FeedbackModel> =
    transaction(database) {
      FeedbackTable
        .selectAll()
        .where { (FeedbackTable.id eq id) and (FeedbackTable.isDeleted eq false) }
        .singleOrNull()
        ?.let(::toModel)
    }.let { Optional.ofNullable(it) }

  /** 按内容模糊查询首条（测试种子数据定位用）。 */
  fun findFirstByContentContaining(content: String): FeedbackModel? =
    transaction(database) {
      FeedbackTable
        .selectAll()
        .where { (FeedbackTable.content like "%$content%") and (FeedbackTable.isDeleted eq false) }
        .limit(1)
        .singleOrNull()
        ?.let(::toModel)
    }

  /** 按用户分页查询（我的-反馈记录，F-USR-005 处理结果回传），仅返回该用户提交的反馈。 */
  fun findByUserId(
    userId: Long,
    pageable: Pageable,
  ): Page<FeedbackModel> =
    transaction(database) {
      pageQuery(
        buildQuery = {
          FeedbackTable.selectAll().where {
            (FeedbackTable.userId eq userId) and (FeedbackTable.isDeleted eq false)
          }
        },
        // 迁移前的派生查询 findByUserId(userId, pageable) 无显式排序，行为保持一致
        orderings = emptyList(),
        pageable = pageable,
        mapper = ::toModel,
      )
    }

  /** 管理后台列表：分页 + 过滤，created_at 倒序。 */
  fun findPage(
    filter: FeedbackFilter,
    pageable: Pageable,
  ): Page<FeedbackModel> =
    transaction(database) {
      pageQuery(
        buildQuery = {
          FeedbackTable.selectAll().where {
            buildList {
              add(FeedbackTable.isDeleted eq false)
              filter.feedbackType?.let { add(FeedbackTable.feedbackType eq it) }
              filter.status?.let { add(FeedbackTable.status eq it) }
              filter.userId?.let { add(FeedbackTable.userId eq it) }
              filter.contactKeyword?.takeIf { it.isNotBlank() }?.let { kw ->
                add(FeedbackTable.contactInfo.lowerCase() like "%${kw.lowercase()}%")
              }
              // 与原 createdAtBetween 一致：两端均为闭区间
              filter.dateFrom?.let { add(FeedbackTable.createdAt greaterEq it) }
              filter.dateTo?.let { add(FeedbackTable.createdAt lessEq it) }
            }.reduce { a, b -> a and b }
          }
        },
        orderings = listOf(FeedbackTable.createdAt to SortOrder.DESC),
        pageable = pageable,
        mapper = ::toModel,
      )
    }

  fun count(): Long =
    transaction(database) {
      FeedbackTable
        .selectAll()
        .where { FeedbackTable.isDeleted eq false }
        .count()
    }

  fun save(feedback: FeedbackModel): FeedbackModel =
    transaction(database) {
      val id =
        feedback.id
          ?: SnowflakeIdGenerator.getInstance().generate().also { feedback.id = it }
      val exists =
        FeedbackTable
          .selectAll()
          .where { FeedbackTable.id eq id }
          .limit(1)
          .any()
      if (exists) {
        val now = LocalDateTime.now()
        FeedbackTable.update({ FeedbackTable.id eq id }) {
          it[userId] = feedback.userId
          it[feedbackType] = feedback.feedbackType
          it[content] = feedback.content
          it[images] = ArrayNodeConverter().convertToDatabaseColumn(feedback.images)
          it[contactInfo] = feedback.contactInfo
          it[status] = feedback.status
          it[handleResult] = feedback.handleResult
          it[handledById] = feedback.handledById
          it[handledByName] = feedback.handledByName
          it[handledAt] = feedback.handledAt
          it[deletedAt] = feedback.deletedAt
          it[deletedBy] = feedback.deletedBy
          it[updatedAt] = now
        }
        feedback.updatedAt = now
      } else {
        FeedbackTable.insert {
          it[FeedbackTable.id] = id
          it[isDeleted] = false // 显式写入：Exposed 的 default() 不会进 INSERT 语句，而 DDL 的 NOT NULL 无兜底
          it[userId] = feedback.userId
          it[feedbackType] = feedback.feedbackType
          it[content] = feedback.content
          it[images] = ArrayNodeConverter().convertToDatabaseColumn(feedback.images)
          it[contactInfo] = feedback.contactInfo
          it[status] = feedback.status
          it[handleResult] = feedback.handleResult
          it[handledById] = feedback.handledById
          it[handledByName] = feedback.handledByName
          it[handledAt] = feedback.handledAt
          it[deletedAt] = feedback.deletedAt
          it[deletedBy] = feedback.deletedBy
          it[createdAt] = feedback.createdAt
          it[updatedAt] = feedback.updatedAt
        }
      }
      feedback
    }

  fun saveAll(feedbacks: List<FeedbackModel>): List<FeedbackModel> = feedbacks.map(::save)

  /** 软删：只翻转 `is_deleted`（与 Banner/Notice 仓储及 Hibernate `@SoftDelete` 行为一致）。 */
  fun delete(feedback: FeedbackModel) {
    val id = feedback.id ?: return
    transaction(database) {
      FeedbackTable.update({ FeedbackTable.id eq id }) {
        it[isDeleted] = true
      }
    }
  }

  private fun toModel(row: ResultRow): FeedbackModel =
    FeedbackModel(
      id = row[FeedbackTable.id],
      userId = row[FeedbackTable.userId],
      feedbackType = row[FeedbackTable.feedbackType],
      content = row[FeedbackTable.content],
      images = ArrayNodeConverter().convertToEntityAttribute(row[FeedbackTable.images]),
      contactInfo = row[FeedbackTable.contactInfo],
      status = row[FeedbackTable.status],
      handleResult = row[FeedbackTable.handleResult],
      handledById = row[FeedbackTable.handledById],
      handledByName = row[FeedbackTable.handledByName],
      handledAt = row[FeedbackTable.handledAt],
      deletedAt = row[FeedbackTable.deletedAt],
      deletedBy = row[FeedbackTable.deletedBy],
      createdAt = row[FeedbackTable.createdAt],
      updatedAt = row[FeedbackTable.updatedAt],
    )
}
