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

import dev.yidafu.aqua.api.dto.SaveGroupProductPricesRequest

/**
 * 分组商品专属价变更服务(管理后台,PRD 10-marketing.md §F-MKT-002)
 * 保存后订单计价即时生效,已生成订单成交价不变(R-MKT-01 快照)。
 */
interface GroupProductPriceMutationService {
  /**
   * 批量保存分组专属价(upsert;priceCents=null 清空该项)
   */
  fun savePrices(
    adminId: Long,
    adminName: String,
    request: SaveGroupProductPricesRequest,
  ): Boolean
}
