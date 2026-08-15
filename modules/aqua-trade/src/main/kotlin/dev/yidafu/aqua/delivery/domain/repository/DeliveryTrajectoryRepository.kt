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

package dev.yidafu.aqua.delivery.domain.repository

import dev.yidafu.aqua.common.domain.model.DeliveryTrajectoryModel
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * 配送轨迹点仓库(PRD 05-address-delivery.md §F-ADR-012,R-ADR-03)
 *
 * 追加式日志数据(不做软删除);delivery_worker_id 即账号 id(admins 单表,迁移 v1.2.1)。
 */
@Repository
interface DeliveryTrajectoryRepository : JpaRepository<DeliveryTrajectoryModel, Long> {
  /**
   * 查询送水工在某订单上的轨迹点(按上报时间升序,轨迹回放顺序)
   */
  fun findByDeliveryWorkerIdAndOrderIdOrderByReportedAtAsc(
    deliveryWorkerId: Long,
    orderId: Long,
  ): List<DeliveryTrajectoryModel>

  /**
   * 查询送水工在时间范围内的全部轨迹点(按上报时间升序,后台轨迹查看)
   */
  fun findByDeliveryWorkerIdAndReportedAtBetweenOrderByReportedAtAsc(
    deliveryWorkerId: Long,
    start: LocalDateTime,
    end: LocalDateTime,
  ): List<DeliveryTrajectoryModel>

  /**
   * 查询送水工最近一个轨迹点(实时位置)
   */
  fun findFirstByDeliveryWorkerIdOrderByReportedAtDesc(deliveryWorkerId: Long): DeliveryTrajectoryModel?
}
