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

package dev.yidafu.aqua.delivery.service.impl

import dev.yidafu.aqua.api.service.order.DeliveryOrderQueryService
import dev.yidafu.aqua.api.service.delivery.DeliveryWorkerQueryService
import dev.yidafu.aqua.common.domain.model.DeliverWorkerModelStatus
import dev.yidafu.aqua.common.domain.model.DeliveryWorkerModel
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.exception.NotFoundException
import dev.yidafu.aqua.common.exception.UserNotFoundException
import dev.yidafu.aqua.delivery.domain.repository.DeliveryWorkerRepository
import org.springframework.stereotype.Service

@Service
class DeliveryWorkerQueryServiceImpl(
  private val workerRepository: DeliveryWorkerRepository,
  private val deliveryOrderQueryService: DeliveryOrderQueryService,
) : DeliveryWorkerQueryService {
  override fun getWorkerById(workerId: Long): DeliveryWorkerModel =
    workerRepository.findById(workerId).orElseThrow {
      NotFoundException("配送员不存在: $workerId")
    }

  override fun getWorkerByAdminId(adminId: Long): DeliveryWorkerModel =
    workerRepository.findByAdminId(adminId) ?: throw NotFoundException("配送员不存在: $adminId")

  override fun getAllWorkers(): List<DeliveryWorkerModel> = workerRepository.findAll()

  override fun getOnlineWorkers(): List<DeliveryWorkerModel> = workerRepository.findByOnlineStatus(DeliverWorkerModelStatus.ONLINE)

  override fun getWorkerActiveTasks(adminId: Long): List<OrderModel> {
    val worker = workerRepository.findByAdminId(adminId)
    val workerId = worker?.id ?: throw UserNotFoundException("管理员账号未关联送水员")

    return deliveryOrderQueryService.getOrdersByStatus(workerId, OrderModelStatus.DELIVERING)
  }

  override fun getWorkerActiveTaskCount(adminId: Long): Int {
    val worker = workerRepository.findByAdminId(adminId)
    val workerId = worker?.id ?: throw UserNotFoundException("管理员账号未关联送水员")
    return 0
  }
}
