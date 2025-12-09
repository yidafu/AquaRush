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

package dev.yidafu.aqua.delivery.graphql.resolvers

import dev.yidafu.aqua.common.graphql.generated.DeliveryWorker
import dev.yidafu.aqua.common.domain.model.DeliveryWorkerModel
import dev.yidafu.aqua.delivery.mapper.DeliveryWorkerMapper
import dev.yidafu.aqua.delivery.service.DeliveryService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import kotlin.collections.map

@Controller
class DeliveryWorkerQueryResolver(
  private val deliveryService: DeliveryService,
) {

  @QueryMapping
  @PreAuthorize("hasRole('ADMIN') or hasRole('WORKER')")
  fun deliveryWorkers(): List<DeliveryWorker> {
    return DeliveryWorkerMapper.mapList(deliveryService.getAllWorkers())
  }

  @QueryMapping
  @PreAuthorize("hasRole('ADMIN') or hasRole('WORKER')")
  fun onlineDeliveryWorkers(): List<DeliveryWorker> {
    return DeliveryWorkerMapper.mapList(deliveryService.getOnlineWorkers())

  }

  @QueryMapping
  @PreAuthorize("hasRole('ADMIN') or hasRole('WORKER')")
  fun deliveryWorker(
    @Argument id: Long,
  ): DeliveryWorker? = DeliveryWorkerMapper.map(deliveryService.getWorkerById(id))
}
