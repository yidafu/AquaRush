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

import dev.yidafu.aqua.api.service.delivery.DeliveryTrajectoryQueryService
import dev.yidafu.aqua.common.domain.model.DeliveryTrajectoryModel
import dev.yidafu.aqua.delivery.domain.repository.DeliveryTrajectoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 配送轨迹查询服务实现(PRD 05-address-delivery.md §F-ADR-012,R-ADR-03)
 *
 * 轨迹数据涉及用户收货地址隐私,仅管理员可查(R-AUTH-02 权限隔离,resolver 层强制)。
 */
@Service
class DeliveryTrajectoryQueryServiceImpl(
  private val trajectoryRepository: DeliveryTrajectoryRepository,
) : DeliveryTrajectoryQueryService {
  @Transactional(readOnly = true)
  override fun getTrajectoriesByOrder(
    deliveryWorkerId: Long,
    orderId: Long,
  ): List<DeliveryTrajectoryModel> = trajectoryRepository.findByDeliveryWorkerIdAndOrderIdOrderByReportedAtAsc(deliveryWorkerId, orderId)

  @Transactional(readOnly = true)
  override fun getTrajectories(
    deliveryWorkerId: Long,
    orderId: Long?,
    start: LocalDateTime?,
    end: LocalDateTime?,
  ): List<DeliveryTrajectoryModel> {
    if (orderId != null) {
      return trajectoryRepository.findByDeliveryWorkerIdAndOrderIdOrderByReportedAtAsc(deliveryWorkerId, orderId)
    }
    val from = start ?: LocalDateTime.MIN.plusDays(1)
    val to = end ?: LocalDateTime.MAX.minusNanos(1)
    return trajectoryRepository.findByDeliveryWorkerIdAndReportedAtBetweenOrderByReportedAtAsc(deliveryWorkerId, from, to)
  }

  @Transactional(readOnly = true)
  override fun getLatestTrajectory(deliveryWorkerId: Long): DeliveryTrajectoryModel? =
    trajectoryRepository.findFirstByDeliveryWorkerIdOrderByReportedAtDesc(deliveryWorkerId)
}
