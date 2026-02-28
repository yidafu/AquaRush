/*
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

import dev.yidafu.aqua.common.domain.model.DeliverWorkerModelStatus
import dev.yidafu.aqua.common.domain.model.DeliveryAreaModel
import dev.yidafu.aqua.common.domain.model.DeliveryWorkerModel
import dev.yidafu.aqua.common.graphql.generated.DeliveryArea
import dev.yidafu.aqua.common.graphql.generated.DeliveryWorker
import dev.yidafu.aqua.common.graphql.generated.DeliveryWorkerStatus
import tech.mappie.api.EnumMappie
import tech.mappie.api.ObjectMappie

/**
 * Mapper for converting DeliveryWorkerModel domain entity to GraphQL DeliveryWorker type
 */
object DeliveryWorkerMapper : ObjectMappie<DeliveryWorkerModel, DeliveryWorker>() {
  override fun map(from: DeliveryWorkerModel): DeliveryWorker = mapping {
    to::id fromValue (from.id ?: 0L)
    to::userId fromProperty from::userId
    to::wechatOpenId fromProperty from::wechatOpenId
    to::name fromProperty from::name
    to::phone fromProperty from::phone
    to::avatarUrl fromProperty from::avatarUrl
    to::onlineStatus fromExpression {
      DeliveryWorkerStatus.valueOf(from.onlineStatus.name)
    }
    to::coordinates fromProperty from::coordinates
    to::currentLocation fromProperty from::currentLocation
    to::totalOrders fromProperty from::totalOrders
    to::completedOrders fromProperty from::completedOrders
    to::rating fromProperty from::rating
    to::averageRating fromProperty from::averageRating
    to::earning fromProperty from::earningCents
    to::isAvailable fromProperty from::isAvailable
    to::createdAt fromProperty from::createdAt
    to::updatedAt fromProperty from::updatedAt
  }
}

/**
 * Mapper for converting DeliveryWorkerModel domain entity to GraphQL DeliveryWorker type (with current user)
 */
object DeliveryWorkerWithCurrentUserMapper : ObjectMappie<DeliveryWorkerModel, DeliveryWorker>() {
  override fun map(from: DeliveryWorkerModel): DeliveryWorker = mapping {
    to::id fromValue (from.id ?: 0L)
    to::userId fromProperty from::userId
    to::wechatOpenId fromProperty from::wechatOpenId
    to::name fromProperty from::name
    to::phone fromProperty from::phone
    to::avatarUrl fromProperty from::avatarUrl
    to::onlineStatus fromExpression {
      DeliveryWorkerStatus.valueOf(from.onlineStatus.name)
    }
    to::coordinates fromProperty from::coordinates
    to::currentLocation fromProperty from::currentLocation
    to::totalOrders fromProperty from::totalOrders
    to::completedOrders fromProperty from::completedOrders
    to::rating fromProperty from::rating
    to::averageRating fromProperty from::averageRating
    to::earning fromProperty from::earningCents
    to::isAvailable fromProperty from::isAvailable
    to::createdAt fromProperty from::createdAt
    to::updatedAt fromProperty from::updatedAt
  }
}

/**
 * Mapper for converting DeliveryAreaModel domain entity to GraphQL DeliveryArea type
 */
object DeliveryAreaMapper : ObjectMappie<DeliveryAreaModel, DeliveryArea>() {
  override fun map(from: DeliveryAreaModel): DeliveryArea = mapping {
    to::id fromProperty from::id
    to::name fromProperty from::name
    to::province fromProperty from::province
    to::city fromProperty from::city
    to::district fromProperty from::district
    to::enabled fromProperty from::enabled
  }
}

/**
 * Enum mapper for WorkerStatus domain enum to GraphQL WorkerStatus enum
 */
object DeliveryWorkerStatusMapper : EnumMappie<DeliverWorkerModelStatus, DeliveryWorkerStatus>()
