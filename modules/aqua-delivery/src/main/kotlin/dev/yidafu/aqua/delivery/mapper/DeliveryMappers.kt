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

import dev.yidafu.aqua.common.domain.model.DeliveryWorkerModel
import dev.yidafu.aqua.common.graphql.generated.DeliveryArea
import dev.yidafu.aqua.common.graphql.generated.DeliveryWorker
import dev.yidafu.aqua.common.graphql.generated.DeliveryWorkerStatus
import tech.mappie.api.EnumMappie
import tech.mappie.api.ObjectMappie
import dev.yidafu.aqua.common.domain.model.DeliverWorkerModelStatus
import dev.yidafu.aqua.common.domain.model.DeliveryAreaModel

/**
 * Mapper for converting DeliveryWorkerModel domain entity to GraphQL DeliveryWorker type
 */
object DeliveryWorkerMapper : ObjectMappie<DeliveryWorkerModel, DeliveryWorker>() {
  override fun map(from: DeliveryWorkerModel) =
    mapping {
      to::id fromValue (from.id ?: -1L)
      to::onlineStatus fromValue DeliveryWorkerModelStatusMapper.map(from.onlineStatus)
      to::earning fromValue from.earningCents

    }
}

/**
 * Mapper for converting DeliveryWorkerModel domain entity to GraphQL DeliveryWorker type (with current user)
 */
object DeliveryWorkerWithCurrentUserMapper : ObjectMappie<DeliveryWorkerModel, DeliveryWorker>() {
  override fun map(from: DeliveryWorkerModel) =
    mapping {
      to::id fromValue (from.id ?: -1L)
      to::onlineStatus fromValue DeliveryWorkerModelStatusMapper.map(from.onlineStatus)
      to::earning fromValue from.earningCents
      // Note: isAvailable maps automatically
    }
}

/**
 * Mapper for converting DeliveryAreaModel domain entity to GraphQL DeliveryArea type
 */
object DeliveryAreaMapper : ObjectMappie<DeliveryAreaModel, DeliveryArea>() {
  override fun map(from: DeliveryAreaModel) =
    mapping {
      // Most fields map automatically by name
      // No custom mapping needed as field names match
    }
}

/**
 * Enum mapper for WorkerStatus domain enum to GraphQL WorkerStatus enum
 */
object DeliveryWorkerModelStatusMapper : EnumMappie<DeliverWorkerModelStatus, DeliveryWorkerStatus>() {
  override fun map(from: DeliverWorkerModelStatus) =
    mapping {
      DeliveryWorkerStatus.ONLINE fromEnumEntry DeliverWorkerModelStatus.ONLINE
      DeliveryWorkerStatus.OFFLINE fromEnumEntry DeliverWorkerModelStatus.OFFLINE
    }
}

object DeliveryWorkerStatusMapper : EnumMappie<DeliveryWorkerStatus, DeliverWorkerModelStatus >() {
  override fun map(from: DeliveryWorkerStatus) =
    mapping {
     DeliverWorkerModelStatus.ONLINE  fromEnumEntry   DeliveryWorkerStatus.ONLINE
      DeliverWorkerModelStatus.OFFLINE fromEnumEntry    DeliveryWorkerStatus.OFFLINE
    }
}

