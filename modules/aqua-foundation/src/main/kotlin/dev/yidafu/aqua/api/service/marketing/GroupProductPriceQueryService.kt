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

package dev.yidafu.aqua.api.service.marketing

import dev.yidafu.aqua.common.domain.model.GroupProductPriceModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * 分组商品专属价查询服务(管理后台,PRD 10-marketing.md §F-MKT-002)
 */
interface GroupProductPriceQueryService {
  /**
   * 分组下已配置的专属价列表(分页)
   */
  fun listByGroup(
    groupId: Long,
    pageable: Pageable,
  ): Page<GroupProductPriceModel>

  /**
   * 按「分组 × 商品」查询(无=回退目录价)
   */
  fun getByGroupAndProduct(
    groupId: Long,
    productId: Long,
  ): GroupProductPriceModel?
}
