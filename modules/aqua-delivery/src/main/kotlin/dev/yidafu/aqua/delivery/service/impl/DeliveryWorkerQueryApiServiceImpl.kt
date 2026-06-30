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

import dev.yidafu.aqua.api.service.delivery.DeliveryWorkerQueryApiService
import dev.yidafu.aqua.common.domain.model.DeliverWorkerModelStatus
import dev.yidafu.aqua.common.domain.model.DeliveryWorkerModel
import dev.yidafu.aqua.delivery.domain.repository.DeliveryWorkerRepository
import org.springframework.stereotype.Service

/**
 * 跨模块配送员查询服务实现
 * 供其他模块使用
 */
@Service
class DeliveryWorkerQueryApiServiceImpl(
  private val deliveryWorkerRepository: DeliveryWorkerRepository,
) : DeliveryWorkerQueryApiService {
  override fun findById(id: Long): DeliveryWorkerModel? = deliveryWorkerRepository.findById(id).orElse(null)

  override fun findByIds(ids: List<Long>): List<DeliveryWorkerModel> = deliveryWorkerRepository.findAllById(ids)

  override fun findAll(): List<DeliveryWorkerModel> = deliveryWorkerRepository.findAll()

  override fun findByOnlineStatus(status: DeliverWorkerModelStatus): List<DeliveryWorkerModel> =
    deliveryWorkerRepository.findByOnlineStatus(status)

  override fun existsById(id: Long): Boolean = deliveryWorkerRepository.existsById(id)

  override fun findByAdminId(adminId: Long): List<DeliveryWorkerModel> {
    val result = deliveryWorkerRepository.findByAdminId(adminId)
    return if (result != null) listOf(result) else emptyList()
  }
}
