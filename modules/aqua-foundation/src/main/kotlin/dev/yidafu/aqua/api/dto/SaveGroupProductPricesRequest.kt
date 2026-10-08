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

package dev.yidafu.aqua.api.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

/**
 * 保存分组商品专属价请求(批量,PRD 10-marketing.md §F-MKT-002):
 * 按「商品 × 分组」upsert;clearUnlisted=true 时清空该分组未在本批内的已配置价格。
 */
data class SaveGroupProductPricesRequest(
  @field:NotNull(message = "客户分组不能为空")
  val customerGroupId: Long,
  @field:NotEmpty(message = "商品价格列表不能为空")
  val items: List<@Valid GroupProductPriceItem>,
  val clearUnlisted: Boolean = false,
)
