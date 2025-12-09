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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package dev.yidafu.aqua.admin.delivery.resolvers

import dev.yidafu.aqua.admin.delivery.resolvers.AdminDeliveryWorkerMutationResolver.Companion.CreateDeliveryWorkerInput
import dev.yidafu.aqua.admin.delivery.resolvers.AdminDeliveryWorkerMutationResolver.Companion.UpdateDeliveryWorkerInput
import dev.yidafu.aqua.common.annotation.AdminService
import dev.yidafu.aqua.common.domain.model.DeliveryWorkerModel
import dev.yidafu.aqua.common.graphql.generated.DeliveryWorker
import  dev.yidafu.aqua.delivery.mapper.DeliveryWorkerMapper
import dev.yidafu.aqua.delivery.service.DeliveryService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

/**
 * 管理端配送员查询解析器
 * 提供配送员管理的查询功能，仅管理员可访问
 */
@AdminService
@Controller
class AdminDeliveryWorkerQueryResolver(
    private val deliveryService: DeliveryService
) {

    /**
     * 查询所有配送员（管理员功能）
     */
    @PreAuthorize("hasRole('ADMIN')")
    fun deliveryWorkers(): List<DeliveryWorker> {
      return DeliveryWorkerMapper.mapList( deliveryService.getAllWorkers())
    }

    /**
     * 查询在线配送员（管理员功能）
     */
    @PreAuthorize("hasRole('ADMIN')")
    fun onlineDeliveryWorkers(): List<DeliveryWorker> {
      return DeliveryWorkerMapper.mapList( deliveryService.getOnlineWorkers())
    }

    /**
     * 根据ID查询配送员（管理员功能）
     */
    @PreAuthorize("hasRole('ADMIN')")
    fun deliveryWorker(id: Long): DeliveryWorker? {
      return DeliveryWorkerMapper.map(deliveryService.getWorkerById(id))
    }

    /**
     * 查询配送员的活跃任务数量（管理员功能）
     */
    @PreAuthorize("hasRole('ADMIN')")
    fun deliveryWorkerActiveTasks(workerId: Long): Int {
        return deliveryService.getWorkerActiveTasks(workerId).size
    }
}
