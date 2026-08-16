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

package dev.yidafu.aqua.api.service.product

import dev.yidafu.aqua.api.dto.ProductOptionCreateRequest
import dev.yidafu.aqua.api.dto.ProductOptionUpdateRequest
import dev.yidafu.aqua.common.domain.model.ProductOptionModel

/**
 * 商品属性选项字典写操作服务(管理后台,PERMISSION_PRODUCT_WRITE)
 * 与 PRD 03-product.md §F-PRO-009 保持一致:规格/水源地选项的增改删。
 */
interface ProductOptionMutationService {
  /**
   * 新建选项(同类型下 value 未删除行内唯一;sortOrder 为空时排到末尾)
   */
  fun createOption(
    adminId: Long,
    adminName: String,
    request: ProductOptionCreateRequest,
  ): ProductOptionModel

  /**
   * 更新选项(整体覆盖;value 变更时校验同类型内唯一)
   */
  fun updateOption(
    adminId: Long,
    adminName: String,
    id: Long,
    request: ProductOptionUpdateRequest,
  ): ProductOptionModel

  /**
   * 删除选项(软删除;仅移除候选,不影响已引用该值的商品)
   */
  fun deleteOption(
    adminId: Long,
    adminName: String,
    id: Long,
  ): Boolean
}
