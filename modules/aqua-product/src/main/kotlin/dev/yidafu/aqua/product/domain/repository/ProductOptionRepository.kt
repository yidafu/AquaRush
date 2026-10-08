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

import dev.yidafu.aqua.common.domain.model.ProductOptionModel
import dev.yidafu.aqua.common.domain.model.enums.ProductOptionType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * 商品属性选项字典仓库(F-PRO-009)
 * 注意:@SoftDelete 自动过滤已删行。
 * 原手写 @Query 方法已迁移至 QueryDSL,见 [ProductOptionRepositoryCustom]/[ProductOptionRepositoryImpl]。
 */
@Repository
interface ProductOptionRepository :
  JpaRepository<ProductOptionModel, Long>,
  ProductOptionRepositoryCustom {
  /**
   * 按类型查询选项(按 sortOrder 升序,同序按创建时间)
   */
  fun findByOptionTypeOrderBySortOrderAscCreatedAtAsc(type: ProductOptionType): List<ProductOptionModel>

  /**
   * 按类型查询启用选项(按 sortOrder 升序,同序按创建时间)
   */
  fun findByOptionTypeAndEnabledTrueOrderBySortOrderAscCreatedAtAsc(type: ProductOptionType): List<ProductOptionModel>

  /**
   * 按类型与值查询未删除选项(唯一性校验用)
   */
  fun findByOptionTypeAndValue(
    type: ProductOptionType,
    value: String,
  ): ProductOptionModel?
}
