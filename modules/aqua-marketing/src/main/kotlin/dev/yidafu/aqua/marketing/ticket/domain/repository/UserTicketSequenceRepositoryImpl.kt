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

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository

/**
 * 水票票号按月取号实现(F-MKT-007,v2.2.0 迁移 ticket_no_sequences):
 * PostgreSQL 原生 INSERT ... ON CONFLICT DO UPDATE ... RETURNING 原子取号,
 * 显式指定结果类型 Long,避免标量映射歧义。
 */
@Repository
class UserTicketSequenceRepositoryImpl : UserTicketSequenceRepository {
  @PersistenceContext
  private lateinit var entityManager: EntityManager

  override fun nextTicketSeq(monthKey: String): Long {
    val query =
      entityManager.createNativeQuery(
        """
        insert into ticket_no_sequences (month_key, last_seq)
        values (:monthKey, 1)
        on conflict (month_key) do update set last_seq = ticket_no_sequences.last_seq + 1
        returning last_seq
        """.trimIndent(),
        Long::class.java,
      )
    query.setParameter("monthKey", monthKey)
    @Suppress("UNCHECKED_CAST")
    return (query.singleResult as Number).toLong()
  }
}
