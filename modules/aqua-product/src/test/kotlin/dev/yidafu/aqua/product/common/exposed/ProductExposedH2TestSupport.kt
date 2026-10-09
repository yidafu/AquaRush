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

package dev.yidafu.aqua.product.common.exposed

import dev.yidafu.aqua.common.domain.table.ProductFavoritesTable
import dev.yidafu.aqua.common.domain.table.ProductOptionsTable
import dev.yidafu.aqua.common.domain.table.ProductsTable
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.LocalDateTime

/**
 * product 仓储实现测试的 H2 内存库支撑(JPA→Exposed 迁移 P4 覆盖率门禁配套),
 * 模式与 user 的 UserExposedH2TestSupport 一致。
 * products/product_favorites/product_options 均无跨表 FK reference,可直接 insert。
 */
object ProductExposedH2TestSupport {
  val database: Database =
    Database.connect(
      url = "jdbc:h2:mem:aquaproductrepo;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
      driver = "org.h2.Driver",
      user = "sa",
      password = "",
    )

  private val allTables: Array<Table> =
    arrayOf(ProductsTable, ProductFavoritesTable, ProductOptionsTable)

  @Volatile
  private var schemaCreated = false

  fun reset() {
    transaction(ProductExposedH2TestSupport.database) {
      if (schemaCreated) {
        allTables.forEach { it.deleteAll() }
      } else {
        SchemaUtils.create(*allTables)
        schemaCreated = true
      }
    }
  }

  private var nextId = 80000L

  fun id(): Long = nextId++

  /** products 种子:NOT NULL 列全给值(deposit_price_cents 实体无字段,恒 0)。 */
  fun product(
    id: Long = id(),
    name: String = "商品$id",
    priceCents: Long = 1990L,
    stock: Int = 10,
    salesVolume: Int = 0,
    status: String = "ONLINE",
    sortOrder: Int = 999,
    specification: String = "19L",
    detailImages: String? = null,
    tags: String? = null,
    description: String? = null,
    createdAt: LocalDateTime = LocalDateTime.now(),
    deleted: Boolean = false,
  ): Long =
    transaction(ProductExposedH2TestSupport.database) {
      ProductsTable.insert {
        it[ProductsTable.id] = id
        it[ProductsTable.name] = name
        it[ProductsTable.priceCents] = priceCents
        it[thumbnailUrl] = "https://img.test/$id.png"
        it[ProductsTable.stockQuantity] = stock
        it[ProductsTable.status] = status
        it[ProductsTable.specification] = specification
        it[ProductsTable.salesVolume] = salesVolume
        it[ProductsTable.sortOrder] = sortOrder
        it[ProductsTable.detailImages] = detailImages
        it[ProductsTable.tags] = tags
        it[ProductsTable.description] = description
        it[depositPriceCents] = 0L
        it[ProductsTable.createdAt] = createdAt
        it[updatedAt] = createdAt
        it[ProductsTable.isDeleted] = deleted
      }
      id
    }

  fun favorite(
    id: Long = id(),
    userId: Long,
    productId: Long,
    enable: Boolean = true,
    createdAt: LocalDateTime = LocalDateTime.now(),
    deleted: Boolean = false,
  ): Long =
    transaction(ProductExposedH2TestSupport.database) {
      ProductFavoritesTable.insert {
        it[ProductFavoritesTable.id] = id
        it[ProductFavoritesTable.userId] = userId
        it[ProductFavoritesTable.productId] = productId
        it[ProductFavoritesTable.enable] = enable
        it[ProductFavoritesTable.createdAt] = createdAt
        it[updatedAt] = createdAt
        it[ProductFavoritesTable.isDeleted] = deleted
      }
      id
    }

  fun option(
    id: Long = id(),
    optionType: String = "SPECIFICATION",
    value: String = "规格$id",
    sortOrder: Int = 0,
    enabled: Boolean = true,
    createdAt: LocalDateTime = LocalDateTime.now(),
    deleted: Boolean = false,
  ): Long =
    transaction(ProductExposedH2TestSupport.database) {
      ProductOptionsTable.insert {
        it[ProductOptionsTable.id] = id
        it[ProductOptionsTable.optionType] = optionType
        it[optionValue] = value
        it[ProductOptionsTable.sortOrder] = sortOrder
        it[ProductOptionsTable.enabled] = enabled
        it[ProductOptionsTable.createdAt] = createdAt
        it[updatedAt] = createdAt
        it[ProductOptionsTable.isDeleted] = deleted
      }
      id
    }
}
