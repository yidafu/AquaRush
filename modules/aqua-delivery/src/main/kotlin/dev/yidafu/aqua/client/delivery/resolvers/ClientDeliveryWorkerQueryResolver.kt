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

package dev.yidafu.aqua.client.delivery.resolvers

import dev.yidafu.aqua.common.annotation.ClientService
import dev.yidafu.aqua.common.graphql.generated.DeliveryWorker
import dev.yidafu.aqua.delivery.mapper.DeliveryWorkerMapper
import dev.yidafu.aqua.delivery.service.DeliveryService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

/**
 * 客户端配送员查询解析器
 * 提供配送员自服务的查询功能，配送员只能查看自己的信息
 */
@ClientService
@Controller
class ClientDeliveryWorkerQueryResolver(
  private val deliveryService: DeliveryService,
) {
  /**
   * 查询在线配送员（公开信息）
   * 用户可以看到哪些配送员在线，但看不到敏感信息
   */
  @PreAuthorize("isAuthenticated()")
  fun onlineDeliveryWorkersPublic(): List<DeliveryWorker> = DeliveryWorkerMapper.mapList(deliveryService.getOnlineWorkers())

  /**
   * 配送员查询自己的信息
   */
  @PreAuthorize("hasRole('WORKER')")
  fun myDeliveryWorkerProfile(): DeliveryWorker? {
    // 获取当前认证的配送员ID
    val currentWorkerId = getCurrentWorkerId()
    return DeliveryWorkerMapper.map(deliveryService.getWorkerById(currentWorkerId))
  }

  /**
   * 配送员查询自己的活跃任务
   */
  @PreAuthorize("hasRole('WORKER')")
  fun myActiveTasks(): List<Any> {
    // 获取当前认证的配送员ID
    val currentWorkerId = getCurrentWorkerId()
    return deliveryService.getWorkerActiveTasks(currentWorkerId)
  }

  /**
   * 查询配送员基本信息（公开信息）
   */
  @PreAuthorize("isAuthenticated()")
  fun deliveryWorkerPublicInfo(workerId: Long): DeliveryWorker? {
    val worker = deliveryService.getWorkerById(workerId)

    return DeliveryWorkerMapper.map(worker)
  }

  /**
   * 获取当前认证配送员的ID
   * 在实际实现中应该从Spring Security Context获取
   */
  private fun getCurrentWorkerId(): Long {
    // TODO: 从Spring Security Context获取当前配送员ID
    // 暂时返回占位符，实际实现需要：
    // val authentication = SecurityContextHolder.getContext().authentication
    // return (authentication.principal as DeliveryWorkerDetails).id
    throw UnsupportedOperationException("需要从Spring Security Context获取配送员ID")
  }
}
