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
import java.time.LocalDateTime
import java.util.Optional

/**
 * 用户水票实例仓库(F-MKT-007,每票一行):
 * 核销/退回用原子条件 UPDATE(防并发重复核销,核销幂等 R-MKT-08)。
 * JPA→Exposed 迁移 P4:剥离 JpaRepository/JpaSpecificationExecutor,仅保留实际被调用的方法;
 * 查询过滤 is_deleted,delete 软删;使用统计/原子条件 UPDATE 见 [UserTicketRepositoryCustom],
 * 按月取号见 [UserTicketSequenceRepository]。
 */
interface UserTicketRepository :
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

  /**
   * 当前可用张数(UNUSED + 未过期 RETURNED),后台水票统计「当前可用」口径。
   */
  fun countByTicketTypeIdAndStatusInAndExpireTimeGreaterThan(
    ticketTypeId: Long,
    statuses: List<UserTicketStatus>,
    expireTime: LocalDateTime,
  ): Long

  fun findById(id: Long): Optional<UserTicketModel>

  /** upsert 语义:无 id 或 id 不存在时插入(雪花 id),否则按 id 更新业务列。 */
  fun save(entity: UserTicketModel): UserTicketModel
}
