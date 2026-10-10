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
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime
import java.util.Optional

/**
 * 水桶购买单仓储(G06:一次性购桶,独立支付,每用户总限购一次;支付入账原子流转 PENDING→PAID,幂等)。
 * JPA→Exposed 迁移 P4:剥离 JpaRepository/JpaSpecificationExecutor,仅保留实际被调用的方法;
 * 查询过滤 is_deleted,delete 软删;支付入账/取消为原子条件 UPDATE。
 */
interface BarrelPurchaseRepository {
  fun findByOrderNo(orderNo: String): BarrelPurchaseModel?

  fun findFirstByUserIdOrderByCreatedAtDesc(userId: Long): BarrelPurchaseModel?

  fun existsByUserIdAndPaymentStatus(
    userId: Long,
    paymentStatus: BarrelPaymentStatus,
  ): Boolean

  fun findById(id: Long): Optional<BarrelPurchaseModel>

  /** 后台购桶单分页:paymentStatus 精确 + paidAt [from, to] 区间;固定 createdAt 降序、id 破平。 */
  fun searchPurchases(
    status: BarrelPaymentStatus?,
    dateFrom: LocalDateTime?,
    dateTo: LocalDateTime?,
    pageable: Pageable,
  ): Page<BarrelPurchaseModel>

  /** upsert 语义:无 id 或 id 不存在时插入(雪花 id),否则按 id 更新业务列。 */
  fun save(entity: BarrelPurchaseModel): BarrelPurchaseModel

  /**
   * 支付入账原子流转(PENDING→PAID,幂等):返回受影响行数(1=成功,0=已被处理/不存在)。
   */
  fun markPaidAtomically(
    id: Long,
    transactionId: String,
    now: LocalDateTime,
  ): Int

  /**
   * 取消该用户未支付(PENDING)旧购桶单(重复发起时幂等化):返回受影响行数。
   */
  fun cancelPendingByUser(
    userId: Long,
    now: LocalDateTime,
  ): Int
}
