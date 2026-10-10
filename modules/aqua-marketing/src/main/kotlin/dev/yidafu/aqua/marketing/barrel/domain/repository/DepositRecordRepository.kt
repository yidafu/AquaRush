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

import dev.yidafu.aqua.common.domain.model.DepositRecordModel
import dev.yidafu.aqua.common.domain.model.enums.DepositStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * 水桶押金记录仓储(R-MON-03 押金账本)。
 * 查询过滤 is_deleted;状态流转走状态谓词条件更新(受影响行数=0 即幂等失败)。
 */
interface DepositRecordRepository {
  fun findByOrderIdAndStatus(
    orderId: Long,
    status: DepositStatus,
  ): List<DepositRecordModel>

  /** 同用户同规格全部有效记录(收取前判「在租只收一次」与退押覆盖用)。 */
  fun findByUserIdAndSpecification(
    userId: Long,
    specification: String,
  ): List<DepositRecordModel>

  /** 用户押金记录分页(水桶账户「押金记录」,status 可选)。 */
  fun pageByUserId(
    userId: Long,
    status: DepositStatus?,
    pageable: Pageable,
  ): Page<DepositRecordModel>

  /** 后台押金记录检索(用户/状态/规格可选过滤,强制分页)。 */
  fun search(
    userId: Long?,
    status: DepositStatus?,
    specification: String?,
    pageable: Pageable,
  ): Page<DepositRecordModel>

  /** PENDING→HELD(支付成功实收确认,条件更新;返回受影响行数)。 */
  fun markHeldIfPending(ids: List<Long>): Int

  /** HELD→REFUNDED(退押/随单退款,条件更新幂等防并发重复退还;返回受影响行数)。 */
  fun markRefundedIfHeld(
    ids: List<Long>,
    refundedBy: Long?,
    remark: String?,
  ): Int

  /** 软删订单全部 PENDING 记录(取消/支付超时清账;返回受影响行数)。 */
  fun softDeletePendingByOrderId(orderId: Long): Int

  /** 批量 upsert(单事务原子)。 */
  fun saveAll(entities: List<DepositRecordModel>): List<DepositRecordModel>
}
