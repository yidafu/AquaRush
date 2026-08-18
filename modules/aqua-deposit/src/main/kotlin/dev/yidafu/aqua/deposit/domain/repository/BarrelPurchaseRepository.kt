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

package dev.yidafu.aqua.deposit.domain.repository

import dev.yidafu.aqua.common.domain.model.BarrelPurchaseModel
import dev.yidafu.aqua.common.domain.model.enums.BarrelPaymentStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * 水桶购买单仓储(G06:一次性购桶,独立支付,每用户总限购一次;支付入账原子流转 PENDING→PAID,幂等)
 */
@Repository
interface BarrelPurchaseRepository :
  JpaRepository<BarrelPurchaseModel, Long>,
  JpaSpecificationExecutor<BarrelPurchaseModel> {
  fun findByOrderNo(orderNo: String): BarrelPurchaseModel?

  fun findFirstByUserIdOrderByCreatedAtDesc(userId: Long): BarrelPurchaseModel?

  fun existsByUserIdAndPaymentStatus(
    userId: Long,
    paymentStatus: BarrelPaymentStatus,
  ): Boolean

  @Modifying
  @Query(
    """
    update BarrelPurchaseModel p
    set p.paymentStatus = dev.yidafu.aqua.common.domain.model.enums.BarrelPaymentStatus.PAID,
        p.transactionId = :transactionId, p.paidAt = :now, p.updatedAt = :now
    where p.id = :id and p.paymentStatus = dev.yidafu.aqua.common.domain.model.enums.BarrelPaymentStatus.PENDING
    """,
  )
  fun markPaidAtomically(
    @Param("id") id: Long,
    @Param("transactionId") transactionId: String,
    @Param("now") now: LocalDateTime,
  ): Int

  /** 取消该用户未支付(PENDING)旧购桶单(重复发起时幂等化) */
  @Modifying
  @Query(
    """
    update BarrelPurchaseModel p
    set p.paymentStatus = dev.yidafu.aqua.common.domain.model.enums.BarrelPaymentStatus.CANCELLED,
        p.updatedAt = :now
    where p.userId = :userId and p.paymentStatus = dev.yidafu.aqua.common.domain.model.enums.BarrelPaymentStatus.PENDING
    """,
  )
  fun cancelPendingByUser(
    @Param("userId") userId: Long,
    @Param("now") now: LocalDateTime,
  ): Int
}
