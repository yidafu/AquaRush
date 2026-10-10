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

import dev.yidafu.aqua.common.domain.model.enums.DepositRefundApplicationStatus
import java.time.LocalDateTime

/**
 * 退押申请实体(R-PRO-04/F-PRO-010):
 * 用户提交(桶数 ≤ 在租押金桶数)→ 门店/送水工回收确认 → 管理员审核退还(微信退款
 * DEPOSIT_REFUND 独立账本)或驳回;应退金额 = 规格押金 × 桶数(提交时快照)。
 */
data class DepositRefundApplicationModel(
  var id: Long? = null,
  var userId: Long,
  var specification: String,
  var bucketCount: Int,
  // 应退金额(分)= 规格押金快照 × 桶数
  var depositCents: Long,
  // 回收地址快照(提交时填写)
  var receiverName: String? = null,
  var receiverPhone: String? = null,
  var receiverDetail: String? = null,
  var remark: String? = null,
  var status: DepositRefundApplicationStatus = DepositRefundApplicationStatus.PENDING,
  var recoveredAt: LocalDateTime? = null,
  var refundedAt: LocalDateTime? = null,
  var rejectedAt: LocalDateTime? = null,
  // 处理人(账号 id:回收确认/审核/驳回)
  var handlerAdminId: Long? = null,
  // 驳回原因(落 refund_remark 同义字段?——设计为状态机 REJECTED 留原因,存 handler 流转备注)
  var rejectReason: String? = null,
  val createdAt: LocalDateTime = LocalDateTime.now(),
  var updatedAt: LocalDateTime = LocalDateTime.now(),
  override var deletedAt: LocalDateTime? = null,
  override var deletedBy: Long? = null,
) : SoftDeletable
