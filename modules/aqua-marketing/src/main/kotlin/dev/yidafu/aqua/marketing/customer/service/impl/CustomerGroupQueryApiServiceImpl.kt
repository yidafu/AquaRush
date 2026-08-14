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

package dev.yidafu.aqua.marketing.customer.service.impl

import dev.yidafu.aqua.api.service.marketing.CustomerGroupQueryApiService
import dev.yidafu.aqua.common.domain.model.CustomerGroupModel
import dev.yidafu.aqua.marketing.customer.domain.repository.CustomerGroupRepository
import org.springframework.stereotype.Service

/**
 * 客户分组跨模块查询服务实现(用户列表展示、定向发放、营销引擎)
 */
@Service
class CustomerGroupQueryApiServiceImpl(
  private val customerGroupRepository: CustomerGroupRepository,
) : CustomerGroupQueryApiService {
  override fun findById(id: Long): CustomerGroupModel? = customerGroupRepository.findById(id).orElse(null)

  override fun findByIds(ids: List<Long>): List<CustomerGroupModel> = customerGroupRepository.findAllById(ids)

  override fun findAll(): List<CustomerGroupModel> = customerGroupRepository.findAll()
}
