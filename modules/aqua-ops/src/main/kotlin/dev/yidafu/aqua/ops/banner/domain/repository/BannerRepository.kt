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

package dev.yidafu.aqua.ops.banner.domain.repository

import dev.yidafu.aqua.common.domain.model.BannerModel
import dev.yidafu.aqua.common.id.SnowflakeIdGenerator
import dev.yidafu.aqua.ops.banner.domain.table.BannerTable
import dev.yidafu.aqua.ops.common.exposed.pageQuery
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.max
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.Optional

/**
 * 轮播图列表筛选条件(取代原 `BannerSpecifications` 的 JPA Criteria)。
 * 排序固定为 sort_order 升序（与迁移前 `BannerQueryServiceImpl` 的 `Sort.by(ASC, "sortOrder")` 一致）。
 */
data class BannerFilter(
  val keyword: String? = null,
  val enabled: Boolean? = null,
)

/**
 * 轮播图仓库（Exposed 实现，F-OPS-001）。
 *
 * 迁移语义说明（相对 JPA 版本）：
 * - **软删过滤必须显式**：原 `@SoftDelete` 由 Hibernate 在 SQL 层自动追加 `is_deleted = false`，
 *   Exposed 无此机制，每个查询都要带该条件（漏掉即查出已删数据）。
 * - **`delete` 语义**：与 Hibernate `@SoftDelete` 一致，只翻转 `is_deleted`，
 *   **不**写 `deleted_at/deleted_by`（迁移前两者也保持 null，勿"顺手"补写改变行为）。
 * - **`save` 语义**：insert 时生成 Snowflake id（对齐 `@SnowflakeIdGenerator`）；
 *   update 时显式刷新 `updated_at`（对齐实体 `@PreUpdate`）。
 * - `findById` 保留 `Optional` 返回值：40+ 个调用点（含 MockK 测试桩）依赖该签名，见迁移方案 §6。
 */
@Repository
class BannerRepository(
  private val database: Database,
) {
  fun findById(id: Long): Optional<BannerModel> =
    transaction(database) {
      BannerTable
        .selectAll()
        .where {
          (BannerTable.id eq id) and (BannerTable.isDeleted eq false)
        }.singleOrNull()
        ?.let(::toModel)
    }.let { Optional.ofNullable(it) }

  /** 按排序号查询(编辑时唯一性校验用)。 */
  fun findBySortOrder(sortOrder: Int): BannerModel? =
    transaction(database) {
      BannerTable
        .selectAll()
        .where {
          (BannerTable.sortOrder eq sortOrder) and (BannerTable.isDeleted eq false)
        }.singleOrNull()
        ?.let(::toModel)
    }

  /** 当前最大排序号(新建时缺省排序号 = max + 1)。 */
  fun findMaxSortOrder(): Int? =
    transaction(database) {
      val maxExpr = BannerTable.sortOrder.max()
      BannerTable
        .select(maxExpr)
        .where { BannerTable.isDeleted eq false }
        .singleOrNull()
        ?.get(maxExpr)
    }

  /** 当前生效轮播图：启用且处于时间窗内(空=长期)，按排序号升序。 */
  fun findActiveBanners(now: LocalDateTime): List<BannerModel> =
    transaction(database) {
      BannerTable
        .selectAll()
        .where {
          (BannerTable.enabled eq true) and
            (BannerTable.startAt.isNull() or BannerTable.startAt.lessEq(now)) and
            (BannerTable.endAt.isNull() or BannerTable.endAt.greaterEq(now)) and
            (BannerTable.isDeleted eq false)
        }.orderBy(BannerTable.sortOrder to SortOrder.ASC)
        .map(::toModel)
    }

  /**
   * 按排序号升序取全量并加悲观写锁(上移/下移交换排序号防并发)。
   * 对齐原 QueryDSL `setLockMode(PESSIMISTIC_WRITE)` → `SELECT ... FOR UPDATE`。
   */
  fun findAllOrderedForUpdate(): List<BannerModel> =
    transaction(database) {
      BannerTable
        .selectAll()
        .where { BannerTable.isDeleted eq false }
        .orderBy(BannerTable.sortOrder to SortOrder.ASC)
        .forUpdate()
        .map(::toModel)
    }

  /** 管理后台列表：分页 + 过滤，固定 sort_order 升序。 */
  fun findPage(
    filter: BannerFilter,
    pageable: Pageable,
  ): Page<BannerModel> =
    transaction(database) {
      pageQuery(
        buildQuery = {
          BannerTable.selectAll().where {
            buildList {
              add(BannerTable.isDeleted eq false)
              filter.keyword?.takeIf { it.isNotBlank() }?.let { kw ->
                add(BannerTable.title.lowerCase() like "%${kw.lowercase()}%")
              }
              filter.enabled?.let { add(BannerTable.enabled eq it) }
            }.reduce { a, b -> a and b }
          }
        },
        orderings = listOf(BannerTable.sortOrder to SortOrder.ASC),
        pageable = pageable,
        mapper = ::toModel,
      )
    }

  fun save(banner: BannerModel): BannerModel =
    transaction(database) {
      val id =
        banner.id
          ?: SnowflakeIdGenerator.getInstance().generate().also { banner.id = it }
      val now = LocalDateTime.now()
      val exists =
        BannerTable
          .selectAll()
          .where { BannerTable.id eq id }
          .limit(1)
          .any()
      if (exists) {
        // 对齐实体 @PreUpdate：更新时刷新 updated_at
        BannerTable.update({ BannerTable.id eq id }) {
          it[imageUrl] = banner.imageUrl
          it[title] = banner.title
          it[subtitle] = banner.subtitle
          it[linkType] = banner.linkType
          it[linkTarget] = banner.linkTarget
          it[sortOrder] = banner.sortOrder
          it[enabled] = banner.enabled
          it[startAt] = banner.startAt
          it[endAt] = banner.endAt
          it[deletedAt] = banner.deletedAt
          it[deletedBy] = banner.deletedBy
          it[updatedAt] = now
        }
        banner.updatedAt = now
      } else {
        BannerTable.insert {
          it[BannerTable.id] = id
          it[isDeleted] = false // 显式写入：Exposed 的 default() 不会进 INSERT 语句，而 DDL 的 NOT NULL 无兜底
          it[imageUrl] = banner.imageUrl
          it[title] = banner.title
          it[subtitle] = banner.subtitle
          it[linkType] = banner.linkType
          it[linkTarget] = banner.linkTarget
          it[sortOrder] = banner.sortOrder
          it[enabled] = banner.enabled
          it[startAt] = banner.startAt
          it[endAt] = banner.endAt
          it[deletedAt] = banner.deletedAt
          it[deletedBy] = banner.deletedBy
          it[createdAt] = banner.createdAt
          it[updatedAt] = banner.updatedAt
        }
      }
      banner
    }

  fun saveAll(banners: List<BannerModel>): List<BannerModel> = banners.map(::save)

  /**
   * 软删：只翻转 `is_deleted`，与 Hibernate `@SoftDelete` 行为逐字一致
   * （不写 deleted_at/deleted_by，见类 KDoc）。
   */
  fun delete(banner: BannerModel) {
    val id =
      banner.id ?: return // 与 JPA 版本一致：未持久化的实体本就无可删行
    transaction(database) {
      BannerTable.update({ BannerTable.id eq id }) {
        it[isDeleted] = true
      }
    }
  }

  private fun toModel(row: ResultRow): BannerModel =
    BannerModel(
      id = row[BannerTable.id],
      imageUrl = row[BannerTable.imageUrl],
      title = row[BannerTable.title],
      subtitle = row[BannerTable.subtitle],
      linkType = row[BannerTable.linkType],
      linkTarget = row[BannerTable.linkTarget],
      sortOrder = row[BannerTable.sortOrder],
      enabled = row[BannerTable.enabled],
      startAt = row[BannerTable.startAt],
      endAt = row[BannerTable.endAt],
      deletedAt = row[BannerTable.deletedAt],
      deletedBy = row[BannerTable.deletedBy],
      createdAt = row[BannerTable.createdAt],
      updatedAt = row[BannerTable.updatedAt],
    )
}
