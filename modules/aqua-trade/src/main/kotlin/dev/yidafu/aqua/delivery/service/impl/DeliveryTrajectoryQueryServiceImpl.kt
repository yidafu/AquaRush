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
  companion object {
    // PostgreSQL timestamp 支持范围 4713 BC ~ 294276 AD;单边时间过滤时用范围内的
    // 安全边界兜底,避免 LocalDateTime.MIN/MAX(±999999999 年)导致 JDBC 绑定异常。
    private val SAFE_MIN_TIMESTAMP: LocalDateTime = LocalDateTime.of(1970, 1, 1, 0, 0)
    private val SAFE_MAX_TIMESTAMP: LocalDateTime = LocalDateTime.of(9999, 12, 31, 23, 59, 59)
  }

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
    // start/end 均为空 = 全部轨迹,直接按送水工查询(不拼时间条件)。
    // 注意:不要用 LocalDateTime.MIN/MAX 作为兜底边界 —— 其极端年份(±999999999)
    // 超出 PostgreSQL timestamp 范围(4713 BC ~ 294276 AD),JDBC 绑定参数会抛异常
    // (此前表现:deliveryTrajectories 无时间参数 → INTERNAL_ERROR)。
    if (start == null && end == null) {
      return trajectoryRepository.findByDeliveryWorkerIdOrderByReportedAtAsc(deliveryWorkerId)
    }
    // 单边为空:用 PostgreSQL timestamp 支持范围内的安全边界
    val from = start ?: SAFE_MIN_TIMESTAMP
    val to = end ?: SAFE_MAX_TIMESTAMP
    return trajectoryRepository.findByDeliveryWorkerIdAndReportedAtBetweenOrderByReportedAtAsc(deliveryWorkerId, from, to)
  }

  @Transactional(readOnly = true)
  override fun getLatestTrajectory(deliveryWorkerId: Long): DeliveryTrajectoryModel? =
    trajectoryRepository.findFirstByDeliveryWorkerIdOrderByReportedAtDesc(deliveryWorkerId)
}
