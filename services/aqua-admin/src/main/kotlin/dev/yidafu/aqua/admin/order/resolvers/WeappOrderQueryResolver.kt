package dev.yidafu.aqua.admin.order.resolvers

import dev.yidafu.aqua.admin.order.mapper.AdminOrderMapper
import dev.yidafu.aqua.api.service.admin.AdminService
import dev.yidafu.aqua.api.service.order.DeliveryOrderQueryService
import dev.yidafu.aqua.api.service.order.OrderQueryService
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.exception.UserNotFoundException
import dev.yidafu.aqua.common.graphql.generated.OrderStatus
import dev.yidafu.aqua.common.graphql.generated.OrderVo
import dev.yidafu.aqua.common.graphql.generated.OrderVoPage
import dev.yidafu.aqua.common.graphql.generated.TodayStatisticsVo
import dev.yidafu.aqua.common.graphql.generated.WeekStatisticsVo
import dev.yidafu.aqua.common.graphql.util.toPageInfo
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.order.mapper.toDomain
import dev.yidafu.aqua.order.mapper.toGraphQL
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

@Controller
class WeappOrderQueryResolver(
  private val deliveryTaskQueryService: DeliveryOrderQueryService,
  private val orderQueryService: OrderQueryService,
  private val adminService: AdminService,
  private val adminOrderMapper: AdminOrderMapper,
) {
  fun getAdminIdByUsername(username: String): Long =
    adminService.findByUsername(username)?.id ?: throw UserNotFoundException("管理员 $username 不存在")

  /**
   * 获取待派单订单列表(抢单大厅) - 管理员权限
   * 号码保护(F-ORD-032/R-ORD-17):待派单非配送阶段,送水工视角脱敏收货人电话。
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_ORDER_READ')")
  fun pendingDeliveryOrders(
    @AuthenticationPrincipal userPrincipal: UserPrincipal?,
  ): List<OrderVo> = deliveryTaskQueryService.getPendingDeliveryOrders().map { adminOrderMapper.mapForViewer(it, userPrincipal) }

  /**
   * 获取配送员的已接单订单
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_ORDER_READ')")
  fun assignedOrders(
    @AuthenticationPrincipal userPrincipal: UserPrincipal,
  ): List<OrderVo> =
    deliveryTaskQueryService
      .getAssignedOrders(getAdminIdByUsername(userPrincipal.username))
      .map { adminOrderMapper.mapForViewer(it, userPrincipal) }

  /**
   * 获取配送员的配送中订单
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_ORDER_READ')")
  fun deliveringOrders(
    @AuthenticationPrincipal userPrincipal: UserPrincipal,
  ): List<OrderVo> =
    deliveryTaskQueryService
      .getOrdersByStatus(
        getAdminIdByUsername(userPrincipal.username),
        dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus.DELIVERING,
      ).map { adminOrderMapper.mapForViewer(it, userPrincipal) }

  /**
   * 获取配送员当日统计数据
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_STATISTICS_READ')")
  fun todayStatistics(
    @AuthenticationPrincipal userPrincipal: UserPrincipal,
  ): TodayStatisticsVo {
    // 送水员档案并入 admins 单表后,账号 id 即送水员 id(迁移 v1.2.1)
    val actualWorkerId = adminService.findByUsername(userPrincipal.username)?.id
    return deliveryTaskQueryService.getTodayStatistics(actualWorkerId).toGraphQL()
  }

  /**
   * 获取配送员一周统计数据
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_STATISTICS_READ')")
  fun weekStatistics(
    @AuthenticationPrincipal userPrincipal: UserPrincipal,
  ): WeekStatisticsVo {
    val actualWorkerId = adminService.findByUsername(userPrincipal.username)?.id
    return deliveryTaskQueryService.getWeekStatistics(actualWorkerId).toGraphQL()
  }

  /**
   * 获取配送员历史订单（已完成、已取消、已退款）
   * 号码保护(F-ORD-032/R-ORD-17):终态订单非配送阶段,送水工视角脱敏收货人电话。
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_ORDER_READ')")
  fun deliveryWorkerHistoryOrders(
    @Argument status: OrderStatus?,
    @Argument keyword: String?,
    @Argument page: Int = 0,
    @Argument size: Int = 20,
    @AuthenticationPrincipal userPrincipal: UserPrincipal,
  ): OrderVoPage {
    val admin = adminService.findByUsername(userPrincipal.username)
    var workerId: Long? = null
    if (admin?.role === AdminRoleModel.DELIVERY_WORKER || admin?.role === AdminRoleModel.ADMIN) {
      workerId = admin.id
    }
    val ordersPage =
      orderQueryService.getDeliveryWorkerHistoryOrders(
        workerId = workerId,
        status = status?.toDomain(),
        keyword = keyword,
        page = page,
        size = size,
      )

    val (orderList, pageInfo) = ordersPage.toPageInfo { adminOrderMapper.mapForViewer(it, userPrincipal) }
    return OrderVoPage(
      content = orderList,
      totalElements = pageInfo.total,
      totalPages = pageInfo.totalPages,
      size = pageInfo.pageSize,
      number = pageInfo.pageNum,
    )
  }
}
