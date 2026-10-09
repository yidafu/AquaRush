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

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 退款日事实(第 3 层历史趋势指标中间表,夜间快照任务写入)
 * 原子粒度:stat_date(退款成功时间 processed_at 归属日) × status × refund_type
 * - refund_count:退款笔数
 * - refund_amount_cents:退款金额合计
 * 高阶口径(仅 COMPLETED、排除 DEPOSIT_REFUND)在查询时组合(PRD F-STA-002 / R-PAY-02 G06)。
 */
data class DailyRefundFactModel(
  var id: Long? = null,
  var statDate: LocalDate = LocalDate.now(),
  var status: String = "",
  var refundType: String = "",
  var refundCount: Long = 0,
  var refundAmountCents: Long = 0,
  val createdAt: LocalDateTime = LocalDateTime.now(),
  var updatedAt: LocalDateTime = LocalDateTime.now(),
)
