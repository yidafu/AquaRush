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

import dev.yidafu.aqua.common.domain.model.DepositPriceModel
import dev.yidafu.aqua.common.graphql.generated.DepositPriceStatus
import dev.yidafu.aqua.common.graphql.generated.DepositPriceVo
import dev.yidafu.aqua.common.domain.model.enums.DepositPriceStatus as DomainDepositPriceStatus

/**
 * 押金价目实体 → GraphQL DepositPriceVo(状态枚举同名互转;
 * 模型 priceCents 即押金价,GraphQL 字段名 depositCents——表名/列名沿用 barrel_fees.price_cents)。
 */
fun DepositPriceModel.toGraphQL(): DepositPriceVo =
  DepositPriceVo(
    id = id ?: 0L,
    specification = specification,
    depositCents = priceCents,
    status = status.toGraphQL(),
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt,
  )

/**
 * 价目状态:领域枚举 → GraphQL 枚举同名 when 穷举互转
 */
fun DomainDepositPriceStatus.toGraphQL(): DepositPriceStatus =
  when (this) {
    DomainDepositPriceStatus.ENABLED -> DepositPriceStatus.ENABLED
    DomainDepositPriceStatus.DISABLED -> DepositPriceStatus.DISABLED
  }

/**
 * 价目状态:GraphQL 枚举 → 领域枚举
 */
fun DepositPriceStatus.toDomain(): DomainDepositPriceStatus =
  when (this) {
    DepositPriceStatus.ENABLED -> DomainDepositPriceStatus.ENABLED
    DepositPriceStatus.DISABLED -> DomainDepositPriceStatus.DISABLED
  }
