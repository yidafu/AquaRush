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

// analytics 聚合查询涉及的跨域源表(JPA→Exposed 迁移 P3)。
// 这些表的主域分属 trade/marketing/platform/user(P4 迁移时由对应域仓储读写),
// 此处为全量列映射(单一事实源,P4 直接复用,避免 analytics 局部定义导致重复)。
// 列定义逐字对齐 changelog DDL(v1.0.0-init + 后续 addColumn 演进的最终形态)。
// 除特别说明外均为 @SoftDelete 表 —— 查询必须显式过滤各表的 isDeleted 列。
// json 列(DDL 类型 json)一律以 jsonString() 映射,结构化转换由仓储层负责;text() 会在 PostgreSQL 上绑定失败。

/** 订单表(`orders`,trade 域,46 列)。 */
object OrdersTable : SoftDeletableTable("orders") {
  val id = long("id")
  val orderNo = varchar("order_no", 64)
  val userId = long("user_id")
  val clientRequestId = varchar("client_request_id", 64).nullable()
  val deliveryTimeSlot = varchar("delivery_time_slot", 20).nullable()
  val totalAmountCents = long("total_amount_cents")
  val deliveryFeeCents = long("delivery_fee_cents")
  val unitPriceCents = long("unit_price_cents").nullable()
  val priceSource = varchar("price_source", 30).nullable()
  val promotionActivityId = long("promotion_activity_id").nullable()
  val promotionActivityName = varchar("promotion_activity_name", 100).nullable()
  val promotionDiscountCents = long("promotion_discount_cents").default(0L)
  val couponId = long("coupon_id").nullable()
  val couponName = varchar("coupon_name", 100).nullable()
  val couponDiscountCents = long("coupon_discount_cents").default(0L)
  val ticketRedeemCount = integer("ticket_redeem_count").default(0)
  val ticketDiscountCents = long("ticket_discount_cents").default(0L)
  val firstOrderActivityId = long("first_order_activity_id").nullable()
  val firstOrderActivityName = varchar("first_order_activity_name", 100).nullable()
  val firstOrderDiscountCents = long("first_order_discount_cents").default(0L)
  val addressId = long("address_id")
  val status = varchar("status", 20)
  val paymentMethod = varchar("payment_method", 20).nullable()
  val paymentTransactionId = varchar("payment_transaction_id", 128).nullable()
  val paidAt = datetime("paid_at").nullable()
  val deliveryWorkerId = long("delivery_worker_id").nullable()
  val deliveryPhotos = jsonString("delivery_photos").nullable()
  val deliveryPhotoReviewStatus = varchar("delivery_photo_review_status", 20).nullable()
  val deliveryPhotoReviewedAt = datetime("delivery_photo_reviewed_at").nullable()
  val deliveryPhotoReviewedBy = long("delivery_photo_reviewed_by").nullable()
  val deliveryPhotoReviewRemark = varchar("delivery_photo_review_remark", 500).nullable()
  val depositAmountCents = long("deposit_amount_cents")
  val isSelfCollect = bool("is_self_collect").default(false)
  val paymentType = varchar("payment_type", 20).nullable()
  val deliveryStartedAt = datetime("delivery_started_at").nullable()
  val deliveryConfirmedAt = datetime("delivery_confirmed_at").nullable()
  val completedAt = datetime("completed_at").nullable()
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
  val remark = varchar("remark", 500).nullable()
  val deliveryRemark = varchar("delivery_remark", 500).nullable()
  val searchContent = text("search_content").nullable()
  override val primaryKey = PrimaryKey(id)
}

/** 订单明细表(`order_items`,trade 域,11 列)。 */
object OrderItemsTable : SoftDeletableTable("order_items") {
  val id = long("id")
  val orderId = long("order_id")
  val productId = long("product_id")
  val quantity = integer("quantity")
  val unitPriceCents = long("unit_price_cents")
  val totalPriceCents = long("total_price_cents")
  val productSnapshot = jsonString("product_snapshot")
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
  override val primaryKey = PrimaryKey(id)
}

/** 用户表(`users`,user 域,17 列)。 */
object UsersTable : SoftDeletableTable("users") {
  val id = long("id")
  val wechatOpenid = varchar("wechat_openid", 128)
  val wechatUnionid = varchar("wechat_unionid", 128).nullable()
  val nickname = varchar("nickname", 255).nullable()
  val phone = varchar("phone", 20).nullable()
  val avatarUrl = varchar("avatar_url", 512).nullable()
  val email = varchar("email", 255)
  val status = varchar("status", 50)
  val role = varchar("role", 50)
  val balanceCents = long("balance_cents").default(0L)
  val totalSpentCents = long("total_spent_cents").default(0L)
  val customerGroupId = long("customer_group_id").nullable()
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
  val lastLoginAt = datetime("last_login_at")
  override val primaryKey = PrimaryKey(id)
}

/**
 * 用户行为日志表(`user_action_logs`,platform 域,由 v1.3.0 raw SQL 建的 RANGE 分区表)。
 * 无软删列;主键为 (id, created_at) 复合 —— analytics 仅按日期窗口聚合,不按单 id 检索。
 */
object UserActionLogsTable : Table("user_action_logs") {
  val id = long("id")
  val userId = varchar("user_id", 50).nullable()
  val username = varchar("username", 100).nullable()
  val actionType = varchar("action_type", 50)
  val target = varchar("target", 500).nullable()
  val pageUrl = varchar("page_url", 1000).nullable()
  val elementId = varchar("element_id", 100).nullable()
  val elementType = varchar("element_type", 50).nullable()
  val elementText = varchar("element_text", 200).nullable()
  val clientIp = varchar("client_ip", 50).nullable()
  val userAgent = varchar("user_agent", 500).nullable()
  val properties = text("properties").nullable()
  val createdAt = datetime("created_at")
  override val primaryKey = PrimaryKey(id, createdAt)
}

/** 退款表(`payment_refunds`,trade 域,20 列)。注意列名与属性名的映射:reason/refund_no/processed_at。 */
object PaymentRefundsTable : SoftDeletableTable("payment_refunds") {
  val id = long("id")
  val paymentId = long("payment_id")
  val orderId = long("order_id")
  val userId = long("user_id")
  val refundId = varchar("refund_id", 255).nullable()
  val refundNo = varchar("refund_no", 100).nullable()
  val refundAmountCents = long("refund_amount_cents")
  val reason = varchar("reason", 500).nullable()
  val status = varchar("status", 20)
  val refundAccount = varchar("refund_account", 100).nullable()
  val failureReason = text("failure_reason").nullable()
  val processedAt = datetime("processed_at").nullable()
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
  val refundType = varchar("refund_type", 20)
  val adminNote = varchar("admin_note", 1000).nullable()
  val processedBy = long("processed_by").nullable()
  override val primaryKey = PrimaryKey(id)
}

/** 用户券表(`user_coupons`,marketing 域,21 列)。 */
object UserCouponsTable : SoftDeletableTable("user_coupons") {
  val id = long("id")
  val userId = long("user_id")
  val couponTemplateId = long("coupon_template_id")
  val status = varchar("status", 30)
  val name = varchar("name", 100)
  val couponType = varchar("coupon_type", 30)
  val thresholdCents = long("threshold_cents").nullable()
  val discountAmountCents = long("discount_amount_cents").nullable()
  val discountRatePermille = long("discount_rate_permille").nullable()
  val scope = varchar("scope", 30)
  val productId = long("product_id").nullable()
  val expireTime = datetime("expire_time")
  val claimTime = datetime("claim_time")
  val usedOrderId = long("used_order_id").nullable()
  val usedAt = datetime("used_at").nullable()
  val returnedAt = datetime("returned_at").nullable()
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
  override val primaryKey = PrimaryKey(id)
}

/** 购桶票订单表(`ticket_purchases`,marketing 域,16 列)。 */
object TicketPurchasesTable : SoftDeletableTable("ticket_purchases") {
  val id = long("id")
  val userId = long("user_id")
  val ticketTypeId = long("ticket_type_id")
  val orderNo = varchar("order_no", 64)
  val packageCount = integer("package_count")
  val giftCount = integer("gift_count")
  val totalCount = integer("total_count")
  val packagePriceCents = long("package_price_cents")
  val paymentStatus = varchar("payment_status", 30)
  val transactionId = varchar("transaction_id", 64).nullable()
  val paidAt = datetime("paid_at").nullable()
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
  override val primaryKey = PrimaryKey(id)
}

/** 用户桶票表(`user_tickets`,marketing 域,16 列;ticket_no 有 DB 唯一约束 —— 列级不表达,此处 KDoc 留痕)。 */
object UserTicketsTable : SoftDeletableTable("user_tickets") {
  val id = long("id")
  val userId = long("user_id")
  val ticketTypeId = long("ticket_type_id")
  val purchaseId = long("purchase_id")
  val status = varchar("status", 30)
  val expireTime = datetime("expire_time")
  val isGift = bool("is_gift").default(false)
  val usedOrderId = long("used_order_id").nullable()
  val usedAt = datetime("used_at").nullable()
  val returnedAt = datetime("returned_at").nullable()
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
  val ticketNo = varchar("ticket_no", 32)
  override val primaryKey = PrimaryKey(id)
}

/** 订单赠品表(`order_gifts`,marketing 域,8 列,无软删列)。 */
object OrderGiftsTable : Table("order_gifts") {
  val id = long("id")
  val orderId = long("order_id")
  val giftActivityId = long("gift_activity_id").nullable()
  val giftActivityName = varchar("gift_activity_name", 100).nullable()
  val productId = long("product_id")
  val productName = varchar("product_name", 100)
  val quantity = integer("quantity")
  val createdAt = datetime("created_at")
  override val primaryKey = PrimaryKey(id)
}

/** 商品表(`products`,product 域,25 列;image_gallery 为遗留死列,全量映射)。 */
object ProductsTable : SoftDeletableTable("products") {
  val id = long("id")
  val name = varchar("name", 255)
  val priceCents = long("price_cents")
  val thumbnailUrl = varchar("thumbnail_url", 512)
  val detailImages = jsonString("detail_images").nullable()
  val description = text("description").nullable()
  val stockQuantity = integer("stock_quantity")
  val status = varchar("status", 20)
  val subtitle = varchar("subtitle", 500).nullable()
  val originalPriceCents = long("original_price_cents").nullable()
  val depositPriceCents = long("deposit_price_cents")
  val imageGallery = jsonString("image_gallery").nullable()
  val specification = varchar("specification", 100)
  val waterSource = varchar("water_source", 200).nullable()
  val mineralContent = varchar("mineral_content", 200).nullable()
  val salesVolume = integer("sales_volume")
  val sortOrder = integer("sort_order")
  val tags = jsonString("tags").nullable()
  val detailContent = text("detail_content").nullable()
  val certificateImages = jsonString("certificate_images").nullable()
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
  override val primaryKey = PrimaryKey(id)
}

/**
 * 管理员/送水员账号表(`admins`,user 域,24 列)。
 * coordinates/current_location 在 DDL 为 json 列,实体系 String 文本(仓储层直接读写字符串)。
 */
object AdminsTable : SoftDeletableTable("admins") {
  val id = long("id")
  val username = varchar("username", 50)
  val passwordHash = varchar("password_hash", 255)
  val realName = varchar("real_name", 100).nullable()
  val phone = varchar("phone", 20).nullable()
  val role = varchar("role", 20)
  val userId = long("user_id").nullable()
  val lastLoginAt = datetime("last_login_at").nullable()
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
  val avatarUrl = varchar("avatar_url", 512).nullable()
  val onlineStatus = varchar("online_status", 20)
  val coordinates = jsonString("coordinates").nullable()
  val currentLocation = jsonString("current_location").nullable()
  val rating = double("rating").nullable()
  val totalOrders = integer("total_orders").default(0)
  val completedOrders = integer("completed_orders").default(0)
  val averageRating = double("average_rating").nullable()
  val earningCents = long("earning_cents").nullable()
  val isAvailable = bool("is_available").default(false)
  val accountStatus = varchar("account_status", 20)
  override val primaryKey = PrimaryKey(id)
}

/** 订单操作流水表(`order_operations`,trade 域,8 列,无软删列)。 */
object OrderOperationsTable : Table("order_operations") {
  val id = long("id")
  val orderId = long("order_id")
  val operationType = varchar("operation_type", 50)
  val operatorId = long("operator_id").nullable()
  val operatorType = varchar("operator_type", 20).nullable()
  val description = varchar("description", 500).nullable()
  val extraData = text("extra_data").nullable()
  val createdAt = datetime("created_at")
  override val primaryKey = PrimaryKey(id)
}
