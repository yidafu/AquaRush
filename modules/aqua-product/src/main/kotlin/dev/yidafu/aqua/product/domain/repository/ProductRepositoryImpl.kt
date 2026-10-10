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

import dev.yidafu.aqua.api.dto.ProductSearchRequest
import dev.yidafu.aqua.common.converter.ArrayNodeConverter
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.enums.ProductModelStatus
import dev.yidafu.aqua.common.domain.table.ProductsTable
import dev.yidafu.aqua.common.domain.table.activeOnly
import dev.yidafu.aqua.common.id.SnowflakeIdGenerator
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.like
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
import java.util.Optional

/**
 * 商品仓储实现(JPA→Exposed 迁移 P4)。
 * 查询过滤 is_deleted=false;deleteById 软删(语义对齐 @SoftDelete)。
 * json 列实体字段为 ArrayNode,仓储层经 [ArrayNodeConverter] 与字符串互转;
 * imageGallery/detailContent 沿用 JPA 实体的物理列名(detail_images/description),
 * products 表的 image_gallery/detail_content 为 DDL 存量列、JPA 从未写入。
 */
@Repository
class ProductRepositoryImpl(
  private val database: Database,
) : ProductRepository {
  private val jsonArray = ArrayNodeConverter()

  private fun toModel(row: ResultRow): ProductModel =
    ProductModel(
      id = row[ProductsTable.id],
      name = row[ProductsTable.name],
      subtitle = row[ProductsTable.subtitle],
      price = row[ProductsTable.priceCents],
      originalPrice = row[ProductsTable.originalPriceCents],
      coverImageUrl = row[ProductsTable.thumbnailUrl],
      imageGallery = jsonArray.convertToEntityAttribute(row[ProductsTable.detailImages]),
      specification = row[ProductsTable.specification],
      waterSource = row[ProductsTable.waterSource],
      mineralContent = row[ProductsTable.mineralContent],
      stock = row[ProductsTable.stockQuantity],
      salesVolume = row[ProductsTable.salesVolume],
      status = ProductModelStatus.valueOf(row[ProductsTable.status]),
      sortOrder = row[ProductsTable.sortOrder],
      tags = jsonArray.convertToEntityAttribute(row[ProductsTable.tags]),
      detailContent = row[ProductsTable.description],
      certificateImages = jsonArray.convertToEntityAttribute(row[ProductsTable.certificateImages]),
      deletedAt = row[ProductsTable.deletedAt],
      deletedBy = row[ProductsTable.deletedBy],
      createdAt = row[ProductsTable.createdAt],
      updatedAt = row[ProductsTable.updatedAt],
    )

  override fun findById(id: Long): Optional<ProductModel> =
    Optional.ofNullable(
      transaction(database) {
        ProductsTable
          .selectAll()
          .where { ProductsTable.activeOnly { ProductsTable.id eq id } }
          .limit(1)
          .firstOrNull()
      }?.let(::toModel),
    )

  override fun findByName(name: String): ProductModel? =
    transaction(database) {
      ProductsTable
        .selectAll()
        .where { ProductsTable.activeOnly { ProductsTable.name eq name } }
        .orderBy(ProductsTable.id to SortOrder.ASC)
        .limit(1)
        .firstOrNull()
    }?.let(::toModel)

  override fun findAllById(ids: List<Long>): List<ProductModel> {
    if (ids.isEmpty()) return emptyList()
    return transaction(database) {
      ProductsTable
        .selectAll()
        .where { ProductsTable.activeOnly { ProductsTable.id inList ids } }
        .orderBy(ProductsTable.id to SortOrder.ASC)
        .toList()
    }.map(::toModel)
  }

  override fun existsById(id: Long): Boolean = findById(id).isPresent

  override fun deleteById(id: Long) {
    transaction(database) {
      val now = LocalDateTime.now()
      // activeOnly:对已软删行二次 delete 为 no-op(对齐旧 JPA load→软删过滤→不落库)
      ProductsTable.update({ ProductsTable.activeOnly { ProductsTable.id eq id } }) {
        it[isDeleted] = true
        it[deletedAt] = now
        it[updatedAt] = now
      }
    }
  }

  override fun save(entity: ProductModel): ProductModel =
    transaction(database) {
      val id = entity.id ?: SnowflakeIdGenerator.getInstance().generate().also { entity.id = it }
      val now = LocalDateTime.now()
      val exists =
        ProductsTable
          .selectAll()
          .where { ProductsTable.id eq id }
          .limit(1)
          .any()
      if (exists) {
        ProductsTable.update({ ProductsTable.id eq id }) {
          it[name] = entity.name
          it[subtitle] = entity.subtitle
          it[priceCents] = entity.price
          it[originalPriceCents] = entity.originalPrice
          it[thumbnailUrl] = entity.coverImageUrl
          it[detailImages] = jsonArray.convertToDatabaseColumn(entity.imageGallery)
          it[specification] = entity.specification
          it[waterSource] = entity.waterSource
          it[mineralContent] = entity.mineralContent
          it[stockQuantity] = entity.stock
          it[salesVolume] = entity.salesVolume
          it[status] = entity.status.name
          it[sortOrder] = entity.sortOrder
          it[tags] = jsonArray.convertToDatabaseColumn(entity.tags)
          it[description] = entity.detailContent
          it[certificateImages] = jsonArray.convertToDatabaseColumn(entity.certificateImages)
          it[updatedAt] = now
        }
      } else {
        ProductsTable.insert {
          it[ProductsTable.id] = id
          it[name] = entity.name
          it[subtitle] = entity.subtitle
          it[priceCents] = entity.price
          it[originalPriceCents] = entity.originalPrice
          it[thumbnailUrl] = entity.coverImageUrl
          it[detailImages] = jsonArray.convertToDatabaseColumn(entity.imageGallery)
          it[specification] = entity.specification
          it[waterSource] = entity.waterSource
          it[mineralContent] = entity.mineralContent
          it[stockQuantity] = entity.stock
          it[salesVolume] = entity.salesVolume
          it[status] = entity.status.name
          it[sortOrder] = entity.sortOrder
          it[tags] = jsonArray.convertToDatabaseColumn(entity.tags)
          it[description] = entity.detailContent
          it[certificateImages] = jsonArray.convertToDatabaseColumn(entity.certificateImages)
          // 实体无押金字段(JPA 依赖 DDL DEFAULT 0);建表不带默认值时该列为 NULL,显式写 0,更新分支不触碰
          it[depositPriceCents] = 0L
          it[ProductsTable.createdAt] = entity.createdAt
          it[updatedAt] = entity.updatedAt
        }
      }
      entity
    }

  override fun findByStatus(status: ProductModelStatus): List<ProductModel> =
    transaction(database) {
      ProductsTable
        .selectAll()
        .where { ProductsTable.activeOnly { ProductsTable.status eq status.name } }
        .orderBy(ProductsTable.id to SortOrder.ASC)
        .toList()
    }.map(::toModel)

  // ===== ProductRepositoryCustom =====

  override fun searchProducts(
    query: ProductSearchRequest,
    pageable: Pageable,
  ): Page<ProductModel> {
    var condition: Op<Boolean> = Op.TRUE
    if (!query.keyword.isNullOrBlank()) {
      condition = condition and (ProductsTable.name like "%${query.keyword}%")
    }
    query.status?.let { condition = condition and (ProductsTable.status eq it.name) }
    query.minPrice?.let { condition = condition and (ProductsTable.priceCents greater it) }
    query.maxPrice?.let { condition = condition and (ProductsTable.priceCents less it) }
    query.minStock?.let { condition = condition and (ProductsTable.stockQuantity greater it) }
    query.maxStock?.let { condition = condition and (ProductsTable.stockQuantity less it) }
    query.minSalesVolume?.let { condition = condition and (ProductsTable.salesVolume greater it) }
    query.maxSalesVolume?.let { condition = condition and (ProductsTable.salesVolume less it) }
    val (sortColumn, sortOrder) =
      when (query.sortBy) {
        "CREATED_AT_ASC" -> ProductsTable.createdAt to SortOrder.ASC
        "CREATED_AT_DESC" -> ProductsTable.createdAt to SortOrder.DESC
        "PRICE_ASC" -> ProductsTable.priceCents to SortOrder.ASC
        "PRICE_DESC" -> ProductsTable.priceCents to SortOrder.DESC
        "SALES_VOLUME_ASC" -> ProductsTable.salesVolume to SortOrder.ASC
        "SALES_VOLUME_DESC" -> ProductsTable.salesVolume to SortOrder.DESC
        "SORT_ORDER_ASC" -> ProductsTable.sortOrder to SortOrder.ASC
        "SORT_ORDER_DESC" -> ProductsTable.sortOrder to SortOrder.DESC
        else -> ProductsTable.createdAt to SortOrder.DESC
      }
    val (content, total) =
      transaction(database) {
        val content =
          ProductsTable
            .selectAll()
            .where { ProductsTable.activeOnly { condition } }
            // id 破平防跨页重复:同值行在页边界不重不漏
            .orderBy(sortColumn to sortOrder, ProductsTable.id to sortOrder)
            .offset(pageable.offset)
            .limit(pageable.pageSize)
            .toList()
            .map(::toModel)
        content to ProductsTable.selectAll().where { ProductsTable.activeOnly { condition } }.count()
      }
    return PageImpl(content, pageable, total)
  }
}
