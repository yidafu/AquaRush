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

import dev.yidafu.aqua.common.domain.model.UserTicketModel
import dev.yidafu.aqua.common.domain.model.enums.UserTicketStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * 用户水票实例仓库(F-MKT-007,每票一行):
 * 核销/退回用原子条件 UPDATE(防并发重复核销,核销幂等 R-MKT-08)。
 * 原手写 @Query 方法已迁移至 QueryDSL,见 [UserTicketRepositoryCustom]/[UserTicketRepositoryImpl]。
 */
@Repository
interface UserTicketRepository :
  JpaRepository<UserTicketModel, Long>,
  JpaSpecificationExecutor<UserTicketModel>,
  UserTicketRepositoryCustom,
  UserTicketSequenceRepository {
  fun findByUserId(userId: Long): List<UserTicketModel>

  fun findByUserIdAndStatus(
    userId: Long,
    status: UserTicketStatus,
  ): List<UserTicketModel>

  fun findByUserIdAndStatusIn(
    userId: Long,
    statuses: List<UserTicketStatus>,
  ): List<UserTicketModel>

  fun findByUsedOrderId(orderId: Long): List<UserTicketModel>

  fun findByPurchaseId(purchaseId: Long): List<UserTicketModel>

  /**
   * 当前可用张数(UNUSED + 未过期 RETURNED),后台水票统计「当前可用」口径。
   */
  fun countByTicketTypeIdAndStatusInAndExpireTimeGreaterThan(
    ticketTypeId: Long,
    statuses: List<UserTicketStatus>,
    expireTime: LocalDateTime,
  ): Long
}
