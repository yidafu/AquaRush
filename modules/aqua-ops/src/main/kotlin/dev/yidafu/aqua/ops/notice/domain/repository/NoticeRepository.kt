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

package dev.yidafu.aqua.ops.notice.domain.repository

import dev.yidafu.aqua.common.domain.model.NoticeModel
import dev.yidafu.aqua.common.domain.model.enums.NoticeStatus
import dev.yidafu.aqua.common.domain.model.enums.NoticeType
import dev.yidafu.aqua.common.id.SnowflakeIdGenerator
import dev.yidafu.aqua.ops.common.exposed.pageQuery
import dev.yidafu.aqua.ops.notice.domain.table.NoticeTable
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.andWhere
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
 * 公告列表筛选条件(取代原 `NoticeSpecifications` 的 JPA Criteria)。
 * 排序固定为 置顶优先、发布时间倒序(草稿无发布时间排最后)、创建时间倒序(AC-OPS-002-02)。
 */
data class NoticeFilter(
  val keyword: String? = null,
  val type: NoticeType? = null,
  val status: NoticeStatus? = null,
)

/**
 * 公告仓库（Exposed 实现，F-OPS-002）。
 *
 * 软删/save/updatedAt 语义与 [dev.yidafu.aqua.ops.banner.domain.repository.BannerRepository] 相同，
 * 详见其类 KDoc（`@SoftDelete` 显式过滤、`delete` 只翻转 is_deleted、`@PreUpdate` 对齐）。
 */
@Repository
class NoticeRepository(
  private val database: Database,
) {
  /** 当前生效公告：已发布且处于时间窗内(空=长期)，置顶优先、发布时间倒序(AC-OPS-002-02)。 */
  fun findActiveNotices(now: LocalDateTime): List<NoticeModel> =
    transaction(database) {
      NoticeTable
        .selectAll()
        .andWhere {
          (NoticeTable.status eq NoticeStatus.PUBLISHED) and
            (NoticeTable.startAt.isNull() or NoticeTable.startAt.lessEq(now)) and
            (NoticeTable.endAt.isNull() or NoticeTable.endAt.greaterEq(now))
        }.orderBy(
          NoticeTable.pinned to SortOrder.DESC,
          NoticeTable.publishedAt to SortOrder.DESC_NULLS_LAST,
          NoticeTable.createdAt to SortOrder.DESC,
        ).map(::toModel)
    }

  fun findById(id: Long): Optional<NoticeModel> =
    transaction(database) {
      NoticeTable
        .selectAll()
        .andWhere { (NoticeTable.id eq id) }
        .singleOrNull()
        ?.let(::toModel)
    }.let { Optional.ofNullable(it) }

  /** 管理后台列表：分页 + 过滤，排序同 AC-OPS-002-02。 */
  fun findPage(
    filter: NoticeFilter,
    pageable: Pageable,
  ): Page<NoticeModel> =
    transaction(database) {
      pageQuery(
        buildQuery = {
          NoticeTable.selectAll().andWhere {
            buildList {
              filter.keyword?.takeIf { it.isNotBlank() }?.let { kw ->
                add(NoticeTable.title.lowerCase() like "%${kw.lowercase()}%")
              }
              filter.type?.let { add(NoticeTable.type eq it) }
              filter.status?.let { add(NoticeTable.status eq it) }
              // 过滤项可能为空(软删过滤已固定在查询入口),空列表等价于恒真
            }.reduceOrNull { a, b -> a and b } ?: Op.TRUE
          }
        },
        orderings =
          listOf(
            NoticeTable.pinned to SortOrder.DESC,
            NoticeTable.publishedAt to SortOrder.DESC_NULLS_LAST,
            NoticeTable.createdAt to SortOrder.DESC,
          ),
        pageable = pageable,
        mapper = ::toModel,
      )
    }

  /** 按标题查找(测试/运营按标题定位;取 1 条)。 */
  fun findByTitle(title: String): NoticeModel? =
    transaction(database) {
      NoticeTable
        .selectAll()
        .andWhere { (NoticeTable.title eq title) }
        .orderBy(NoticeTable.createdAt to SortOrder.DESC)
        .limit(1)
        .firstOrNull()
    }?.let(::toModel)

  fun save(notice: NoticeModel): NoticeModel =
    transaction(database) {
      val id =
        notice.id
          ?: SnowflakeIdGenerator.getInstance().generate().also { notice.id = it }
      val exists =
        NoticeTable
          .selectAllIncludingDeleted()
          .where { NoticeTable.id eq id }
          .limit(1)
          .any()
      if (exists) {
        val now = LocalDateTime.now()
        NoticeTable.update({ NoticeTable.id eq id }) {
          it[title] = notice.title
          it[content] = notice.content
          it[type] = notice.type
          it[pinned] = notice.pinned
          it[startAt] = notice.startAt
          it[endAt] = notice.endAt
          it[status] = notice.status
          it[publishedAt] = notice.publishedAt
          it[deletedAt] = notice.deletedAt
          it[deletedBy] = notice.deletedBy
          it[updatedAt] = now
        }
        notice.updatedAt = now
      } else {
        NoticeTable.insert {
          it[NoticeTable.id] = id
          it[isDeleted] = false // 显式写入：Exposed 的 default() 不会进 INSERT 语句，而 DDL 的 NOT NULL 无兜底
          it[title] = notice.title
          it[content] = notice.content
          it[type] = notice.type
          it[pinned] = notice.pinned
          it[startAt] = notice.startAt
          it[endAt] = notice.endAt
          it[status] = notice.status
          it[publishedAt] = notice.publishedAt
          it[deletedAt] = notice.deletedAt
          it[deletedBy] = notice.deletedBy
          it[createdAt] = notice.createdAt
          it[updatedAt] = notice.updatedAt
        }
      }
      notice
    }

  fun saveAll(notices: List<NoticeModel>): List<NoticeModel> = notices.map(::save)

  /** 软删：只翻转 `is_deleted`（与 Hibernate `@SoftDelete` 逐字一致）。 */
  fun delete(notice: NoticeModel) {
    val id = notice.id ?: return
    transaction(database) {
      NoticeTable.update({ NoticeTable.id eq id }) {
        it[isDeleted] = true
      }
    }
  }

  private fun toModel(row: ResultRow): NoticeModel =
    NoticeModel(
      id = row[NoticeTable.id],
      title = row[NoticeTable.title],
      content = row[NoticeTable.content],
      type = row[NoticeTable.type],
      pinned = row[NoticeTable.pinned],
      startAt = row[NoticeTable.startAt],
      endAt = row[NoticeTable.endAt],
      status = row[NoticeTable.status],
      publishedAt = row[NoticeTable.publishedAt],
      deletedAt = row[NoticeTable.deletedAt],
      deletedBy = row[NoticeTable.deletedBy],
      createdAt = row[NoticeTable.createdAt],
      updatedAt = row[NoticeTable.updatedAt],
    )
}
