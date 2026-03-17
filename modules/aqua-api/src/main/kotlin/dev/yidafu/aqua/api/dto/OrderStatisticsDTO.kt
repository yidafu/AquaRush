package dev.yidafu.aqua.api.dto

import dev.yidafu.aqua.common.utils.MoneyUtils
import java.math.BigDecimal

/**
 * 订单统计DTO
 */
data class OrderStatisticsDTO(
  val totalOrders: Int = 0,
  val pendingOrders: Int = 0,
  val paidOrders: Int = 0,
  val deliveredOrders: Int = 0,
  val cancelledOrders: Int = 0,
  val totalAmountCents: Long = 0L,
  val averageOrderValueCents: Long = 0L,
) {
  val totalAmount: BigDecimal get() = MoneyUtils.fromCents(totalAmountCents)
  val averageOrderValue: BigDecimal get() = MoneyUtils.fromCents(averageOrderValueCents)
}
