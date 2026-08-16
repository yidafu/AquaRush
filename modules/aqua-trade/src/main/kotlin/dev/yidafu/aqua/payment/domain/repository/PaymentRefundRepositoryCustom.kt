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

package dev.yidafu.aqua.payment.domain.repository

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 退款记录聚合查询(PRD 08-statistics.md §F-STA-002 退款/净营收维度):
 * 口径统一 = payment_refunds.status=COMPLETED,按退款成功时间(processed_at)归属窗口。
 */
interface PaymentRefundRepositoryCustom {
  /**
   * 日期窗口内已退款金额合计(分)
   */
  fun sumRefundedAmountByProcessedAtBetween(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): Long

  /**
   * 日期窗口内按日聚合的已退款金额(分,键为退款成功时间所在日期)
   */
  fun sumRefundedAmountGroupByDay(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): Map<LocalDate, Long>
}
