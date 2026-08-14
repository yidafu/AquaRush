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

import dev.yidafu.aqua.api.service.marketing.CustomerGroupQueryService
import dev.yidafu.aqua.api.service.user.UserQueryApiService
import dev.yidafu.aqua.common.domain.model.CustomerGroupModel
import dev.yidafu.aqua.marketing.customer.domain.repository.CustomerGroupRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

/**
 * 客户分组查询服务实现(管理后台,F-MKT-001)
 */
@Service
class CustomerGroupQueryServiceImpl(
  private val customerGroupRepository: CustomerGroupRepository,
  private val userQueryApiService: UserQueryApiService,
) : CustomerGroupQueryService {
  override fun listGroups(
    keyword: String?,
    pageable: Pageable,
  ): Page<CustomerGroupModel> =
    if (keyword.isNullOrBlank()) {
      customerGroupRepository.findAll(pageable)
    } else {
      customerGroupRepository.findByNameContainingIgnoreCaseOrderByCreatedAtDesc(keyword, pageable)
    }

  override fun getGroup(id: Long): CustomerGroupModel? = customerGroupRepository.findById(id).orElse(null)

  override fun findByName(name: String): CustomerGroupModel? = customerGroupRepository.findByName(name)

  override fun memberCount(groupId: Long): Long = userQueryApiService.countUsersByCustomerGroupId(groupId)
}
