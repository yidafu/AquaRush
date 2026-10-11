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

package dev.yidafu.aqua.common.domain.table

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.javatime.datetime

// 统计日事实表(第 3 层历史趋势指标中间表,JPA→Exposed 迁移 P3)。
// 列定义逐字对齐 changelog 的 DDL(含列名/类型/可空性);对应 JPA 实体为 Daily*FactModel。
// 注意:updated_at 在 model 上非空,但部分 DDL 允许 NULL —— 列定义按 model 语义非空,读到 NULL 行为与 JPA 版一致。

/** 订单日事实(`daily_order_facts`,原子粒度 stat_date × status × delivery_worker_id)。 */
object DailyOrderFactsTable : Table("aqua_daily_order_facts") {
  val id = long("id")
  val statDate = date("stat_date")
  val status = varchar("status", 30)
  val deliveryWorkerId = long("delivery_worker_id").nullable()
  val orderCount = long("order_count").default(0L)
  val amountCents = long("amount_cents").default(0L)
  val itemQuantity = long("item_quantity").default(0L)
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
  override val primaryKey = PrimaryKey(id)
}

/** 商品日事实(`daily_product_facts`,原子粒度 stat_date × status × product_id)。 */
object DailyProductFactsTable : Table("aqua_daily_product_facts") {
  val id = long("id")
  val statDate = date("stat_date")
  val status = varchar("status", 30)
  val productId = long("product_id")
  val quantity = long("quantity").default(0L)
  val revenueCents = long("revenue_cents").default(0L)
  val orderCount = long("order_count").default(0L)
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
  override val primaryKey = PrimaryKey(id)
}

/** 用户日事实(`daily_user_facts`,每日一行,新增/登录计数)。 */
object DailyUserFactsTable : Table("aqua_daily_user_facts") {
  val id = long("id")
  val statDate = date("stat_date")
  val newUserCount = long("new_user_count").default(0L)
  val loginUserCount = long("login_user_count").default(0L)
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
  override val primaryKey = PrimaryKey(id)
}

/** 退款日事实(`daily_refund_facts`,原子粒度 stat_date × status × refund_type)。 */
object DailyRefundFactsTable : Table("aqua_daily_refund_facts") {
  val id = long("id")
  val statDate = date("stat_date")
  val status = varchar("status", 30)
  val refundType = varchar("refund_type", 30)
  val refundCount = long("refund_count").default(0L)
  val refundAmountCents = long("refund_amount_cents").default(0L)
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
  override val primaryKey = PrimaryKey(id)
}

/** 已完成营收日事实(`daily_revenue_facts`,按 completed_at 归属日)。 */
object DailyRevenueFactsTable : Table("aqua_daily_revenue_facts") {
  val id = long("id")
  val statDate = date("stat_date")
  val orderCount = long("order_count").default(0L)
  val amountCents = long("amount_cents").default(0L)
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
  override val primaryKey = PrimaryKey(id)
}

/** 营销日事实(`daily_marketing_facts`,F-STA-007)。 */
object DailyMarketingFactsTable : Table("aqua_daily_marketing_facts") {
  val id = long("id")
  val statDate = date("stat_date")
  val marketingOrderCount = long("marketing_order_count").default(0L)
  val couponBenefitCents = long("coupon_benefit_cents").default(0L)
  val promotionBenefitCents = long("promotion_benefit_cents").default(0L)
  val firstOrderBenefitCents = long("first_order_benefit_cents").default(0L)
  val couponRedeemedCount = long("coupon_redeemed_count").default(0L)
  val ticketSalesCount = long("ticket_sales_count").default(0L)
  val ticketSalesRevenueCents = long("ticket_sales_revenue_cents").default(0L)
  val ticketRedeemedCount = long("ticket_redeemed_count").default(0L)
  val giftQuantity = long("gift_quantity").default(0L)
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
  override val primaryKey = PrimaryKey(id)
}

/** 营销使用日事实(`daily_marketing_usage_facts`,原子粒度 stat_date × kind × ref_id)。 */
object DailyMarketingUsageFactsTable : Table("aqua_daily_marketing_usage_facts") {
  val id = long("id")
  val statDate = date("stat_date")
  val kind = varchar("kind", 30)
  val refId = long("ref_id")
  val orderCount = long("order_count").default(0L)
  val benefitCents = long("benefit_cents").default(0L)
  val usageCount = long("usage_count").default(0L)
  val ticketSalesCount = long("ticket_sales_count").default(0L)
  val ticketSalesRevenueCents = long("ticket_sales_revenue_cents").default(0L)
  val ticketRedeemedCount = long("ticket_redeemed_count").default(0L)
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
  override val primaryKey = PrimaryKey(id)
}
