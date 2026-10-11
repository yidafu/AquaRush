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
import java.math.BigDecimal

// trade 域表(JPA→Exposed 迁移 P4):支付/评价/配送轨迹/送水员统计/每日收款与对账/配送区域。
// 列定义逐字对齐 changelog DDL(v1.0.0 基础 schema + v1.0.4/v1.0.5/v1.2.3/v2.0.0/v2.7.0 演进的最终形态,
// 已与线上库 psql \d 输出逐表核对)。对应 JPA 实体为 PaymentModel/ReviewModel/DeliveryTrajectoryModel 等。
// FK/CHECK/UNIQUE 约束不由 Exposed 表表达(DDL 层持有),唯一约束在各表 KDoc 注明;
// created_at/updated_at 均 NOT NULL,由仓储插入时赋值(与 ReconciliationTables 风格一致)。

/** 支付单表(`payments`,19 列;transaction_id/prepay_id 各有 DB 唯一约束,status 有取值 CHECK)。 */
object PaymentsTable : SoftDeletableTable("aqua_payments") {
  val id = long("id")
  val orderId = long("order_id")
  val userId = long("user_id")
  val transactionId = varchar("transaction_id", 255).nullable()
  val prepayId = varchar("prepay_id", 255).nullable()
  val amountCents = long("amount_cents")
  val currency = varchar("currency", 3)
  val status = varchar("status", 20)
  val paymentMethod = varchar("payment_method", 20)
  val description = text("description").nullable()
  val failureReason = text("failure_reason").nullable()
  val paidAt = datetime("paid_at").nullable()
  val expiredAt = datetime("expired_at").nullable()
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
  val version = long("version").default(0L)
  override val primaryKey = PrimaryKey(id)
}

/** 订单评价表(`reviews`,12 列;order_id 有 DB 唯一约束 —— 一单一评,rating 有 1~5 CHECK)。 */
object ReviewsTable : SoftDeletableTable("aqua_reviews") {
  val id = long("id")
  val orderId = long("order_id")
  val userId = long("user_id")
  val deliveryWorkerId = long("delivery_worker_id")
  val rating = integer("rating")
  val comment = text("comment").nullable()
  val isAnonymous = bool("is_anonymous").default(false)
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
  override val primaryKey = PrimaryKey(id)
}

/** 配送轨迹实时上报表(`delivery_trajectories`,8 列;无软删/updated_at —— 裸 Table)。 */
object DeliveryTrajectoriesTable : Table("aqua_delivery_trajectories") {
  val id = long("id")
  val deliveryWorkerId = long("delivery_worker_id")
  val orderId = long("order_id")
  val longitude = double("longitude")
  val latitude = double("latitude")
  val accuracy = double("accuracy").nullable()
  val reportedAt = datetime("reported_at")
  val createdAt = datetime("created_at")
  override val primaryKey = PrimaryKey(id)
}

/**
 * 送水员评价统计表(`delivery_worker_statistics`,15 列;delivery_worker_id 有 DB 唯一约束)。
 * last_updated 由数据库触发器在 UPDATE 时自动刷新,仓储更新时无需写入。
 * INSERT 必须显式设置 [lastUpdated]:H2 无 CURRENT_TIMESTAMP 列默认,PG 侧也不依赖列默认。
 */
object DeliveryWorkerStatisticsTable : SoftDeletableTable("aqua_delivery_worker_statistics") {
  val id = long("id")
  val deliveryWorkerId = long("delivery_worker_id")
  val averageRating = decimal("average_rating", 3, 2).default(BigDecimal("0.00"))
  val totalReviews = integer("total_reviews").default(0)
  val oneStarReviews = integer("one_star_reviews").default(0)
  val twoStarReviews = integer("two_star_reviews").default(0)
  val threeStarReviews = integer("three_star_reviews").default(0)
  val fourStarReviews = integer("four_star_reviews").default(0)
  val fiveStarReviews = integer("five_star_reviews").default(0)
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
  val lastUpdated = datetime("last_updated")
  override val primaryKey = PrimaryKey(id)
}

/**
 * 每日收款记录表(`daily_collection_records`,23 列)。
 * DDL 仅有 is_deleted(无 deleted_at/deleted_by)—— 不可用 SoftDeletableTable,裸 Table + 自声明软删列;
 * (delivery_worker_id, collection_date) 有 DB 唯一约束。
 */
object DailyCollectionRecordsTable : SoftDeleteFlagTable("aqua_daily_collection_records") {
  val id = long("id")
  val deliveryWorkerId = long("delivery_worker_id")
  val collectionDate = date("collection_date")
  val cashAmountCents = long("cash_amount_cents")
  val waterTicketCount = integer("water_ticket_count")
  val qrCodeAmountCents = long("qr_code_amount_cents")
  val orderCount = integer("order_count").nullable()
  val orderAmountCents = long("order_amount_cents").nullable()
  val differenceAmountCents = long("difference_amount_cents").nullable()
  val status = varchar("status", 20)
  val confirmedAt = datetime("confirmed_at").nullable()
  val confirmedBy = long("confirmed_by").nullable()
  val notes = varchar("notes", 500).nullable()
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
  val calculatedCashAmountCents = long("calculated_cash_amount_cents").default(0L)
  val calculatedQrCodeAmountCents = long("calculated_qr_code_amount_cents").default(0L)
  val calculatedWaterTicketCount = integer("calculated_water_ticket_count").default(0)
  val calculatedOrderAmountCents = long("calculated_order_amount_cents").default(0L)
  val rejectedAt = datetime("rejected_at").nullable()
  val rejectedBy = long("rejected_by").nullable()
  val rejectReason = varchar("reject_reason", 500).nullable()
  override val primaryKey = PrimaryKey(id)
}

/**
 * 每日对账记录表(`daily_reconciliations`,17 列)。
 * 同 daily_collection_records:仅 is_deleted → SoftDeleteFlagTable(无留痕列);reconciliation_date 有 DB 唯一约束;
 * report_data 在 DDL 与线上库均为 text(实体注解写 json,以 DDL 为准,故用 text 而非 jsonString)。
 */
object DailyReconciliationsTable : SoftDeleteFlagTable("aqua_daily_reconciliations") {
  val id = long("id")
  val reconciliationDate = date("reconciliation_date")
  val totalCashAmountCents = long("total_cash_amount_cents")
  val totalWaterTicketCount = integer("total_water_ticket_count")
  val totalQrCodeAmountCents = long("total_qr_code_amount_cents")
  val totalOrderCount = integer("total_order_count")
  val totalOrderAmountCents = long("total_order_amount_cents")
  val totalCollectionAmountCents = long("total_collection_amount_cents")
  val discrepancyAmountCents = long("discrepancy_amount_cents")
  val status = varchar("status", 20)
  val reportData = text("report_data").nullable()
  val reviewedAt = datetime("reviewed_at").nullable()
  val reviewedBy = long("reviewed_by").nullable()
  val reviewNotes = varchar("review_notes", 500).nullable()
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
  override val primaryKey = PrimaryKey(id)
}

/**
 * 配送区域表(`delivery_areas`,11 列)。
 * DDL/线上列名为 `is_enabled`(JPA 实体 `enabled` 漂移,P5 迁移时对齐),此处以 DDL 为准。
 */
object DeliveryAreasTable : SoftDeletableTable("aqua_delivery_areas") {
  val id = long("id")
  val name = varchar("name", 255)
  val province = varchar("province", 50)
  val city = varchar("city", 50)
  val district = varchar("district", 50)
  val isEnabled = bool("is_enabled").default(true)
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
  override val primaryKey = PrimaryKey(id)
}
