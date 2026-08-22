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

package dev.yidafu.aqua.analytics.statistics.domain.repository

import dev.yidafu.aqua.common.domain.model.DailyMarketingFactModel
import dev.yidafu.aqua.common.domain.model.DailyMarketingUsageFactModel
import dev.yidafu.aqua.common.domain.model.DailyOrderFactModel
import dev.yidafu.aqua.common.domain.model.DailyProductFactModel
import dev.yidafu.aqua.common.domain.model.DailyRefundFactModel
import dev.yidafu.aqua.common.domain.model.DailyUserFactModel
import java.time.LocalDate

/**
 * 统计快照按日聚合仓储(第 3 层历史趋势指标中间表写入侧)
 * 对单个日期从源表(orders/order_items/users/user_action_logs/payment_refunds)聚合出原子事实行;
 * 所有聚合均带日期窗口过滤,不做全表扫描(CLAUDE.md 规则 13)。
 */
interface StatisticsSnapshotAggregationRepository {
  /**
   * 聚合单日订单事实:按 (status, delivery_worker_id) 分组。
   * 金额按订单只计一次(orders 单表聚合,不随 order_items 明细重复求和 P1-3);件数按明细聚合后按组键合并。
   */
  fun aggregateOrderFacts(date: LocalDate): List<DailyOrderFactModel>

  /**
   * 聚合单日商品事实:按 (status, product_id) 分组(orders ⋈ order_items)。
   */
  fun aggregateProductFacts(date: LocalDate): List<DailyProductFactModel>

  /**
   * 聚合单日用户事实:新增用户数(users.created_at)+ 登录用户数(USER_LOGIN 按 userId 去重)。
   * 无数据的日期也返回全 0 行,保证事实表按日连续(查询侧无需回退实时)。
   */
  fun aggregateUserFact(date: LocalDate): DailyUserFactModel

  /**
   * 聚合单日退款事实:按 (status, refund_type) 分组,归属日为退款成功时间(processed_at)。
   */
  fun aggregateRefundFacts(date: LocalDate): List<DailyRefundFactModel>

  /**
   * 聚合单日营销事实(F-STA-007):营销订单数(任一快照优惠,按订单去重)+ 券/满减/首单立减让利 +
   * 用券数 + 水票销售/核销 + 赠品件数。无数据的日期也返回全 0 行,保证事实表按日连续。
   */
  fun aggregateMarketingFacts(date: LocalDate): DailyMarketingFactModel

  /**
   * 聚合日期范围营销事实(按日分组,查询侧实时兜底段;空数据日补零)。
   */
  fun aggregateMarketingFactsRange(
    startDate: LocalDate,
    endDate: LocalDate,
  ): List<DailyMarketingFactModel>

  /**
   * 聚合单日营销使用事实(F-STA-007):按 (kind, ref_id) 分组,营销方式使用明细数据源。
   */
  fun aggregateMarketingUsageFacts(date: LocalDate): List<DailyMarketingUsageFactModel>

  /**
   * 聚合日期范围营销使用事实(按 日 × kind × ref_id 分组,查询侧实时兜底段)。
   */
  fun aggregateMarketingUsageFactsRange(
    startDate: LocalDate,
    endDate: LocalDate,
  ): List<DailyMarketingUsageFactModel>

  /**
   * 最早源数据日期(orders/users/user_action_logs/payment_refunds 四源最小创建/处理日期)。
   * 无任何源数据时返回 null;用于首次回填起点。
   */
  fun findEarliestSourceDate(): LocalDate?
}
