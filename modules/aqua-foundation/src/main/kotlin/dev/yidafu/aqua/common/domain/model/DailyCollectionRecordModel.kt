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

import dev.yidafu.aqua.common.domain.model.enums.DailyCollectionStatus
import jakarta.persistence.*
import org.hibernate.annotations.SoftDelete
import java.time.LocalDate
import java.time.LocalDateTime
import dev.yidafu.aqua.common.annotation.SnowflakeIdGenerator

/**
 * 每日收款记录实体
 * 记录配送员每日的收款情况，包括现金、水票、扫码等
 */
@Entity
@SoftDelete(columnName = "is_deleted")
@Table(
  name = "daily_collection_records",
  uniqueConstraints = [UniqueConstraint(columnNames = ["delivery_worker_id", "collection_date"])],
)
data class DailyCollectionRecordModel(
  @Id
  @SnowflakeIdGenerator
  @Column(name = "id", nullable = false, updatable = false)
  var id: Long? = null,
  @Column(name = "delivery_worker_id", nullable = false)
  var deliveryWorkerId: Long = 0L,
  @Column(name = "collection_date", nullable = false)
  var collectionDate: LocalDate = LocalDate.now(),
  @Column(name = "cash_amount_cents", nullable = false)
  var cashAmountCents: Long = 0L,
  @Column(name = "water_ticket_count", nullable = false)
  var waterTicketCount: Int = 0,
  @Column(name = "qr_code_amount_cents", nullable = false)
  var qrCodeAmountCents: Long = 0L,
  // 以下字段由系统自动计算
  @Column(name = "order_count")
  var orderCount: Int = 0,
  @Column(name = "order_amount_cents")
  var orderAmountCents: Long = 0L,
  @Column(name = "difference_amount_cents")
  var differenceAmountCents: Long = 0L,
  // 从订单计算出的金额（按支付方式分类）
  @Column(name = "calculated_cash_amount_cents")
  var calculatedCashAmountCents: Long = 0L,
  @Column(name = "calculated_qr_code_amount_cents")
  var calculatedQrCodeAmountCents: Long = 0L,
  @Column(name = "calculated_water_ticket_count")
  var calculatedWaterTicketCount: Int = 0,
  @Column(name = "calculated_order_amount_cents")
  var calculatedOrderAmountCents: Long = 0L,
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  var status: DailyCollectionStatus = DailyCollectionStatus.DRAFT,
  @Column(name = "confirmed_at")
  var confirmedAt: LocalDateTime? = null,
  @Column(name = "confirmed_by")
  var confirmedBy: Long? = null,
  @Column(name = "notes", length = 500)
  var notes: String? = null,
  @Column(name = "created_at", nullable = false, updatable = false)
  val createdAt: LocalDateTime = LocalDateTime.now(),
  @Column(name = "updated_at", nullable = false)
  var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
  /**
   * 手工收款总额（现金 + 扫码）
   * 水票不计入金额，只记录数量
   */
  @Transient
  fun getManualTotalCollectionAmountCents(): Long = cashAmountCents + qrCodeAmountCents

  /**
   * 系统计算收款总额（现金 + 扫码）
   * 从订单计算得出
   */
  @Transient
  fun getCalculatedTotalCollectionAmountCents(): Long = calculatedCashAmountCents + calculatedQrCodeAmountCents

  /**
   * 手工填写的订单金额（已废弃，请使用 calculatedOrderAmountCents）
   */
  @Deprecated("Use calculatedOrderAmountCents instead", ReplaceWith("calculatedOrderAmountCents"))
  @Transient
  fun getOrderAmountCentsLegacy(): Long = orderAmountCents
}
