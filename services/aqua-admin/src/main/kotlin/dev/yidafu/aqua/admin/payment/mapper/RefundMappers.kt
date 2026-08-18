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

package dev.yidafu.aqua.admin.payment.mapper

import dev.yidafu.aqua.common.domain.model.PaymentRefundModel
import dev.yidafu.aqua.common.graphql.generated.RefundRequest

/**
 * 退款申请实体 → GraphQL RefundRequest(R-PAY-02 后台退款管理页)
 * originalTransactionId 由调用方经支付记录解析后注入。
 */
fun PaymentRefundModel.toRefundRequest(originalTransactionId: String?): RefundRequest =
  RefundRequest(
    id = id?.toString() ?: "",
    originalTransactionId = originalTransactionId ?: "",
    orderId = orderId,
    userId = userId,
    amountCents = refundAmount,
    reason = refundReason ?: "",
    status = status,
    requestedAt = createdAt,
    processedAt = refundedAt,
    processedBy = processedBy,
    adminNote = adminNote,
  )
