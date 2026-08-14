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

package dev.yidafu.aqua.marketing.ticket.service.impl

import dev.yidafu.aqua.api.service.marketing.TicketTypeQueryService
import dev.yidafu.aqua.common.domain.model.TicketTypeModel
import dev.yidafu.aqua.common.domain.model.enums.TicketTypeStatus
import dev.yidafu.aqua.marketing.ticket.domain.repository.TicketTypeRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service

/**
 * 桶水票票种查询服务实现(管理后台,F-MKT-007)
 */
@Service
class TicketTypeQueryServiceImpl(
  private val ticketTypeRepository: TicketTypeRepository,
) : TicketTypeQueryService {
  override fun listTicketTypes(
    keyword: String?,
    status: TicketTypeStatus?,
    pageable: Pageable,
  ): Page<TicketTypeModel> {
    val spec =
      Specification<TicketTypeModel> { root, _, cb ->
        val predicates = mutableListOf<jakarta.persistence.criteria.Predicate>()
        if (!keyword.isNullOrBlank()) {
          predicates.add(cb.like(cb.lower(root.get("name")), "%${keyword.lowercase()}%"))
        }
        if (status != null) {
          predicates.add(cb.equal(root.get<Any>("status"), status))
        }
        cb.and(*predicates.toTypedArray())
      }
    return ticketTypeRepository.findAll(spec, pageable)
  }

  override fun getTicketType(id: Long): TicketTypeModel? = ticketTypeRepository.findById(id).orElse(null)
}
