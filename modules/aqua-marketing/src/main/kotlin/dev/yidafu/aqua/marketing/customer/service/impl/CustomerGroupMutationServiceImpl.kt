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

import dev.yidafu.aqua.api.dto.CustomerGroupCreateRequest
import dev.yidafu.aqua.api.dto.CustomerGroupUpdateRequest
import dev.yidafu.aqua.api.service.marketing.CustomerGroupMutationService
import dev.yidafu.aqua.api.service.user.UserQueryApiService
import dev.yidafu.aqua.api.service.user.UserService
import dev.yidafu.aqua.common.domain.model.CustomerGroupModel
import dev.yidafu.aqua.marketing.customer.domain.exception.CustomerGroupNotFoundException
import dev.yidafu.aqua.marketing.customer.domain.exception.DuplicateGroupNameException
import dev.yidafu.aqua.marketing.customer.domain.repository.CustomerGroupRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 客户分组变更服务实现(管理后台,F-MKT-001)
 * 名称唯一;删除分组时成员回「未分组」;设置分组即时生效(R-MKT-01 分组维度)。
 */
@Service
class CustomerGroupMutationServiceImpl(
  private val customerGroupRepository: CustomerGroupRepository,
  private val userQueryApiService: UserQueryApiService,
  private val userService: UserService,
) : CustomerGroupMutationService {
  private val logger = LoggerFactory.getLogger(CustomerGroupMutationServiceImpl::class.java)

  @Transactional
  override fun createGroup(
    adminId: Long,
    adminName: String,
    request: CustomerGroupCreateRequest,
  ): CustomerGroupModel {
    if (customerGroupRepository.existsByName(request.name)) {
      throw DuplicateGroupNameException(request.name)
    }
    val group =
      CustomerGroupModel(
        name = request.name,
        description = request.description,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )
    val saved = customerGroupRepository.save(group)
    logger.info("Admin {} created customer group {}: {}", adminId, saved.id, saved.name)
    return saved
  }

  @Transactional
  override fun updateGroup(
    adminId: Long,
    adminName: String,
    id: Long,
    request: CustomerGroupUpdateRequest,
  ): CustomerGroupModel {
    val group = customerGroupRepository.findById(id).orElseThrow { CustomerGroupNotFoundException(id) }
    val existing = customerGroupRepository.findByName(request.name)
    if (existing != null && existing.id != id) {
      throw DuplicateGroupNameException(request.name)
    }
    group.name = request.name
    group.description = request.description
    val saved = customerGroupRepository.save(group)
    logger.info("Admin {} updated customer group {}: {}", adminId, id, saved.name)
    return saved
  }

  @Transactional
  override fun deleteGroup(
    adminId: Long,
    adminName: String,
    id: Long,
  ): Boolean {
    val group = customerGroupRepository.findById(id).orElseThrow { CustomerGroupNotFoundException(id) }
    // 成员回「未分组」(F-MKT-001:删除分组 → 成员回退未分组)
    userQueryApiService
      .findByCustomerGroupId(id)
      .forEach { user ->
        userService.setCustomerGroup(user.id!!, null)
      }
    customerGroupRepository.delete(group)
    logger.info("Admin {} deleted customer group {}: {} ({} members reset to ungrouped)", adminId, id, group.name)
    return true
  }

  @Transactional
  override fun setUserGroup(
    adminId: Long,
    adminName: String,
    userId: Long,
    customerGroupId: Long?,
  ): CustomerGroupModel? {
    if (customerGroupId != null && !customerGroupRepository.existsById(customerGroupId)) {
      throw CustomerGroupNotFoundException(customerGroupId)
    }
    userService.setCustomerGroup(userId, customerGroupId)
    logger.info("Admin {} set user {} to group {}", adminId, userId, customerGroupId ?: "未分组")
    return customerGroupId?.let { customerGroupRepository.findById(it).orElse(null) }
  }
}
