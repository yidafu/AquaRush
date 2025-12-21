/*
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

package dev.yidafu.aqua.api.service

import dev.yidafu.aqua.common.domain.model.AmountRange
import dev.yidafu.aqua.common.domain.model.PaymentModel
import dev.yidafu.aqua.common.domain.model.PaymentReportRow
import dev.yidafu.aqua.common.domain.model.PaymentStatus
import dev.yidafu.aqua.common.graphql.generated.DateRange
import dev.yidafu.aqua.common.graphql.generated.PaymentStatistics
import java.time.LocalDateTime

/**
 * 通用服务接口
 */
interface CommonService {
  /**
   * 查找过期的待支付订单
   */
  fun findExpiredPendingPayments(now: LocalDateTime): List<PaymentModel>

  /**
   * 获取支付统计信息
   */
  fun getPaymentStatistics(
    status: PaymentStatus,
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): PaymentStatistics

  /**
   * 根据灵活条件查找支付记录
   */
  fun findPaymentsWithFlexibleFilters(
    userId: Long? = null,
    status: PaymentStatus? = null,
    transactionId: String? = null,
    dateRange: DateRange? = null,
    amountRange: AmountRange? = null,
  ): List<PaymentModel>

  /**
   * 批量更新支付状态
   */
  fun batchUpdatePaymentStatus(
    paymentIds: List<Long>,
    newStatus: PaymentStatus,
  ): Int

  /**
   * 获取月度支付报告
   */
  fun getPaymentReportByMonth(
    year: Int,
    month: Int,
  ): List<PaymentReportRow>
}
