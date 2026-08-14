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

import dev.yidafu.aqua.api.dto.OrderPricingRequest
import dev.yidafu.aqua.api.dto.OrderPricingResult

/**
 * 促销引擎(PRD 10-marketing.md:营销规则与配置的唯一归属,订单域应用):
 * 按 R-MKT-02/07 链路统一计算——成交单价解析(②分组价→③目录价)→ 水票核销(被覆盖桶单价 0,
 * 不参与现金优惠 R-MKT-08)→ 满减/折扣(叠加①,多活动取最大 R-MKT-04)→ 优惠券(叠加③,
 * 最优优惠组合自动勾选,用户可改选/取消 R-MKT-03)→ 封顶不超应付(R-MKT-07)。
 * 后续规则(限时特价/会员折扣/新人价)按同链路插入。
 */
interface PromotionPricingService {
  /**
   * 计价预览(纯计算,无副作用):确认订单页费用明细、可用券最优组合、水票核销可用张数
   */
  fun preview(request: OrderPricingRequest): OrderPricingResult

  /**
   * 下单计价并消费(须在订单事务内调用):
   * 与 preview 同计算,并原子核销水票/优惠券(带 orderId 留痕);实际核销数 < 预检 → 抛异常触发整体回滚。
   */
  fun applyForOrder(
    orderId: Long,
    request: OrderPricingRequest,
  ): OrderPricingResult
}
