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

import java.time.LocalDateTime

/**
 * 水桶购买单仓储 QueryDSL 自定义查询(BarrelPurchaseRepository 中手写 @Query 迁移至此)。
 */
interface BarrelPurchaseRepositoryCustom {
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
