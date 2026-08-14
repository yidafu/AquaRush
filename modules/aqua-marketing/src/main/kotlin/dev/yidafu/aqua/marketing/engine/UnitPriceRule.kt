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

package dev.yidafu.aqua.marketing.engine

import dev.yidafu.aqua.api.service.marketing.MarketingPricingQueryApiService
import org.springframework.stereotype.Component

/**
 * 成交单价解析规则(R-MKT-02):
 * ① 手动实收价(预留,未接入)→ ② 用户所属分组商品专属价(熟客价)→ ③ 商品目录价。
 * 后续限时特价(链路③)/新人专享价(链路④)按同链路插入优先级。
 */
@Component
class UnitPriceRule(
  private val marketingPricingQueryApiService: MarketingPricingQueryApiService,
) : PricingRule {
  override val order: Int = 100

  override fun evaluate(
    ctx: PricingContext,
    state: PricingState,
  ) {
    val resolution = marketingPricingQueryApiService.resolveUnitPrice(ctx.userId, ctx.productId)
    state.unitPriceCents = resolution.unitPriceCents
    state.priceSource = resolution.priceSource
    state.subtotalCents = resolution.unitPriceCents * ctx.quantity
  }
}
