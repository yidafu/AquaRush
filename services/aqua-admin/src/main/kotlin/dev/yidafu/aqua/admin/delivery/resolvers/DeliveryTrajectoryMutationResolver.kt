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

import dev.yidafu.aqua.admin.delivery.dto.ReportTrajectoryInput
import dev.yidafu.aqua.api.service.admin.AdminService
import dev.yidafu.aqua.api.service.delivery.DeliveryTrajectoryMutationService
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.exception.UserNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Controller
import java.time.LocalDateTime

/**
 * 配送轨迹上报解析器(F-ADR-012,R-ADR-03)
 *
 * 送水工端 App 在订单「配送中」期间按设定频率(默认 5s)调本 mutation 上报位置点;
 * 服务端校验仅本人 DELIVERING 订单可上报(非配送中上报丢弃)。
 *
 * 权限:PERMISSION_DELIVERY_TASK(送水工/管理员兼送水均有,见 01-overview.md §角色权限矩阵)。
 */
@Controller
class DeliveryTrajectoryMutationResolver(
  private val deliveryTrajectoryMutationService: DeliveryTrajectoryMutationService,
  private val adminService: AdminService,
) {
  private val logger = LoggerFactory.getLogger(DeliveryTrajectoryMutationResolver::class.java)

  /**
   * 上报轨迹点(送水工本人,workerId 由登录主体推导,防越权)
   */
  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_DELIVERY_TASK')")
  fun reportTrajectory(
    @Argument input: ReportTrajectoryInput,
    @AuthenticationPrincipal userDetail: UserDetails,
  ): Boolean {
    val admin =
      adminService.findByUsername(userDetail.username)
        ?: throw UserNotFoundException("账号 ${userDetail.username} 不存在")
    if (admin.role != AdminRoleModel.DELIVERY_WORKER && admin.role != AdminRoleModel.ADMIN) {
      throw BadRequestException("当前账号不具备配送能力,无法上报轨迹")
    }
    val workerId = admin.id ?: throw UserNotFoundException("账号 ${admin.username} 不存在")

    deliveryTrajectoryMutationService.reportTrajectory(
      deliveryWorkerId = workerId,
      orderId = input.orderId,
      longitude = input.longitude.toDouble(),
      latitude = input.latitude.toDouble(),
      accuracy = input.accuracy?.toDouble(),
      reportedAt = input.reportedAt ?: LocalDateTime.now(),
    )
    logger.debug("Trajectory reported by worker $workerId for order ${input.orderId}")
    return true
  }
}
