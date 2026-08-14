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

import dev.yidafu.aqua.api.dto.TicketTypeCreateRequest
import dev.yidafu.aqua.api.dto.TicketTypeUpdateRequest
import dev.yidafu.aqua.common.domain.model.TicketTypeModel
import dev.yidafu.aqua.common.graphql.generated.CreateTicketTypeInput
import dev.yidafu.aqua.common.graphql.generated.TicketType
import dev.yidafu.aqua.common.graphql.generated.TicketTypeKind
import dev.yidafu.aqua.common.graphql.generated.TicketTypeStatus
import dev.yidafu.aqua.common.graphql.generated.UpdateTicketTypeInput
import dev.yidafu.aqua.common.graphql.generated.ValidityType
import tech.mappie.api.EnumMappie
import tech.mappie.api.ObjectMappie
import dev.yidafu.aqua.common.domain.model.enums.TicketTypeKind as DomainTicketTypeKind
import dev.yidafu.aqua.common.domain.model.enums.TicketTypeStatus as DomainTicketTypeStatus
import dev.yidafu.aqua.common.domain.model.enums.ValidityType as DomainValidityType

/**
 * 桶水票票种映射器:域实体 → GraphQL 类型(PRD 10-marketing.md §F-MKT-007)
 */
object TicketTypeMapper : ObjectMappie<TicketTypeModel, TicketType>() {
  override fun map(from: TicketTypeModel): TicketType =
    mapping {
      to::id fromValue (from.id ?: 0L)
      to::ticketType fromExpression { TicketTypeKindMapper.map(from.ticketType) }
      to::status fromExpression { TicketTypeStatusMapper.map(from.status) }
      to::validityType fromExpression { ValidityTypeMapper.map(from.validityType) }
    }
}

object TicketTypeKindMapper : EnumMappie<DomainTicketTypeKind, TicketTypeKind>()

object TicketTypeStatusMapper : EnumMappie<DomainTicketTypeStatus, TicketTypeStatus>()

object ValidityTypeMapper : EnumMappie<DomainValidityType, ValidityType>()

object TicketTypeKindInputMapper : EnumMappie<TicketTypeKind, DomainTicketTypeKind>()

object TicketTypeStatusInputMapper : EnumMappie<TicketTypeStatus, DomainTicketTypeStatus>()

object ValidityTypeInputMapper : EnumMappie<ValidityType, DomainValidityType>()

/**
 * 新建票种输入映射器:GraphQL 输入 → 服务请求
 */
object CreateTicketTypeInputMapper : ObjectMappie<CreateTicketTypeInput, TicketTypeCreateRequest>() {
  override fun map(from: CreateTicketTypeInput): TicketTypeCreateRequest =
    mapping {
      to::ticketType fromExpression { TicketTypeKindInputMapper.map(from.ticketType) }
      to::validityType fromExpression { ValidityTypeInputMapper.map(from.validityType) }
    }
}

/**
 * 编辑票种输入映射器
 */
object UpdateTicketTypeInputMapper : ObjectMappie<UpdateTicketTypeInput, TicketTypeUpdateRequest>() {
  override fun map(from: UpdateTicketTypeInput): TicketTypeUpdateRequest =
    mapping {
      to::ticketType fromExpression { TicketTypeKindInputMapper.map(from.ticketType) }
      to::validityType fromExpression { ValidityTypeInputMapper.map(from.validityType) }
    }
}
