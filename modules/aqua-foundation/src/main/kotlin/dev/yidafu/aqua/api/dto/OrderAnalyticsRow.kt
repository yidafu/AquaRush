package dev.yidafu.aqua.api.dto

import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import java.time.LocalDate

/**
 * Data class for order analytics results
 */
data class OrderAnalyticsRow(
  val orderDate: LocalDate,
  val status: OrderModelStatus,
  val orderCount: Long,
  val totalRevenue: Double,
  val averageOrderValue: Double,
  val uniqueCustomers: Long,
  val activeWorkers: Long,
)
