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
import java.util.Optional

/**
 * 商品属性选项字典仓库(F-PRO-009;JPA→Exposed 迁移 P4:
 * 剥离 JpaRepository,仅保留实际被调用的方法;@SoftDelete 语义=查询过滤 is_deleted,delete 软删)。
 */
interface ProductOptionRepository {
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

  fun findById(id: Long): Optional<ProductOptionModel>

  /** 语义对齐 @SoftDelete 下的 delete:软删。 */
  fun delete(entity: ProductOptionModel)

  /** upsert 语义:无 id 或 id 不存在时插入(雪花 id),否则按 id 更新业务列。 */
  fun save(entity: ProductOptionModel): ProductOptionModel

  /**
   * 同类型当前最大排序号(新建默认排末尾用)
   */
  fun findMaxSortOrder(type: ProductOptionType): Int?
}
