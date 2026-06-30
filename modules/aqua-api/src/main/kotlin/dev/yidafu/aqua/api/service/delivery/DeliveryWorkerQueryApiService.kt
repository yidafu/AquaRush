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

package dev.yidafu.aqua.api.service.delivery

import dev.yidafu.aqua.common.domain.model.DeliverWorkerModelStatus
import dev.yidafu.aqua.common.domain.model.DeliveryWorkerModel
import dev.yidafu.aqua.common.domain.model.DeliveryWorkerStatisticsModel

/**
 * 跨模块配送员查询服务接口
 * 由 aqua-delivery 模块实现，供其他模块使用
 */
interface DeliveryWorkerQueryApiService {
  fun findById(id: Long): DeliveryWorkerModel?

  fun findByIds(ids: List<Long>): List<DeliveryWorkerModel>

  fun findAll(): List<DeliveryWorkerModel>

  fun findByOnlineStatus(status: DeliverWorkerModelStatus): List<DeliveryWorkerModel>

  fun existsById(id: Long): Boolean

  fun findByAdminId(adminId: Long): List<DeliveryWorkerModel>
}

/**
 * 跨模块配送员统计查询服务接口
 * 由 aqua-review 模块实现，供其他模块使用
 */
interface DeliveryWorkerStatisticsQueryApiService {
  fun findByDeliveryWorkerId(deliveryWorkerId: Long): DeliveryWorkerStatisticsModel?
}
