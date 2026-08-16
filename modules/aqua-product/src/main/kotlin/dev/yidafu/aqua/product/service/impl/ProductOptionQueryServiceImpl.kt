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

package dev.yidafu.aqua.product.service.impl

import dev.yidafu.aqua.api.service.product.ProductOptionQueryService
import dev.yidafu.aqua.common.domain.model.ProductOptionModel
import dev.yidafu.aqua.common.domain.model.enums.ProductOptionType
import dev.yidafu.aqua.product.domain.repository.ProductOptionRepository
import org.springframework.stereotype.Service

/**
 * 商品属性选项字典查询服务实现(F-PRO-009,管理后台)
 * 商品表单「规格 / 水源地」下拉选项的来源;enabledOnly=true 供表单下拉,false 供配置页全量展示。
 */
@Service
class ProductOptionQueryServiceImpl(
  private val productOptionRepository: ProductOptionRepository,
) : ProductOptionQueryService {
  override fun listOptions(
    type: ProductOptionType,
    enabledOnly: Boolean,
  ): List<ProductOptionModel> =
    if (enabledOnly) {
      productOptionRepository.findByOptionTypeAndEnabledTrueOrderBySortOrderAscCreatedAtAsc(type)
    } else {
      productOptionRepository.findByOptionTypeOrderBySortOrderAscCreatedAtAsc(type)
    }

  override fun getOption(id: Long): ProductOptionModel? = productOptionRepository.findById(id).orElse(null)
}
