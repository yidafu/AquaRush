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

import tools.jackson.module.kotlin.jacksonObjectMapper
import dev.yidafu.aqua.api.dto.Coordinates
import dev.yidafu.aqua.api.dto.DeliveryWorkerDTO
import dev.yidafu.aqua.common.domain.model.DeliveryWorker
import org.springframework.stereotype.Component
import tech.mappie.api.ObjectMappie

@Component
object DeliveryWorkerMapper : ObjectMappie<DeliveryWorker, DeliveryWorkerDTO>() {
  private val objectMapper = jacksonObjectMapper()

  override fun map(from: DeliveryWorker): DeliveryWorkerDTO {
    // 处理 current_location 的 JSON 转换
    val currentLocation = from.currentLocation?.let { json ->
      try {
        objectMapper.readValue(json, Coordinates::class.java)
      } catch (e: Exception) {
        null
      }
    }

    // 处理 coordinates 的 JSON 转换
    val coordinates = from.coordinates?.let { json ->
      try {
        objectMapper.readValue(json, Coordinates::class.java)
      } catch (e: Exception) {
        null
      }
    }

    return DeliveryWorkerDTO(
      id = from.id,
      userId = from.userId,
      name = from.name,
      phone = from.phone,
      avatarUrl = from.avatarUrl,
      status = from.status,
      currentLocation = currentLocation,
      rating = from.rating,
      totalOrders = from.totalOrders,
      completedOrders = from.completedOrders,
      averageRating = from.averageRating,
      earning = from.earning,
      isAvailable = from.isAvailable,
      createdAt = from.createdAt,
      updatedAt = from.updatedAt,
    )
  }
}
