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

import dev.yidafu.aqua.common.graphql.generated.RefundStatus
import dev.yidafu.aqua.common.graphql.generated.RefundType
import java.time.LocalDateTime

data class PaymentRefundModel(
  var id: Long? = null,
  val paymentId: Long,
  // 关联订单/用户(FK,见 v1.0.0 迁移 payment_refunds 表)
  val orderId: Long,
  val userId: Long,
  var refundId: String? = null,
  // 商户退款单号(微信 out_refund_no;迁移列名 refund_no)
  var outRefundNo: String? = null,
  var refundAmount: Long,
  // 迁移列名 reason
  var refundReason: String? = null,
  var status: RefundStatus = RefundStatus.PENDING,
  // 退款类型(v2.0.1,R-PAY-02/G06):ORDER_REFUND=订单退款(计入退款统计);DEPOSIT_REFUND=水桶费/桶押金退还(不计入)
  var refundType: RefundType = RefundType.ORDER_REFUND,
  var refundAccount: String? = null,
  // 退款处理完成时间(迁移列名 processed_at;退款统计按此归属时间窗口)
  var refundedAt: LocalDateTime? = null,
  // 审核/处理人(v2.0.1 退款审核流,R-PAY-02)
  var processedBy: Long? = null,
  // 管理员审核备注(v2.0.1)
  var adminNote: String? = null,
  // 退款失败原因(v1.0.0 基础 schema 已有列 payment_refunds.failure_reason)
  var failureReason: String? = null,
  val createdAt: LocalDateTime = LocalDateTime.now(),
  var updatedAt: LocalDateTime = LocalDateTime.now(),
  override var deletedAt: LocalDateTime? = null,
  override var deletedBy: Long? = null,
) : SoftDeletable
