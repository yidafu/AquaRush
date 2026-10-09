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
import org.jetbrains.exposed.v1.javatime.datetime

// marketing 域表(JPA→Exposed 迁移 P4):券模板/客户分组/分组价/促销活动与档位/限时特价/买赠/
// 首单活动/水票票种与票号序列/水桶费/购桶单/押金记录。
// 列定义逐字对齐 changelog DDL(v1.0.9 + v1.1.0 + v2.0.2 + v2.1.0 + v2.2.0 演进的最终形态,
// 已与线上库 psql \d 输出逐表核对)。对应 JPA 实体为 CouponTemplate/CustomerGroup/Promotion*/Barrel*/Deposit*Model 等;
// ticket_no_sequences 无实体(仓储为原生 SQL)。
// FK/CHECK/UNIQUE 约束不由 Exposed 表表达(DDL 层持有),唯一约束在各表 KDoc 注明;
// created_at/updated_at 均 NOT NULL,由仓储插入时赋值(与 ReconciliationTables 风格一致)。

/** 优惠券模板表(`coupon_templates`,20 列)。 */
object CouponTemplatesTable : SoftDeletableTable("coupon_templates") {
  val id = long("id")
  val name = varchar("name", 100)
  val couponType = varchar("coupon_type", 30)
  val thresholdCents = long("threshold_cents").nullable()
  val discountAmountCents = long("discount_amount_cents").nullable()
  val discountRatePermille = long("discount_rate_permille").nullable()
  val scope = varchar("scope", 30)
  val productId = long("product_id").nullable()
  val validityType = varchar("validity_type", 30)
  val validFrom = datetime("valid_from").nullable()
  val validTo = datetime("valid_to").nullable()
  val validDays = integer("valid_days").nullable()
  val issuanceType = varchar("issuance_type", 30)
  val perUserLimit = integer("per_user_limit").default(1)
  val status = varchar("status", 30)
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
  override val primaryKey = PrimaryKey(id)
}

/** 客户分组表(`customer_groups`,8 列;name 有 DB 唯一约束)。 */
object CustomerGroupsTable : SoftDeletableTable("customer_groups") {
  val id = long("id")
  val name = varchar("name", 50)
  val description = varchar("description", 500).nullable()
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
  override val primaryKey = PrimaryKey(id)
}

/** 分组商品专属价表(`group_product_prices`,9 列;(customer_group_id, product_id) 有 DB 唯一约束)。 */
object GroupProductPricesTable : SoftDeletableTable("group_product_prices") {
  val id = long("id")
  val customerGroupId = long("customer_group_id")
  val productId = long("product_id")
  val priceCents = long("price_cents")
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
  override val primaryKey = PrimaryKey(id)
}

/** 满减/折扣活动表(`promotion_activities`,14 列)。 */
object PromotionActivitiesTable : SoftDeletableTable("promotion_activities") {
  val id = long("id")
  val name = varchar("name", 100)
  val activityType = varchar("activity_type", 30)
  val scope = varchar("scope", 30)
  val productId = long("product_id").nullable()
  val customerGroupId = long("customer_group_id").nullable()
  val validFrom = datetime("valid_from")
  val validTo = datetime("valid_to")
  val status = varchar("status", 30)
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
  override val primaryKey = PrimaryKey(id)
}

/** 活动档位表(`promotion_activity_tiers`,11 列,档位金额递增互斥)。 */
object PromotionActivityTiersTable : SoftDeletableTable("promotion_activity_tiers") {
  val id = long("id")
  val activityId = long("activity_id")
  val thresholdCents = long("threshold_cents")
  val discountAmountCents = long("discount_amount_cents").nullable()
  val discountRatePermille = long("discount_rate_permille").nullable()
  val sort = integer("sort").default(0)
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
  override val primaryKey = PrimaryKey(id)
}

/** 限时特价活动表(`flash_sale_activities`,12 列)。 */
object FlashSaleActivitiesTable : SoftDeletableTable("flash_sale_activities") {
  val id = long("id")
  val name = varchar("name", 100)
  val productId = long("product_id")
  val salePriceCents = long("sale_price_cents")
  val validFrom = datetime("valid_from")
  val validTo = datetime("valid_to")
  val status = varchar("status", 30)
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
  override val primaryKey = PrimaryKey(id)
}

/** 买赠/满赠活动表(`gift_activities`,18 列,v1 单档单赠品)。 */
object GiftActivitiesTable : SoftDeletableTable("gift_activities") {
  val id = long("id")
  val name = varchar("name", 100)
  val giftType = varchar("gift_type", 30)
  val thresholdCents = long("threshold_cents").nullable()
  val thresholdQuantity = integer("threshold_quantity").nullable()
  val scope = varchar("scope", 30)
  val productId = long("product_id").nullable()
  val customerGroupId = long("customer_group_id").nullable()
  val giftProductId = long("gift_product_id")
  val giftQuantity = integer("gift_quantity")
  val validFrom = datetime("valid_from")
  val validTo = datetime("valid_to")
  val status = varchar("status", 30)
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
  override val primaryKey = PrimaryKey(id)
}

/** 首单立减/新人专享价活动表(`first_order_activities`,17 列,两形态单表)。 */
object FirstOrderActivitiesTable : SoftDeletableTable("first_order_activities") {
  val id = long("id")
  val name = varchar("name", 100)
  val activityType = varchar("activity_type", 30)
  val thresholdCents = long("threshold_cents").nullable()
  val discountAmountCents = long("discount_amount_cents").nullable()
  val discountRatePermille = long("discount_rate_permille").nullable()
  val scope = varchar("scope", 30)
  val productId = long("product_id").nullable()
  val newcomerPriceCents = long("newcomer_price_cents").nullable()
  val validFrom = datetime("valid_from")
  val validTo = datetime("valid_to")
  val status = varchar("status", 30)
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
  override val primaryKey = PrimaryKey(id)
}

/** 桶水票票种表(`ticket_types`,19 列)。 */
object TicketTypesTable : SoftDeletableTable("ticket_types") {
  val id = long("id")
  val name = varchar("name", 100)
  val ticketType = varchar("ticket_type", 30)
  val productId = long("product_id").nullable()
  val specification = varchar("specification", 100).nullable()
  val packageCount = integer("package_count")
  val packagePriceCents = long("package_price_cents")
  val giftCount = integer("gift_count").default(0)
  val validityType = varchar("validity_type", 30)
  val validFrom = datetime("valid_from").nullable()
  val validTo = datetime("valid_to").nullable()
  val validDays = integer("valid_days").nullable()
  val perUserLimit = integer("per_user_limit").default(0)
  val status = varchar("status", 30)
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
  override val primaryKey = PrimaryKey(id)
}

/**
 * 水票票号按月计数表(`ticket_no_sequences`,2 列)。
 * 无 bigint id —— 主键为 month_key(varchar(6),YYYYMM);仓储以
 * `INSERT ... ON CONFLICT DO UPDATE ... RETURNING` 原子取号。
 */
object TicketNoSequencesTable : Table("ticket_no_sequences") {
  val monthKey = varchar("month_key", 6)
  val lastSeq = long("last_seq")
  override val primaryKey = PrimaryKey(monthKey)
}

/** 水桶费价目表(`barrel_fees`,10 列;specification 有 DB 唯一约束)。 */
object BarrelFeesTable : SoftDeletableTable("barrel_fees") {
  val id = long("id")
  val specification = varchar("specification", 100)
  val priceCents = long("price_cents")
  val status = varchar("status", 30).default("ENABLED")
  val sortOrder = integer("sort_order").default(999)
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
  override val primaryKey = PrimaryKey(id)
}

/** 水桶购买单表(`barrel_purchases`,16 列;order_no 有 DB 唯一约束,BK 前缀作支付 out_trade_no)。 */
object BarrelPurchasesTable : SoftDeletableTable("barrel_purchases") {
  val id = long("id")
  val userId = long("user_id")
  val orderNo = varchar("order_no", 64)
  val totalAmountCents = long("total_amount_cents")
  val paymentStatus = varchar("payment_status", 30).default("PENDING")
  val transactionId = varchar("transaction_id", 64).nullable()
  val paidAt = datetime("paid_at").nullable()
  val refundedAt = datetime("refunded_at").nullable()
  val refundedBy = long("refunded_by").nullable()
  val refundRemark = varchar("refund_remark", 500).nullable()
  val remark = varchar("remark", 500).nullable()
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
  override val primaryKey = PrimaryKey(id)
}

/** 购桶单明细表(`barrel_purchase_items`,12 列,规格/单价/数量快照)。 */
object BarrelPurchaseItemsTable : SoftDeletableTable("barrel_purchase_items") {
  val id = long("id")
  val purchaseId = long("purchase_id")
  val barrelFeeId = long("barrel_fee_id")
  val specification = varchar("specification", 100)
  val unitPriceCents = long("unit_price_cents")
  val quantity = integer("quantity")
  val amountCents = long("amount_cents")
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
  override val primaryKey = PrimaryKey(id)
}

/** 桶押金记录表(`deposit_records`,14 列,历史数据只读;G06 已由水桶费替代)。 */
object DepositRecordsTable : SoftDeletableTable("deposit_records") {
  val id = long("id")
  val orderId = long("order_id")
  val userId = long("user_id")
  val depositAmountCents = long("deposit_amount_cents")
  val status = varchar("status", 20).default("COLLECTED")
  val collectedAt = datetime("collected_at").nullable()
  val refundedAt = datetime("refunded_at").nullable()
  val refundedBy = long("refunded_by").nullable()
  val refundRemark = varchar("refund_remark", 500).nullable()
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
  override val primaryKey = PrimaryKey(id)
}
