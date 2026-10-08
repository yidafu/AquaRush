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

package dev.yidafu.aqua.delivery.mapper

import dev.yidafu.aqua.common.domain.model.DeliveryTrajectoryModel
import dev.yidafu.aqua.common.graphql.generated.DeliveryTrajectory
import tech.mappie.api.ObjectMappie

/**
 * 配送轨迹点模型 → GraphQL 类型映射(F-ADR-012,R-ADR-03)
 *
 * 数据库 double 精度坐标投影为 GraphQL Float(与 Address 投影一致)。
 */
object DeliveryTrajectoryMapper : ObjectMappie<DeliveryTrajectoryModel, DeliveryTrajectory>() {
  override fun map(from: DeliveryTrajectoryModel): DeliveryTrajectory =
    mapping {
      to::id fromValue (from.id ?: 0L)
      to::deliveryWorkerId fromProperty from::deliveryWorkerId
      to::orderId fromProperty from::orderId
      to::longitude fromExpression { from.longitude.toFloat() }
      to::latitude fromExpression { from.latitude.toFloat() }
      to::accuracy fromExpression { from.accuracy?.toFloat() }
      to::reportedAt fromProperty from::reportedAt
      to::createdAt fromProperty from::createdAt
    }
}
