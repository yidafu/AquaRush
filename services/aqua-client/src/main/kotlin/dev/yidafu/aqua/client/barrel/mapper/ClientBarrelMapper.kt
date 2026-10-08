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

package dev.yidafu.aqua.client.barrel.mapper

import dev.yidafu.aqua.common.domain.model.BarrelFeeModel
import dev.yidafu.aqua.common.domain.model.BarrelPurchaseItemModel
import dev.yidafu.aqua.common.domain.model.BarrelPurchaseModel
import dev.yidafu.aqua.common.graphql.generated.BarrelFeeVo
import dev.yidafu.aqua.common.graphql.generated.BarrelPaymentStatus
import dev.yidafu.aqua.common.graphql.generated.BarrelPurchaseItemVo
import dev.yidafu.aqua.common.graphql.generated.BarrelPurchaseVo
import tools.jackson.databind.node.ObjectNode

/**
 * 水桶费价目/购桶单实体 → 用户端 GraphQL VO(金额分、状态枚举同名值互转)
 */
fun BarrelFeeModel.toGraphQL(): BarrelFeeVo =
  BarrelFeeVo(
    id = id ?: 0L,
    specification = specification,
    priceCents = priceCents,
  )

fun BarrelPurchaseModel.toGraphQL(
  items: List<BarrelPurchaseItemModel>,
  requiresSimulation: Boolean,
  paymentParams: ObjectNode?,
): BarrelPurchaseVo =
  BarrelPurchaseVo(
    id = id ?: 0L,
    orderNo = orderNo,
    totalAmountCents = totalAmountCents,
    paymentStatus = BarrelPaymentStatus.valueOf(paymentStatus.name),
    paidAt = paidAt,
    refundedAt = refundedAt,
    items =
      items.map {
        BarrelPurchaseItemVo(
          specification = it.specification,
          unitPriceCents = it.unitPriceCents,
          quantity = it.quantity,
          amountCents = it.amountCents,
        )
      },
    createdAt = createdAt,
    requiresSimulation = requiresSimulation,
    paymentParams = paymentParams,
  )
