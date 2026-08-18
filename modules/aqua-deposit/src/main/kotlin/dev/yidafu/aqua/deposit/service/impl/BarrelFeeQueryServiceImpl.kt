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

package dev.yidafu.aqua.deposit.service.impl

import dev.yidafu.aqua.api.service.barrel.BarrelFeeQueryService
import dev.yidafu.aqua.common.domain.model.BarrelFeeModel
import dev.yidafu.aqua.common.domain.model.enums.BarrelFeeStatus
import dev.yidafu.aqua.deposit.domain.repository.BarrelFeeRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service

/**
 * 水桶费价目查询服务实现(G06:用户端买水桶页 + 后台价目管理分页)
 */
@Service
class BarrelFeeQueryServiceImpl(
  private val barrelFeeRepository: BarrelFeeRepository,
) : BarrelFeeQueryService {
  override fun listEnabled(): List<BarrelFeeModel> = barrelFeeRepository.findAllByStatusOrderBySortOrderAscIdAsc(BarrelFeeStatus.ENABLED)

  override fun list(
    status: BarrelFeeStatus?,
    keyword: String?,
    page: Int,
    size: Int,
  ): Page<BarrelFeeModel> {
    val spec =
      Specification<BarrelFeeModel> { root, _, cb ->
        val predicates = mutableListOf<jakarta.persistence.criteria.Predicate>()
        status?.let { predicates.add(cb.equal(root.get<BarrelFeeStatus>("status"), it)) }
        keyword?.takeIf { it.isNotBlank() }?.let {
          predicates.add(cb.like(cb.lower(root.get<String>("specification")), "%${it.lowercase()}%"))
        }
        cb.and(*predicates.toTypedArray())
      }
    return barrelFeeRepository.findAll(
      spec,
      PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "sortOrder")),
    )
  }
}
