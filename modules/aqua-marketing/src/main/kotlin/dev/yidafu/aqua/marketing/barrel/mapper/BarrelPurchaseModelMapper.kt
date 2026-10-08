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

import dev.yidafu.aqua.common.domain.model.BarrelPurchaseItemModel
import dev.yidafu.aqua.common.domain.model.BarrelPurchaseModel
import dev.yidafu.aqua.common.graphql.generated.BarrelPaymentStatus
import dev.yidafu.aqua.common.graphql.generated.BarrelPurchase
import dev.yidafu.aqua.common.graphql.generated.BarrelPurchaseItem
import dev.yidafu.aqua.common.graphql.generated.BarrelPurchaseItemVo
import dev.yidafu.aqua.common.graphql.generated.BarrelPurchaseVo

/**
 * 购桶单实体 → GraphQL BarrelPurchase(管理端)/BarrelPurchaseVO(用户端);
 * 明细来自 barrel_purchase_items,由调用方注入(状态枚举同名值由 Mappie 自动互转)。
 */
object BarrelPurchaseModelMapper {
  fun map(
    model: BarrelPurchaseModel,
    items: List<BarrelPurchaseItemModel>,
  ): BarrelPurchase =
    BarrelPurchase(
      id = model.id ?: 0L,
      userId = model.userId,
      orderNo = model.orderNo,
      totalAmountCents = model.totalAmountCents,
      paymentStatus = BarrelPaymentStatus.valueOf(model.paymentStatus.name),
      paidAt = model.paidAt,
      refundedAt = model.refundedAt,
      refundedBy = model.refundedBy,
      refundRemark = model.refundRemark,
      remark = model.remark,
      items = items.map { mapItem(it) },
      createdAt = model.createdAt,
      updatedAt = model.updatedAt,
    )

  fun mapToVo(
    model: BarrelPurchaseModel,
    items: List<BarrelPurchaseItemModel>,
    requiresSimulation: Boolean,
    paymentParams: tools.jackson.databind.node.ObjectNode?,
  ): BarrelPurchaseVo =
    BarrelPurchaseVo(
      id = model.id ?: 0L,
      orderNo = model.orderNo,
      totalAmountCents = model.totalAmountCents,
      paymentStatus = BarrelPaymentStatus.valueOf(model.paymentStatus.name),
      paidAt = model.paidAt,
      refundedAt = model.refundedAt,
      items = items.map { mapItemVo(it) },
      createdAt = model.createdAt,
      requiresSimulation = requiresSimulation,
      paymentParams = paymentParams,
    )

  private fun mapItem(model: BarrelPurchaseItemModel): BarrelPurchaseItem =
    BarrelPurchaseItem(
      specification = model.specification,
      unitPriceCents = model.unitPriceCents,
      quantity = model.quantity,
      amountCents = model.amountCents,
    )

  private fun mapItemVo(model: BarrelPurchaseItemModel): BarrelPurchaseItemVo =
    BarrelPurchaseItemVo(
      specification = model.specification,
      unitPriceCents = model.unitPriceCents,
      quantity = model.quantity,
      amountCents = model.amountCents,
    )
}
