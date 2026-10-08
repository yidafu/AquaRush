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

import dev.yidafu.aqua.common.domain.model.DeliveryTrajectoryModel
import java.time.LocalDateTime

/**
 * 配送轨迹查询服务接口(PRD 05-address-delivery.md §F-ADR-012,R-ADR-03)
 *
 * 轨迹数据涉及用户收货地址隐私,仅管理员可查(R-AUTH-02 权限隔离,resolver 层强制)。
 */
interface DeliveryTrajectoryQueryService {
  /**
   * 查询送水工在指定订单上的轨迹点(按上报时间升序)
   */
  fun getTrajectoriesByOrder(
    deliveryWorkerId: Long,
    orderId: Long,
  ): List<DeliveryTrajectoryModel>

  /**
   * 查询送水工在时间范围内的轨迹点(按上报时间升序;时间范围可空=全部)
   */
  fun getTrajectories(
    deliveryWorkerId: Long,
    orderId: Long?,
    start: LocalDateTime?,
    end: LocalDateTime?,
  ): List<DeliveryTrajectoryModel>

  /**
   * 查询送水工最近一个轨迹点(实时位置;无轨迹返回 null)
   */
  fun getLatestTrajectory(deliveryWorkerId: Long): DeliveryTrajectoryModel?
}
