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

package dev.yidafu.aqua.common.domain.model

import java.math.BigDecimal
import java.time.LocalDateTime

data class PaymentReportRow(
    val year: Int,
    val month: Int,
    val totalAmount: BigDecimal,
    val totalTransactions: Int,
    val successfulTransactions: Int,
    val failedTransactions: Int,
    val refundCount: Int,
    val averageTransactionAmount: BigDecimal,
    val periodStart: LocalDateTime,
    val periodEnd: LocalDateTime
)