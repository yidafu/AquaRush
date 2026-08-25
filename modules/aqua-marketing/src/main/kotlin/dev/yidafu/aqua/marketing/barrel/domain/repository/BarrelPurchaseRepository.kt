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

package dev.yidafu.aqua.marketing.barrel.domain.repository

import dev.yidafu.aqua.common.domain.model.BarrelPurchaseModel
import dev.yidafu.aqua.common.domain.model.enums.BarrelPaymentStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

/**
 * 水桶购买单仓储(G06:一次性购桶,独立支付,每用户总限购一次;支付入账原子流转 PENDING→PAID,幂等)
 * 原手写 @Query 方法已迁移至 QueryDSL,见 [BarrelPurchaseRepositoryCustom]/[BarrelPurchaseRepositoryImpl]。
 */
@Repository
interface BarrelPurchaseRepository :
  JpaRepository<BarrelPurchaseModel, Long>,
  JpaSpecificationExecutor<BarrelPurchaseModel>,
  BarrelPurchaseRepositoryCustom {
  fun findByOrderNo(orderNo: String): BarrelPurchaseModel?

  fun findFirstByUserIdOrderByCreatedAtDesc(userId: Long): BarrelPurchaseModel?

  fun existsByUserIdAndPaymentStatus(
    userId: Long,
    paymentStatus: BarrelPaymentStatus,
  ): Boolean
}
