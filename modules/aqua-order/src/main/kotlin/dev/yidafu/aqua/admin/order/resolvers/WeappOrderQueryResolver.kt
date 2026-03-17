package dev.yidafu.aqua.admin.order.resolvers

import dev.yidafu.aqua.api.service.AdminService
import dev.yidafu.aqua.api.service.delivery.DeliveryTaskQueryService
import dev.yidafu.aqua.api.service.delivery.DeliveryWorkerQueryService
import dev.yidafu.aqua.common.exception.UserNotFoundException
import dev.yidafu.aqua.common.graphql.generated.Order
import dev.yidafu.aqua.order.mapper.OrderMapper
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Controller

@Controller
class WeappOrderQueryResolver(
  private val deliveryTaskQueryService: DeliveryTaskQueryService,
  private val deliveryWorkerQueryService: DeliveryWorkerQueryService,
  private val adminService: AdminService,
) {
  fun getAdminIdByUsername(username: String): Long =
    adminService.findByUsername(username)?.id ?: throw UserNotFoundException("管理员 $username 不存在")

  /**
   * 获取待派单订单列表 - 管理员权限
   */
  @QueryMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun pendingDeliveryOrders(): List<Order> = deliveryTaskQueryService.getPendingDeliveryOrders().map { OrderMapper.map(it) }

  /**
   * 获取配送员的已接单订单
   */
  @QueryMapping
  fun assignedOrders(
    @AuthenticationPrincipal userDetail: UserDetails,
  ): List<Order> = deliveryTaskQueryService.getAssignedOrders(getAdminIdByUsername(userDetail.username)).map { OrderMapper.map(it) }

  /**
   * 获取配送员的配送中订单
   */
  @QueryMapping
  fun deliveringOrders(
    @AuthenticationPrincipal userDetail: UserDetails,
  ): List<Order> = deliveryWorkerQueryService.getWorkerActiveTasks(getAdminIdByUsername(userDetail.username)).map { OrderMapper.map(it) }

  /**
   * 获取配送员当日统计数据
   */
  @QueryMapping
  fun todayStatistics(
    @AuthenticationPrincipal userDetail: UserDetails,
  ): DeliveryTaskQueryService.TodayStatistics = deliveryTaskQueryService.getTodayStatistics(getAdminIdByUsername(userDetail.username))
}
