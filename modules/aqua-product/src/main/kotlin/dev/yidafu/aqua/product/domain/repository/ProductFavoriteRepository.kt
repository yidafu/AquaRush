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
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * 商品收藏仓储(JPA→Exposed 迁移 P4:剥离 JpaRepository/JpaSpecificationExecutor,
 * 仅保留实际被调用的方法;查询过滤 is_deleted)。
 */
interface ProductFavoriteRepository : ProductFavoriteRepositoryCustom {
  fun findByUserIdAndProductId(
    userId: Long,
    productId: Long,
  ): ProductFavoriteModel?

  fun findByUserId(
    userId: Long,
    pageable: Pageable,
  ): Page<ProductFavoriteModel>

  fun findByProductId(productId: Long): List<ProductFavoriteModel>

  fun count(): Long

  /** upsert 语义:无 id 或 id 不存在时插入(雪花 id),否则按 id 更新业务列。 */
  fun save(entity: ProductFavoriteModel): ProductFavoriteModel
}
