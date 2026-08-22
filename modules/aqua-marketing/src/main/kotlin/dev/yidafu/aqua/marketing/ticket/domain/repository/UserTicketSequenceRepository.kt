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

/**
 * 水票票号按月取号仓库片段(F-MKT-007):
 * 原子取当月下一个递增序号,跨月自动重置(见 v2.2.0 迁移 ticket_no_sequences 表)。
 */
interface UserTicketSequenceRepository {
  /**
   * 原子取号:INSERT ... ON CONFLICT DO UPDATE ... RETURNING,并发安全;
   * 当月首票置 1,其后逐次 +1;随事务提交/回滚(回滚时空号可复用,不产生重复票号)。
   */
  fun nextTicketSeq(monthKey: String): Long
}
