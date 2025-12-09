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

package dev.yidafu.aqua.payment.mapper

import dev.yidafu.aqua.api.dto.PaymentDTO
import dev.yidafu.aqua.common.domain.model.PaymentModel
import tech.mappie.api.ObjectMappie

/**
 * Mapper for converting PaymentModel domain entity to PaymentDTO for API responses
 * Note: This mapper excludes sensitive fields like userId, prepayId, etc.
 */
object PaymentMapper : ObjectMappie<PaymentModel, PaymentDTO>() {
    override fun map(from: PaymentModel) = mapping {
        // Automatic mapping for matching fields (id, orderId, amount, transactionId, status, paidAt, paymentMethod)
        // Manual overrides only needed for field name mismatches

        // Note: The following fields exist in PaymentModel but are intentionally excluded from PaymentDTO:
        // - userId: sensitive information, not exposed to client
        // - prepayId: internal payment pre-transaction ID
        // - currency: fixed as CNY, not needed in response
        // - description: internal payment description
        // - failureReason: contains sensitive information
        // - expiredAt: internal payment expiration time
        // - createdAt, updatedAt: audit fields, not needed by client
    }
}
