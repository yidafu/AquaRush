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

import dev.yidafu.aqua.api.dto.SaveDeliveryFeeRuleRequest
import dev.yidafu.aqua.common.domain.model.DeliveryFeeRuleModel
import dev.yidafu.aqua.common.graphql.generated.DeliveryFeeRule
import dev.yidafu.aqua.common.graphql.generated.SaveDeliveryFeeRuleInput
import tech.mappie.api.ObjectMappie

/**
 * 配送费规则映射器:域实体 → GraphQL 类型
 */
object DeliveryFeeRuleMapper : ObjectMappie<DeliveryFeeRuleModel, DeliveryFeeRule>() {
  override fun map(from: DeliveryFeeRuleModel): DeliveryFeeRule =
    mapping {
      to::freeFloorLimit fromValue from.freeFloorLimit
      to::surchargeCents fromValue from.surchargeCents
    }
}

/**
 * 保存配送费规则输入映射器:GraphQL 输入 → 服务请求
 */
object SaveDeliveryFeeRuleInputMapper : ObjectMappie<SaveDeliveryFeeRuleInput, SaveDeliveryFeeRuleRequest>()
