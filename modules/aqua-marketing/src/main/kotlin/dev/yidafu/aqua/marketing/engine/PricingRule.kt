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

/**
 * 促销引擎规则接口(PRD 10-marketing.md §R-MKT-02/07):
 * 规则按 order 升序执行,后继规则在前序结果上累积;
 * 后续规则(限时特价/会员折扣/新人价)按 R-MKT-02 单价链与 R-MKT-07 叠加链插入同接口规则。
 */
interface PricingRule {
  /** 执行顺序(R-MKT-07:①满减 → ②会员折扣 → ③优惠券;单价链规则先行) */
  val order: Int

  /** 计算(纯);orderId 非空时同时执行状态消费(原子核销) */
  fun evaluate(
    ctx: PricingContext,
    state: PricingState,
  )
}
