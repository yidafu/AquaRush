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

package dev.yidafu.aqua.admin.delivery.resolvers

import dev.yidafu.aqua.api.service.delivery.DeliveryTrajectoryQueryService
import dev.yidafu.aqua.common.graphql.generated.DeliveryTrajectory
import dev.yidafu.aqua.delivery.mapper.toGraphQL
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import java.time.LocalDateTime

/**
 * 配送轨迹查询解析器(F-ADR-012,R-ADR-03)
 *
 * 轨迹数据涉及用户收货地址隐私,仅管理员可查(R-AUTH-02 权限隔离)。
 * 权限:PERMISSION_DELIVERY_WORKER_READ(超级管理员/管理员;送水工不可查,见 01-overview.md §角色权限矩阵)。
 */
@Controller
class DeliveryTrajectoryQueryResolver(
  private val deliveryTrajectoryQueryService: DeliveryTrajectoryQueryService,
) {
  /**
   * 送水工配送轨迹(后台轨迹管理页;orderId 指定=查该订单轨迹,否则按时间范围)
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_DELIVERY_WORKER_READ')")
  fun deliveryTrajectories(
    @Argument workerId: Long,
    @Argument orderId: Long?,
    @Argument startTime: LocalDateTime?,
    @Argument endTime: LocalDateTime?,
  ): List<DeliveryTrajectory> =
    deliveryTrajectoryQueryService
      .getTrajectories(workerId, orderId, startTime, endTime)
      .map { it.toGraphQL() }

  /**
   * 送水工最近轨迹点(实时位置;无轨迹返回 null)
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_DELIVERY_WORKER_READ')")
  fun deliveryWorkerLatestLocation(
    @Argument workerId: Long,
  ): DeliveryTrajectory? = deliveryTrajectoryQueryService.getLatestTrajectory(workerId)?.toGraphQL()
}
