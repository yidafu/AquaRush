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

package dev.yidafu.aqua.marketing.customer.mapper

import dev.yidafu.aqua.api.dto.CustomerGroupCreateRequest
import dev.yidafu.aqua.api.dto.CustomerGroupUpdateRequest
import dev.yidafu.aqua.common.domain.model.CustomerGroupModel
import dev.yidafu.aqua.common.graphql.generated.CreateCustomerGroupInput
import dev.yidafu.aqua.common.graphql.generated.CustomerGroup
import dev.yidafu.aqua.common.graphql.generated.UpdateCustomerGroupInput
import tech.mappie.api.ObjectMappie

/**
 * 客户分组映射器:域实体 → GraphQL 类型(成员数由服务统计后传入,PRD 10-marketing.md §F-MKT-001)
 */
object CustomerGroupMapper {
  fun toGraphQL(
    model: CustomerGroupModel,
    memberCount: Long,
  ): CustomerGroup =
    CustomerGroup(
      id = model.id ?: 0L,
      name = model.name,
      description = model.description,
      memberCount = memberCount.toInt(),
      createdAt = model.createdAt,
      updatedAt = model.updatedAt,
    )
}

/**
 * 新建客户分组输入映射器:GraphQL 输入 → 服务请求
 */
object CreateCustomerGroupInputMapper : ObjectMappie<CreateCustomerGroupInput, CustomerGroupCreateRequest>()

/**
 * 编辑客户分组输入映射器
 */
object UpdateCustomerGroupInputMapper : ObjectMappie<UpdateCustomerGroupInput, CustomerGroupUpdateRequest>()
