/**
 * AquaRush
 *
 * Copyright (C) 2026 AquaRush Team
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

package dev.yidafu.aqua.admin.testutil

import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.enums.ProductModelStatus
import dev.yidafu.aqua.product.domain.repository.ProductRepository

/**
 * 统计域集成测试共用种子(P4 收尾:测试库改为 Liquibase 全新 schema 后暴露的缺父行问题)。
 *
 * 背景:历史开发库的 schema 由 Hibernate `ddl-auto` 建出,**没有外键**;改为 Liquibase
 * 迁移的全新 schema(与生产一致)后,`order_items.product_id` 的
 * `fk_order_items_product` 会拒绝只插明细不插商品的种子。统计用例以固定商品 id(1/2/3)
 * 断言聚合口径,故这里按 id 幂等 upsert 这些商品(Exposed save 带 id 即 upsert)。
 */
object StatisticsTestSeeds {
  /** 统计用例固定引用的商品 id(与断言中的字面量一致)。 */
  val productIds: List<Long> = listOf(1L, 2L, 3L)

  fun seedProducts(productRepository: ProductRepository) {
    productIds.forEach { productId ->
      productRepository.save(
        ProductModel(
          id = productId,
          name = "统计测试商品$productId",
          price = 1000L,
          specification = "19L",
          stock = 100,
          status = ProductModelStatus.ONLINE,
          coverImageUrl = "http://localhost:9090/static/stat-product-$productId.png",
        ),
      )
    }
  }
}
