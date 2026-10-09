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

package dev.yidafu.aqua.admin.marketing.resolvers

import dev.yidafu.aqua.api.service.marketing.CustomerGroupMutationService
import dev.yidafu.aqua.api.service.marketing.CustomerGroupQueryService
import dev.yidafu.aqua.common.graphql.generated.CreateCustomerGroupInput
import dev.yidafu.aqua.common.graphql.generated.CustomerGroup
import dev.yidafu.aqua.common.graphql.generated.UpdateCustomerGroupInput
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.marketing.customer.mapper.toGraphQL
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

/**
 * 客户分组变更 Resolver(管理后台,PERMISSION_MKT_WRITE)
 * 操作人取自登录主体,由服务落审计日志(PRD 10-marketing.md §F-MKT-001)。
 */
@Controller
class AdminCustomerGroupMutationResolver(
  private val customerGroupMutationService: CustomerGroupMutationService,
  private val customerGroupQueryService: CustomerGroupQueryService,
) {
  private val logger = LoggerFactory.getLogger(AdminCustomerGroupMutationResolver::class.java)

  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_MKT_WRITE')")
  fun createCustomerGroup(
    @Argument @Valid input: CreateCustomerGroupInput,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): CustomerGroup {
    logger.info("Admin {} creating customer group: {}", principal.username, input.name)
    val group =
      customerGroupMutationService.createGroup(
        adminId = principal.id,
        adminName = principal.username,
        input = input,
      )
    return group.toGraphQL(customerGroupQueryService.memberCount(group.id ?: 0L))
  }

  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_MKT_WRITE')")
  fun updateCustomerGroup(
    @Argument id: Long,
    @Argument @Valid input: UpdateCustomerGroupInput,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): CustomerGroup {
    logger.info("Admin {} updating customer group {}", principal.username, id)
    val group =
      customerGroupMutationService.updateGroup(
        adminId = principal.id,
        adminName = principal.username,
        id = id,
        input = input,
      )
    return group.toGraphQL(customerGroupQueryService.memberCount(id))
  }

  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_MKT_WRITE')")
  fun deleteCustomerGroup(
    @Argument id: Long,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): Boolean {
    logger.info("Admin {} deleting customer group {}", principal.username, id)
    return customerGroupMutationService.deleteGroup(principal.id, principal.username, id)
  }

  /**
   * 设置用户所属分组(customerGroupId=null 回「未分组」)
   */
  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_MKT_WRITE')")
  fun setUserCustomerGroup(
    @Argument userId: Long,
    @Argument customerGroupId: Long?,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): Boolean {
    logger.info("Admin {} setting user {} customer group {}", principal.username, userId, customerGroupId)
    customerGroupMutationService.setUserGroup(principal.id, principal.username, userId, customerGroupId)
    return true
  }
}
