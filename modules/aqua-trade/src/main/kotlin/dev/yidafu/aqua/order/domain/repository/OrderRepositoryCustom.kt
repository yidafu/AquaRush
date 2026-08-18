package dev.yidafu.aqua.order.domain.repository

import dev.yidafu.aqua.api.dto.OrderAnalyticsRow
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.domain.model.enums.PaymentType
import org.springframework.data.domain.Page
import java.time.LocalDateTime

interface OrderRepositoryCustom {
  fun findOrdersWithFilters(
    userId: Long?,
    status: OrderModelStatus?,
    deliveryWorkerId: Long?,
    startDate: LocalDateTime?,
    endDate: LocalDateTime?,
    orderNumber: String?,
    statuses: List<OrderModelStatus>?,
  ): List<OrderModel>

  fun findDeliveryWorkerOrdersWithFilters(
    deliveryWorkerId: Long,
    status: OrderModelStatus,
    startDate: LocalDateTime?,
    endDate: LocalDateTime?,
    limit: Int?,
  ): List<OrderModel>

  fun countOrdersWithFilters(
    userId: Long?,
    status: OrderModelStatus?,
    deliveryWorkerId: Long?,
    startDate: LocalDateTime?,
    endDate: LocalDateTime?,
    statuses: List<OrderModelStatus>?,
  ): Long

  /**
   * 分页查询订单
   */
  fun findOrdersPaginated(
    keyword: String? = null,
    status: OrderModelStatus? = null,
    userId: Long? = null,
    deliveryWorkerId: Long? = null,
    startDate: LocalDateTime? = null,
    endDate: LocalDateTime? = null,
    minAmount: Long? = null,
    maxAmount: Long? = null,
    page: Int = 0,
    size: Int = 20,
    sortField: String = "createdAt",
    sortDirection: String = "desc",
  ): Page<OrderModel>

  fun bulkUpdateOrderStatus(
    orderIds: List<Long>,
    newStatus: OrderModelStatus,
    deliveryWorkerId: Long?,
  ): Int

  fun getOrderAnalytics(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): List<OrderAnalyticsRow>

  /**
   * 分页查询配送员历史订单（按多个状态）
   */
  fun findByDeliveryWorkerIdAndStatusIn(
    deliveryWorkerId: Long?,
    statuses: List<OrderModelStatus>,
    keyword: String? = null,
    page: Int,
    size: Int,
  ): Page<OrderModel>

  /**
   * 用户订单分页查询(F-ORD-002 用户端订单列表,G02):
   * statuses 为空=全部状态;按下单时间倒序;userId 由调用方强制为登录用户(防越权)。
   */
  fun findUserOrdersPaginated(
    userId: Long,
    statuses: List<OrderModelStatus>?,
    page: Int,
    size: Int,
  ): Page<OrderModel>

  /**
   * 统计指定日期范围内的订单数量（可按配送员筛选，可按状态筛选）
   */
  fun countOrdersByDateRange(
    startOfDay: LocalDateTime,
    endOfDay: LocalDateTime,
    deliveryWorkerId: Long?,
    statuses: List<OrderModelStatus>? = null,
  ): Long

  /**
   * 统计指定日期范围内指定状态订单的金额总和（可按配送员筛选）
   */
  fun sumAmountCentsByStatusAndDateRange(
    statuses: List<OrderModelStatus>,
    startOfDay: LocalDateTime,
    endOfDay: LocalDateTime,
    deliveryWorkerId: Long?,
  ): Long

  /**
   * 按支付类型和日期范围统计订单数量和金额
   */
  fun countAndSumByPaymentType(
    deliveryWorkerId: Long,
    status: OrderModelStatus,
    paymentType: PaymentType,
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): Array<Long>

  /**
   * 按订单状态分组统计指定用户的订单数(F-USR-002,G31 我的页订单角标):
   * 仅本人订单(软删除自动过滤);缺失状态不计入(调用方补 0)。
   */
  fun countOrdersByUserIdGroupByStatus(userId: Long): Map<OrderModelStatus, Long>
}
