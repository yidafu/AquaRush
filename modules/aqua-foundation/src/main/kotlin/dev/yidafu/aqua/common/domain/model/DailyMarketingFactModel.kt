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

package dev.yidafu.aqua.common.domain.model

import dev.yidafu.aqua.common.annotation.SnowflakeIdGenerator
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 营销日事实(第 3 层历史趋势指标中间表,夜间快照任务写入,F-STA-007)
 * 原子粒度:stat_date(单日一行,无数据日期也落全 0 行保证覆盖连续)
 * - marketing_order_count:当日营销订单数(订单含 券/满减/首单立减 任一快照优惠 > 0,按订单去重)
 * - coupon/promotion/first_order_benefit_cents:当日各营销方式优惠金额合计(订单快照列,单位分 R-MON-01)
 * - coupon_redeemed_count:当日核销券张数(user_coupons USED 按 used_at)
 * - ticket_sales_count/ticket_sales_revenue_cents:当日已支付购票张数/金额(ticket_purchases PAID 按 paidAt)
 * - ticket_redeemed_count:当日水票核销张数(user_tickets USED 按 usedAt)
 * - gift_quantity:当日赠品件数(order_gifts 按 created_at)
 */
@Entity
@Table(
  name = "daily_marketing_facts",
  uniqueConstraints = [UniqueConstraint(columnNames = ["stat_date"])],
)
data class DailyMarketingFactModel(
  @Id
  @SnowflakeIdGenerator
  @Column(name = "id", nullable = false, updatable = false)
  var id: Long? = null,
  @Column(name = "stat_date", nullable = false)
  var statDate: LocalDate = LocalDate.now(),
  @Column(name = "marketing_order_count", nullable = false)
  var marketingOrderCount: Long = 0,
  @Column(name = "coupon_benefit_cents", nullable = false)
  var couponBenefitCents: Long = 0,
  @Column(name = "promotion_benefit_cents", nullable = false)
  var promotionBenefitCents: Long = 0,
  @Column(name = "first_order_benefit_cents", nullable = false)
  var firstOrderBenefitCents: Long = 0,
  @Column(name = "coupon_redeemed_count", nullable = false)
  var couponRedeemedCount: Long = 0,
  @Column(name = "ticket_sales_count", nullable = false)
  var ticketSalesCount: Long = 0,
  @Column(name = "ticket_sales_revenue_cents", nullable = false)
  var ticketSalesRevenueCents: Long = 0,
  @Column(name = "ticket_redeemed_count", nullable = false)
  var ticketRedeemedCount: Long = 0,
  @Column(name = "gift_quantity", nullable = false)
  var giftQuantity: Long = 0,
  @Column(name = "created_at", nullable = false, updatable = false)
  val createdAt: LocalDateTime = LocalDateTime.now(),
  @Column(name = "updated_at", nullable = false)
  var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
  @PreUpdate
  fun preUpdate() {
    updatedAt = LocalDateTime.now()
  }
}
