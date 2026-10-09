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

package dev.yidafu.aqua.marketing.barrel.mapper

import dev.yidafu.aqua.common.domain.model.BarrelFeeModel
import dev.yidafu.aqua.common.graphql.generated.BarrelFee
import dev.yidafu.aqua.common.graphql.generated.BarrelFeeStatus
import dev.yidafu.aqua.common.domain.model.enums.BarrelFeeStatus as DomainBarrelFeeStatus

/**
 * 水桶费价目实体 → GraphQL BarrelFee(状态枚举同名 valueOf 互转)
 */
fun BarrelFeeModel.toGraphQL(): BarrelFee =
  BarrelFee(
    id = id ?: 0L,
    specification = specification,
    priceCents = priceCents,
    status = status.toGraphQL(),
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt,
  )

/**
 * 价目状态:领域枚举 → GraphQL 枚举同名 valueOf 互转
 */
fun DomainBarrelFeeStatus.toGraphQL(): BarrelFeeStatus = BarrelFeeStatus.valueOf(name)
