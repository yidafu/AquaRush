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
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 每日收款记录实体
 * 记录配送员每日的收款情况，包括现金、水票、扫码等
 */
data class DailyCollectionRecordModel(
  var id: Long? = null,
  var deliveryWorkerId: Long = 0L,
  var collectionDate: LocalDate = LocalDate.now(),
  var cashAmountCents: Long = 0L,
  var waterTicketCount: Int = 0,
  var qrCodeAmountCents: Long = 0L,
  // 以下字段由系统自动计算
  var orderCount: Int = 0,
  var orderAmountCents: Long = 0L,
  var differenceAmountCents: Long = 0L,
  // 从订单计算出的金额（按支付方式分类）
  var calculatedCashAmountCents: Long = 0L,
  var calculatedQrCodeAmountCents: Long = 0L,
  var calculatedWaterTicketCount: Int = 0,
  var calculatedOrderAmountCents: Long = 0L,
  var status: DailyCollectionStatus = DailyCollectionStatus.DRAFT,
  var confirmedAt: LocalDateTime? = null,
  var confirmedBy: Long? = null,
  var notes: String? = null,
  // 管理员驳回留痕(G28 争议日报驳回,v2.0.0:rejected_at/by/reject_reason)
  var rejectedAt: LocalDateTime? = null,
  var rejectedBy: Long? = null,
  var rejectReason: String? = null,
  val createdAt: LocalDateTime = LocalDateTime.now(),
  var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
  /**
   * 手工收款总额（现金 + 扫码）
   * 水票不计入金额，只记录数量
   */
  fun getManualTotalCollectionAmountCents(): Long = cashAmountCents + qrCodeAmountCents

  /**
   * 系统计算收款总额（现金 + 扫码）
   * 从订单计算得出
   */
  fun getCalculatedTotalCollectionAmountCents(): Long = calculatedCashAmountCents + calculatedQrCodeAmountCents

  /**
   * 手工填写的订单金额（已废弃，请使用 calculatedOrderAmountCents）
   */
  @Deprecated("Use calculatedOrderAmountCents instead", ReplaceWith("calculatedOrderAmountCents"))
  fun getOrderAmountCentsLegacy(): Long = orderAmountCents
}
