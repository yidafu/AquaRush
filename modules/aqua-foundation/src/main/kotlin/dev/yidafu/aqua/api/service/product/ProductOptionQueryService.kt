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

import dev.yidafu.aqua.common.domain.model.ProductOptionModel
import dev.yidafu.aqua.common.domain.model.enums.ProductOptionType

/**
 * 商品属性选项字典查询服务(管理后台,PERMISSION_PRODUCT_READ)
 * 与 PRD 03-product.md §F-PRO-009 保持一致:商品表单「规格 / 水源地」下拉选项的来源。
 */
interface ProductOptionQueryService {
  /**
   * 按类型查询选项列表(按 sortOrder 升序)
   *
   * @param enabledOnly 仅返回启用选项(true 用于商品表单下拉;false 用于配置页全量展示)
   */
  fun listOptions(
    type: ProductOptionType,
    enabledOnly: Boolean,
  ): List<ProductOptionModel>

  /**
   * 按 ID 查询选项(不存在返回 null)
   */
  fun getOption(id: Long): ProductOptionModel?
}
