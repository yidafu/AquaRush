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
 * 水桶押金记录实体(R-MON-03 押金账本,PRD 14-misc.md §核心实体「押金」):
 * 下单按规格建 PENDING,支付成功置 HELD(在租),退押审核/随单退款置 REFUNDED;
 * 同一用户同规格在租只收一次(V346 扩列 specification/unit_deposit_cents/bucket_count,
 * G06 时代历史行这三列可空)。
 */
data class DepositRecordModel(
  var id: Long? = null,
  var orderId: Long,
  var userId: Long,
  // 押金金额(分,R-MON-01)= unit_deposit_cents × bucket_count
  var depositAmountCents: Long,
  // 规格快照(历史 G06 前行可空)
  var specification: String? = null,
  // 单桶押金价快照(分)
  var unitDepositCents: Long? = null,
  // 收取桶数快照
  var bucketCount: Int? = null,
  var status: DepositStatus = DepositStatus.PENDING,
  // 收取时间(支付成功实收确认)
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
