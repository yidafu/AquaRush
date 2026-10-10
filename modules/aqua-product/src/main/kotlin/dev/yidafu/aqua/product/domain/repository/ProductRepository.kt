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
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.enums.ProductModelStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.Optional

/**
 * 商品仓储(JPA→Exposed 迁移 P4:剥离 JpaRepository/JpaSpecificationExecutor,
 * 仅保留实际被调用的方法;查询过滤 is_deleted,deleteById 软删)。
 */
interface ProductRepository {
  // 参数须用域枚举 ProductModelStatus(实体字段类型)
  fun findByStatus(status: ProductModelStatus): List<ProductModel>

  fun findById(id: Long): Optional<ProductModel>

  /** 按商品名精确查找(取 1 条;名称为业务唯一语义,DDL 未建唯一索引)。 */
  fun findByName(name: String): ProductModel?

  fun findAllById(ids: List<Long>): List<ProductModel>

  fun existsById(id: Long): Boolean

  /** 语义对齐 @SoftDelete 下的 deleteById:软删。 */
  fun deleteById(id: Long)

  /** upsert 语义:无 id 或 id 不存在时插入(雪花 id),否则按 id 更新业务列。 */
  fun save(entity: ProductModel): ProductModel

  /**
   * Search products with optional keyword and status filters
   * @param keyword Search keyword for product name (uses LIKE query)
   * @param status Optional status filter
   * @param pageable Pagination parameters
   * @return Page of matching products
   */
  fun searchProducts(
    query: ProductSearchRequest,
    pageable: Pageable,
  ): Page<ProductModel>

  /**
   * 原子扣减库存(设计红线 #5 / AC-ORD-001-10,裁决缺陷 #2):
   * 单条条件 UPDATE `SET stock_quantity = stock_quantity - ? WHERE id = ? AND stock_quantity >= ?`
   * (带软删过滤),封死读-改-写超卖窗口;扣减后库存为 0 时同事务置 OUT_OF_STOCK。
   * 返回受影响行数:1=扣减成功,0=库存不足或商品不存在(调用方决定抛错或回退)。
   */
  fun decreaseStockIfAvailable(
    productId: Long,
    quantity: Int,
  ): Int
}
