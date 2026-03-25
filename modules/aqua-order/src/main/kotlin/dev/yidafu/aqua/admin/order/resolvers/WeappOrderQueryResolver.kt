package dev.yidafu.aqua.admin.order.resolvers

import dev.yidafu.aqua.api.service.AdminService
import dev.yidafu.aqua.api.service.delivery.DeliveryWorkerQueryService
import dev.yidafu.aqua.api.service.order.DeliveryOrderQueryService
import dev.yidafu.aqua.api.service.order.OrderQueryService
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import dev.yidafu.aqua.common.exception.UserNotFoundException
import dev.yidafu.aqua.common.graphql.generated.Order
import dev.yidafu.aqua.common.graphql.generated.OrderPage
import dev.yidafu.aqua.common.graphql.generated.OrderStatus
import dev.yidafu.aqua.common.graphql.generated.TodayStatistics
import dev.yidafu.aqua.common.graphql.util.toPageInfo
import dev.yidafu.aqua.order.mapper.OrderMapper
import dev.yidafu.aqua.order.mapper.OrderStatusMapper
import dev.yidafu.aqua.order.mapper.TodayStatisticsMapper
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Controller

@Controller
class WeappOrderQueryResolver(
  private val deliveryTaskQueryService: DeliveryOrderQueryService,
  private val deliveryWorkerQueryService: DeliveryWorkerQueryService,
  private val orderQueryService: OrderQueryService,
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
  ): TodayStatistics {
    val actualWorkerId = adminService.findByUsername(userDetail.username)?.deliveryWorkerId
    return TodayStatisticsMapper.map(deliveryTaskQueryService.getTodayStatistics(actualWorkerId))
  }

  /**
   * 获取配送员历史订单（已完成、已取消、已退款）
   */
  @QueryMapping
  fun deliveryWorkerHistoryOrders(
    @Argument status: OrderStatus?,
    @Argument page: Int = 0,
    @Argument size: Int = 20,
    @AuthenticationPrincipal userDetail: UserDetails,
  ): OrderPage {
    val admin = adminService.findByUsername(userDetail.username)
    var workerId: Long? = null
    if (admin?.role === AdminRoleModel.DELIVERY_WORKER) {
      workerId = admin.deliveryWorkerId
    }
    val ordersPage =
      orderQueryService.getDeliveryWorkerHistoryOrders(
        workerId = workerId,
        status = status?.let { OrderStatusMapper.map(it) },
        page = page,
        size = size,
      )

    val (orderList, pageInfo) = ordersPage.toPageInfo { OrderMapper.map(it) }
    return OrderPage(
      content = orderList,
      totalElements = pageInfo.total,
      totalPages = pageInfo.totalPages,
      size = pageInfo.pageSize,
      number = pageInfo.pageNum,
    )
  }
}
