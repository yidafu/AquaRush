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

package dev.yidafu.aqua.marketing.ticket.domain.repository

import dev.yidafu.aqua.common.domain.model.TicketPurchaseModel
import dev.yidafu.aqua.common.domain.model.enums.TicketPurchaseStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * 购票单仓库(F-MKT-007):orderNo 作为微信支付 out_trade_no;支付入账原子流转(PENDING→PAID,幂等)。
 */
@Repository
interface TicketPurchaseRepository : JpaRepository<TicketPurchaseModel, Long> {
  fun findByOrderNo(orderNo: String): TicketPurchaseModel?

  fun countByUserIdAndTicketTypeIdAndPaymentStatus(
    userId: Long,
    ticketTypeId: Long,
    paymentStatus: TicketPurchaseStatus,
  ): Long

  @Modifying
  @Query(
    """
    update TicketPurchaseModel p
    set p.paymentStatus = dev.yidafu.aqua.common.domain.model.enums.TicketPurchaseStatus.PAID,
        p.transactionId = :transactionId, p.paidAt = :now, p.updatedAt = :now
    where p.id = :id and p.paymentStatus = dev.yidafu.aqua.common.domain.model.enums.TicketPurchaseStatus.PENDING
    """,
  )
  fun markPaidAtomically(
    @Param("id") id: Long,
    @Param("transactionId") transactionId: String,
    @Param("now") now: LocalDateTime,
  ): Int
}
