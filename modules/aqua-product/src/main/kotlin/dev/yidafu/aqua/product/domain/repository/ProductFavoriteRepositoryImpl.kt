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

package dev.yidafu.aqua.product.domain.repository

import dev.yidafu.aqua.common.domain.model.ProductFavoriteModel
import dev.yidafu.aqua.common.domain.table.ProductFavoritesTable
import dev.yidafu.aqua.common.domain.table.activeOnly
import dev.yidafu.aqua.common.id.SnowflakeIdGenerator
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.countDistinct
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * 商品收藏仓储实现(JPA→Exposed 迁移 P4)。
 * 查询过滤 is_deleted=false(语义对齐 @SoftDelete);收藏无 delete 调用点,
 * 启用/停用走 [updateEnableStatus];保存为 upsert。
 */
@Repository
class ProductFavoriteRepositoryImpl(
  private val database: Database,
) : ProductFavoriteRepository {
  private fun toModel(row: ResultRow): ProductFavoriteModel =
    ProductFavoriteModel(
      id = row[ProductFavoritesTable.id],
      userId = row[ProductFavoritesTable.userId],
      productId = row[ProductFavoritesTable.productId],
      // @ManyToOne(insertable=false) 关联由 JPA 惰性加载;Exposed 不加载,保持 null
      product = null,
      enable = row[ProductFavoritesTable.enable],
      createdAt = row[ProductFavoritesTable.createdAt],
      updatedAt = row[ProductFavoritesTable.updatedAt],
      deletedAt = row[ProductFavoritesTable.deletedAt],
      deletedBy = row[ProductFavoritesTable.deletedBy],
    )

  override fun findByUserIdAndProductId(
    userId: Long,
    productId: Long,
  ): ProductFavoriteModel? =
    transaction(database) {
      ProductFavoritesTable
        .selectAll()
        .where {
          ProductFavoritesTable.activeOnly {
            (ProductFavoritesTable.userId eq userId) and (ProductFavoritesTable.productId eq productId)
          }
        }.limit(1)
        .firstOrNull()
    }?.let(::toModel)

  override fun findByUserId(
    userId: Long,
    pageable: Pageable,
  ): Page<ProductFavoriteModel> {
    val (content, total) =
      transaction(database) {
        val content =
          ProductFavoritesTable
            .selectAll()
            .where { ProductFavoritesTable.activeOnly { ProductFavoritesTable.userId eq userId } }
            .orderBy(ProductFavoritesTable.id to SortOrder.ASC)
            .offset(pageable.offset)
            .limit(pageable.pageSize)
            .toList()
            .map(::toModel)
        content to
          ProductFavoritesTable.selectAll().where { ProductFavoritesTable.activeOnly { ProductFavoritesTable.userId eq userId } }.count()
      }
    return PageImpl(content, pageable, total)
  }

  override fun findByProductId(productId: Long): List<ProductFavoriteModel> =
    transaction(database) {
      ProductFavoritesTable
        .selectAll()
        .where { ProductFavoritesTable.activeOnly { ProductFavoritesTable.productId eq productId } }
        .orderBy(ProductFavoritesTable.id to SortOrder.ASC)
        .toList()
    }.map(::toModel)

  override fun count(): Long =
    transaction(database) {
      ProductFavoritesTable.selectAll().where { ProductFavoritesTable.activeOnly() }.count()
    }

  override fun save(entity: ProductFavoriteModel): ProductFavoriteModel =
    transaction(database) {
      val id = entity.id ?: SnowflakeIdGenerator.getInstance().generate().also { entity.id = it }
      val now = LocalDateTime.now()
      val exists =
        ProductFavoritesTable
          .selectAll()
          .where { ProductFavoritesTable.id eq id }
          .limit(1)
          .any()
      if (exists) {
        ProductFavoritesTable.update({ ProductFavoritesTable.id eq id }) {
          it[userId] = entity.userId
          it[productId] = entity.productId
          it[enable] = entity.enable
          it[updatedAt] = now
        }
      } else {
        ProductFavoritesTable.insert {
          it[ProductFavoritesTable.id] = id
          it[userId] = entity.userId
          it[productId] = entity.productId
          it[enable] = entity.enable
          it[createdAt] = entity.createdAt
          it[updatedAt] = entity.updatedAt
        }
      }
      entity
    }

  // ===== ProductFavoriteRepositoryCustom =====

  override fun findFavoriteProductIdsByUserId(userId: Long): List<Long> =
    transaction(database) {
      ProductFavoritesTable
        .select(ProductFavoritesTable.productId)
        .where {
          ProductFavoritesTable.activeOnly {
            (ProductFavoritesTable.userId eq userId) and (ProductFavoritesTable.enable eq true)
          }
        }.orderBy(ProductFavoritesTable.id to SortOrder.ASC)
        .toList()
    }.map { it[ProductFavoritesTable.productId] }

  override fun findFavoriteIdsByUserId(
    userId: Long,
    pageable: Pageable,
  ): Page<Long> {
    val (content, total) =
      transaction(database) {
        val content =
          ProductFavoritesTable
            .select(ProductFavoritesTable.productId)
            .where {
              ProductFavoritesTable.activeOnly {
                (ProductFavoritesTable.userId eq userId) and (ProductFavoritesTable.enable eq true)
              }
            }.orderBy(ProductFavoritesTable.createdAt to SortOrder.DESC)
            .offset(pageable.offset)
            .limit(pageable.pageSize)
            .toList()
            .map { it[ProductFavoritesTable.productId] }
        content to
          ProductFavoritesTable
            .selectAll()
            .where {
              ProductFavoritesTable.activeOnly {
                (ProductFavoritesTable.userId eq userId) and (ProductFavoritesTable.enable eq true)
              }
            }.count()
      }
    return PageImpl(content, pageable, total)
  }

  override fun existsByUserIdAndProductId(
    userId: Long,
    productId: Long,
  ): Boolean =
    transaction(database) {
      ProductFavoritesTable
        .selectAll()
        .where {
          ProductFavoritesTable.activeOnly {
            (ProductFavoritesTable.userId eq userId) and
              (ProductFavoritesTable.productId eq productId) and
              (ProductFavoritesTable.enable eq true)
          }
        }.limit(1)
        .any()
    }

  override fun countByUserId(userId: Long): Long =
    transaction(database) {
      ProductFavoritesTable
        .selectAll()
        .where {
          ProductFavoritesTable.activeOnly {
            (ProductFavoritesTable.userId eq userId) and (ProductFavoritesTable.enable eq true)
          }
        }.count()
    }

  override fun updateEnableStatus(
    userId: Long,
    productId: Long,
    enable: Boolean,
  ): Int =
    transaction(database) {
      ProductFavoritesTable.update({
        ProductFavoritesTable.activeOnly {
          (ProductFavoritesTable.userId eq userId) and (ProductFavoritesTable.productId eq productId)
        }
      }) {
        it[ProductFavoritesTable.enable] = enable
      }
    }

  // ===== 管理端统计 =====

  override fun countDistinctUsersWithFavorites(): Long =
    transaction(database) {
      val distinctUsers = ProductFavoritesTable.userId.countDistinct()
      ProductFavoritesTable
        .select(distinctUsers)
        .where { ProductFavoritesTable.activeOnly() }
        .first()[distinctUsers]
    }

  override fun countFavoritesSince(startDate: LocalDateTime): Long =
    transaction(database) {
      ProductFavoritesTable
        .selectAll()
        .where {
          ProductFavoritesTable.activeOnly { ProductFavoritesTable.createdAt greaterEq startDate }
        }.count()
    }

  override fun findMostFavoritedProducts(): List<ProductFavoriteRepositoryCustom.ProductFavoriteCount> {
    val favoriteCount = ProductFavoritesTable.productId.count()
    return transaction(database) {
      ProductFavoritesTable
        .select(ProductFavoritesTable.productId, favoriteCount)
        .where { ProductFavoritesTable.activeOnly() }
        .groupBy(ProductFavoritesTable.productId)
        .orderBy(favoriteCount to SortOrder.DESC)
        .map {
          ProductFavoriteRepositoryCustom.ProductFavoriteCount(
            productId = it[ProductFavoritesTable.productId],
            favoriteCount = it[favoriteCount],
          )
        }
    }
  }

  // ===== 导出 =====

  override fun findFavoritesForExport(
    userId: Long?,
    productIds: List<Long>?,
    dateFrom: LocalDateTime?,
    dateTo: LocalDateTime?,
  ): List<ProductFavoriteModel> {
    var condition: Op<Boolean> = Op.TRUE
    userId?.let { condition = condition and (ProductFavoritesTable.userId eq it) }
    productIds?.let { ids ->
      if (ids.isNotEmpty()) {
        condition = condition and (ProductFavoritesTable.productId inList ids)
      }
    }
    dateFrom?.let { condition = condition and (ProductFavoritesTable.createdAt greaterEq it) }
    dateTo?.let { condition = condition and (ProductFavoritesTable.createdAt lessEq it) }
    return transaction(database) {
      ProductFavoritesTable
        .selectAll()
        .where { ProductFavoritesTable.activeOnly { condition } }
        .orderBy(ProductFavoritesTable.id to SortOrder.ASC)
        .toList()
    }.map(::toModel)
  }

  // ===== 周期统计 =====

  override fun getAverageFavoritesPerUser(): Double =
    transaction(database) {
      val totalFavorites =
        ProductFavoritesTable.selectAll().where { ProductFavoritesTable.activeOnly() }.count()
      val distinctUsers = ProductFavoritesTable.userId.countDistinct()
      val distinctCount =
        ProductFavoritesTable
          .select(distinctUsers)
          .where { ProductFavoritesTable.activeOnly() }
          .first()[distinctUsers]
      if (distinctCount > 0) totalFavorites.toDouble() / distinctCount else 0.0
    }
}
