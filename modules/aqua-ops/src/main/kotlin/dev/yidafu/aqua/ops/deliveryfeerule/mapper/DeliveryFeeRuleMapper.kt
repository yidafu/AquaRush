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

package dev.yidafu.aqua.ops.deliveryfeerule.mapper

import dev.yidafu.aqua.common.domain.model.DeliveryFeeRuleModel
import dev.yidafu.aqua.common.graphql.generated.DeliveryDistanceTierVo
import dev.yidafu.aqua.common.graphql.generated.DeliveryFeeRuleVo
import dev.yidafu.aqua.common.graphql.generated.DeliveryFeeTierVo

/**
 * 配送费规则映射器:域实体 → GraphQL 类型
 * 金额字段两侧均为分制 Long,直接赋值无需转换。
 */
fun DeliveryFeeRuleModel.toGraphQL(): DeliveryFeeRuleVo =
  DeliveryFeeRuleVo(
    freeFloorLimit = freeFloorLimit,
    surchargeCents = surchargeCents,
    tiers =
      tiers?.map { tier ->
        DeliveryFeeTierVo(
          fromFloor = tier.fromFloor,
          toFloor = tier.toFloor,
          feeCents = tier.feeCents,
        )
      },
    minimumOrderCents = minimumOrderCents,
    distanceTiers =
      distanceTiers?.map { tier ->
        DeliveryDistanceTierVo(
          upToKm = tier.upToKm?.toFloat(),
          feeCents = tier.feeCents,
        )
      },
    storeLongitude = storeLongitude?.toFloat(),
    storeLatitude = storeLatitude?.toFloat(),
    updatedAt = updatedAt,
  )
