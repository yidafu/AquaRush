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

import dev.yidafu.aqua.api.service.marketing.CustomerGroupQueryService
import dev.yidafu.aqua.common.graphql.generated.CustomerGroupListInput
import dev.yidafu.aqua.common.graphql.generated.CustomerGroupVo
import dev.yidafu.aqua.common.graphql.generated.CustomerGroupVoPage
import dev.yidafu.aqua.marketing.customer.mapper.toGraphQL
import org.springframework.data.domain.PageRequest
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

/**
 * 客户分组查询 Resolver(管理后台,PERMISSION_MKT_READ)
 * 与 PRD 10-marketing.md §F-MKT-001 保持一致。
 */
@Controller
class AdminCustomerGroupQueryResolver(
  private val customerGroupQueryService: CustomerGroupQueryService,
) {
  /**
   * 分页查询客户分组列表(附成员数)
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_MKT_READ')")
  fun customerGroups(
    @Argument input: CustomerGroupListInput?,
  ): CustomerGroupVoPage {
    val page =
      customerGroupQueryService.listGroups(
        keyword = input?.keyword,
        pageable = PageRequest.of(input?.page ?: 0, input?.size ?: 20),
      )
    return CustomerGroupVoPage(
      content =
        page.content.map { group ->
          group.toGraphQL(customerGroupQueryService.memberCount(group.id ?: 0L))
        },
      number = page.number,
      size = page.size,
      totalElements = page.totalElements.toInt(),
      totalPages = page.totalPages,
    )
  }

  /**
   * 根据 ID 查询客户分组
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_MKT_READ')")
  fun customerGroup(
    @Argument id: Long,
  ): CustomerGroupVo? {
    val group =
      customerGroupQueryService.getGroup(id) ?: throw IllegalArgumentException("客户分组不存在")
    return group.toGraphQL(customerGroupQueryService.memberCount(id))
  }
}
