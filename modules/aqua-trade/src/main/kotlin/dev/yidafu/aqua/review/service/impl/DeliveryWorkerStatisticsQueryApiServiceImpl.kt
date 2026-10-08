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

package dev.yidafu.aqua.review.service.impl

import dev.yidafu.aqua.api.service.delivery.DeliveryWorkerStatisticsQueryApiService
import dev.yidafu.aqua.common.domain.model.DeliveryWorkerStatisticsModel
import dev.yidafu.aqua.review.domain.repository.DeliveryWorkerStatisticsRepository
import org.springframework.stereotype.Service

/**
 * 跨模块配送员统计查询服务实现
 * 供其他模块（如 statistics）使用
 */
@Service
class DeliveryWorkerStatisticsQueryApiServiceImpl(
  private val deliveryWorkerStatisticsRepository: DeliveryWorkerStatisticsRepository,
) : DeliveryWorkerStatisticsQueryApiService {
  override fun findByDeliveryWorkerId(deliveryWorkerId: Long): DeliveryWorkerStatisticsModel? =
    deliveryWorkerStatisticsRepository.findByDeliveryWorkerId(deliveryWorkerId)
}
