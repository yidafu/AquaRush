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

import dev.yidafu.aqua.common.domain.model.enums.DailyReconciliationStatus
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.SoftDelete
import org.hibernate.type.SqlTypes
import java.time.LocalDate
import java.time.LocalDateTime
import dev.yidafu.aqua.common.annotation.SnowflakeIdGenerator

/**
 * 每日对账记录实体
 * 汇总每日所有配送员的收款和订单情况
 */
@Entity
@SoftDelete(columnName = "is_deleted")
@Table(name = "daily_reconciliations")
data class DailyReconciliationModel(
  @Id
  @SnowflakeIdGenerator
  @Column(name = "id", nullable = false, updatable = false)
  var id: Long? = null,
  @Column(name = "reconciliation_date", nullable = false, unique = true)
  var reconciliationDate: LocalDate = LocalDate.now(),
  @Column(name = "total_cash_amount_cents", nullable = false)
  var totalCashAmountCents: Long = 0L,
  @Column(name = "total_water_ticket_count", nullable = false)
  var totalWaterTicketCount: Int = 0,
  @Column(name = "total_qr_code_amount_cents", nullable = false)
  var totalQrCodeAmountCents: Long = 0L,
  @Column(name = "total_order_count", nullable = false)
  var totalOrderCount: Int = 0,
  @Column(name = "total_order_amount_cents", nullable = false)
  var totalOrderAmountCents: Long = 0L,
  @Column(name = "total_collection_amount_cents", nullable = false)
  var totalCollectionAmountCents: Long = 0L,
  @Column(name = "discrepancy_amount_cents", nullable = false)
  var discrepancyAmountCents: Long = 0L,
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  var status: DailyReconciliationStatus = DailyReconciliationStatus.PENDING,
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "report_data", columnDefinition = "json")
  var reportData: Map<String, Any>? = null,
  @Column(name = "reviewed_at")
  var reviewedAt: LocalDateTime? = null,
  @Column(name = "reviewed_by")
  var reviewedBy: Long? = null,
  @Column(name = "review_notes", length = 500)
  var reviewNotes: String? = null,
  @Column(name = "created_at", nullable = false, updatable = false)
  val createdAt: LocalDateTime = LocalDateTime.now(),
  @Column(name = "updated_at", nullable = false)
  var updatedAt: LocalDateTime = LocalDateTime.now(),
)
