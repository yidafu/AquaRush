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

import dev.yidafu.aqua.common.domain.model.CustomerGroupModel
import dev.yidafu.aqua.common.graphql.generated.CustomerGroupVo

/**
 * 客户分组映射器:域实体 → GraphQL 类型(成员数由服务统计后传入,PRD 10-marketing.md §F-MKT-001)
 */
fun CustomerGroupModel.toGraphQL(memberCount: Long): CustomerGroupVo =
  CustomerGroupVo(
    id = id ?: 0L,
    name = name,
    description = description,
    memberCount = memberCount.toInt(),
    createdAt = createdAt,
    updatedAt = updatedAt,
  )
