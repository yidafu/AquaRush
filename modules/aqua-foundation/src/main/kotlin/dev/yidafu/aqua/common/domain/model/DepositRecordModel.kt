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

import dev.yidafu.aqua.common.domain.model.enums.DepositStatus
import java.time.LocalDateTime

/**
 * 桶押金记录实体(G06/R-MON-03 押金模块,PRD 14-misc.md §核心实体「押金:用户、金额、状态、退还时间」)
 * 押金随下单一并收取(deposit_records 落账),订单取消自动退还,空桶退回由管理员退还。
 */
data class DepositRecordModel(
  var id: Long? = null,
  var orderId: Long,
  var userId: Long,
  // 押金金额(分,R-MON-01)
  var depositAmountCents: Long,
  var status: DepositStatus = DepositStatus.COLLECTED,
  // 收取时间
  var collectedAt: LocalDateTime? = null,
  // 退还时间
  var refundedAt: LocalDateTime? = null,
  // 退还操作人(账号 id)
  var refundedBy: Long? = null,
  var refundRemark: String? = null,
  val createdAt: LocalDateTime = LocalDateTime.now(),
  var updatedAt: LocalDateTime = LocalDateTime.now(),
  override var deletedAt: LocalDateTime? = null,
  override var deletedBy: Long? = null,
) : SoftDeletable
