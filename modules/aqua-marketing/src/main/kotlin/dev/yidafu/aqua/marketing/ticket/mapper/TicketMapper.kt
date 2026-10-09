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

package dev.yidafu.aqua.marketing.ticket.mapper

import dev.yidafu.aqua.common.domain.model.TicketTypeModel
import dev.yidafu.aqua.common.graphql.generated.TicketType
import dev.yidafu.aqua.common.graphql.generated.TicketTypeKind
import dev.yidafu.aqua.common.graphql.generated.TicketTypeStatus
import dev.yidafu.aqua.common.graphql.generated.ValidityType
import dev.yidafu.aqua.common.domain.model.enums.TicketTypeKind as DomainTicketTypeKind
import dev.yidafu.aqua.common.domain.model.enums.TicketTypeStatus as DomainTicketTypeStatus
import dev.yidafu.aqua.common.domain.model.enums.ValidityType as DomainValidityType

/**
 * 桶水票票种映射器:域实体 → GraphQL 类型(PRD 10-marketing.md §F-MKT-007)
 */
fun TicketTypeModel.toGraphQL(): TicketType =
  TicketType(
    id = id ?: 0L,
    name = name,
    ticketType = ticketType.toGraphQL(),
    productId = productId,
    specification = specification,
    packageCount = packageCount,
    packagePriceCents = packagePriceCents,
    giftCount = giftCount,
    validityType = validityType.toGraphQL(),
    validFrom = validFrom,
    validTo = validTo,
    validDays = validDays,
    perUserLimit = perUserLimit,
    status = status.toGraphQL(),
    createdAt = createdAt,
    updatedAt = updatedAt,
  )

/**
 * 票种类型:领域枚举 ↔ GraphQL 枚举同名 valueOf 互转
 */
fun DomainTicketTypeKind.toGraphQL(): TicketTypeKind = TicketTypeKind.valueOf(name)

fun TicketTypeKind.toDomain(): DomainTicketTypeKind = DomainTicketTypeKind.valueOf(name)

/**
 * 票种状态:领域枚举 ↔ GraphQL 枚举同名 valueOf 互转
 */
fun DomainTicketTypeStatus.toGraphQL(): TicketTypeStatus = TicketTypeStatus.valueOf(name)

fun TicketTypeStatus.toDomain(): DomainTicketTypeStatus = DomainTicketTypeStatus.valueOf(name)

/**
 * 有效期类型:领域枚举 ↔ GraphQL 枚举同名 valueOf 互转
 */
fun DomainValidityType.toGraphQL(): ValidityType = ValidityType.valueOf(name)

fun ValidityType.toDomain(): DomainValidityType = DomainValidityType.valueOf(name)
