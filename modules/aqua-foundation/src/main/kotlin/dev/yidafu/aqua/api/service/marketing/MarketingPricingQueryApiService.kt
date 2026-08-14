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

import dev.yidafu.aqua.common.domain.model.enums.PriceSource

/**
 * 成交单价解析结果(R-MKT-01/02):
 * ① 手动实收价(预留)→ ② 分组商品专属价 → ③ 限时特价 → ④ 新人专享价 → ⑤ 商品目录价
 */
data class PriceResolution(
  val unitPriceCents: Long,
  val priceSource: PriceSource,
  val hasGroupPrice: Boolean,
  // 是否命中限时特价(用户端「特价」展示依据)
  val hasFlashSale: Boolean = false,
  // 是否命中新人专享价(用户端「新人价」展示依据)
  val hasNewcomerPrice: Boolean = false,
)

/**
 * 营销取价跨模块查询服务(R-MKT-01 成交单价解析,供订单域计价与用户端商品价展示)
 */
interface MarketingPricingQueryApiService {
  /**
   * 解析用户在某商品上的成交单价(②分组价 → ③限时特价 → ④新人价 → ⑤目录价)
   */
  fun resolveUnitPrice(
    userId: Long,
    productId: Long,
  ): PriceResolution

  /**
   * 查询用户所属分组(空=未分组)
   */
  fun findGroupId(userId: Long): Long?
}
