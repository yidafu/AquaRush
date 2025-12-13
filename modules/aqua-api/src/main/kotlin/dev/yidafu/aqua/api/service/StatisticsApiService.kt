/*
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

package dev.yidafu.aqua.api.service

import dev.yidafu.aqua.api.dto.*
import dev.yidafu.aqua.common.domain.model.PaymentMethod
import dev.yidafu.aqua.common.utils.MoneyUtils
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

/**
 * 统计API服务接口
 */
interface StatisticsApiService {
  /**
   * 获取系统概览统计
   */
  fun getSystemOverview(): SystemOverviewDTO

  /**
   * 获取用户统计信息
   */
  fun getUserStatistics(
    userId: Long,
    period: StatisticsPeriod = StatisticsPeriod.MONTH,
  ): UserStatisticsDTO

  /**
   * 获取订单统计信息
   */
  fun getOrderStatistics(
    period: StatisticsPeriod = StatisticsPeriod.MONTH,
    startDate: LocalDateTime? = null,
    endDate: LocalDateTime? = null,
  ): OrderStatisticsDTO

  /**
   * 获取收入统计
   */
  fun getRevenueStatistics(
    period: StatisticsPeriod = StatisticsPeriod.MONTH,
    startDate: LocalDateTime? = null,
    endDate: LocalDateTime? = null,
  ): RevenueStatisticsDTO

  /**
   * 获取配送统计
   */
  fun getDeliveryStatistics(period: StatisticsPeriod = StatisticsPeriod.MONTH): DeliveryStatisticsDTO

  /**
   * 获取产品销售排行
   */
  fun getProductSalesRanking(
    period: StatisticsPeriod = StatisticsPeriod.MONTH,
    limit: Int = 10,
  ): List<ProductSalesRankingDTO>

  /**
   * 获取用户消费排行
   */
  fun getUserSpendingRanking(
    period: StatisticsPeriod = StatisticsPeriod.MONTH,
    limit: Int = 10,
  ): List<UserSpendingRankingDTO>

  /**
   * 获取地区销售统计
   */
  fun getRegionalSalesStatistics(period: StatisticsPeriod = StatisticsPeriod.MONTH): List<RegionalSalesDTO>

  /**
   * 获取实时统计数据
   */
  fun getRealTimeStatistics(): RealTimeStatisticsDTO

  /**
   * 获取配-送员效率统计
   */
  fun getWorkerEfficiencyStats(period: StatisticsPeriod = StatisticsPeriod.MONTH): List<WorkerEfficiencyDTO>

  /**
   * 获取支付方式统计
   */
  fun getPaymentMethodStatistics(period: StatisticsPeriod = StatisticsPeriod.MONTH): List<PaymentMethodStatsDTO>

  /**
   * 导出统计报告
   */
  fun exportStatisticsReport(
    reportType: StatisticsReportType,
    period: StatisticsPeriod,
    format: ExportFormat = ExportFormat.EXCEL,
  ): ByteArray

  /**
   * 获取增长趋势数据
   */
  fun getGrowthTrends(
    metric: GrowthMetric,
    period: StatisticsPeriod = StatisticsPeriod.YEAR,
  ): List<TrendDataDTO>
}

/**
 * 统计周期枚举
 */
enum class StatisticsPeriod {
  TODAY,
  WEEK,
  MONTH,
  QUARTER,
  YEAR,
  CUSTOM,
}

/**
 * 统计报告类型
 */
enum class StatisticsReportType {
  REVENUE,
  ORDERS,
  USERS,
  DELIVERY,
  PRODUCTS,
}

/**
 * 导出格式
 */
enum class ExportFormat {
  EXCEL,
  CSV,
  PDF,
  JSON,
}

/**
 * 增长指标
 */
enum class GrowthMetric {
  REVENUE,
  ORDERS,
  USERS,
  DELIVERIES,
}

/**
 * 系统概览DTO
 */
data class SystemOverviewDTO(
  val totalUsers: Int = 0,
  val activeUsers: Int = 0,
  val totalOrders: Int = 0,
  val pendingOrders: Int = 0,
  val totalRevenueCents: Long = 0L,
  val todayRevenueCents: Long = 0L,
  val totalDeliveryWorkers: Int = 0,
  val activeDeliveryWorkers: Int = 0,
  val averageOrderValueCents: Long = 0L,
  val systemUptime: Long = 0L,
) {
  val totalRevenue: BigDecimal get() = MoneyUtils.fromCents(totalRevenueCents)
  val todayRevenue: BigDecimal get() = MoneyUtils.fromCents(todayRevenueCents)
  val averageOrderValue: BigDecimal get() = MoneyUtils.fromCents(averageOrderValueCents)
}

/**
 * 用户统计DTO
 */
data class UserStatisticsDTO(
  val userId: Long,
  val totalOrders: Int = 0,
  val totalSpentCents: Long = 0L,
  val averageOrderValueCents: Long = 0L,
  val favoriteProduct: String? = null,
  val orderFrequency: Map<String, Int> = emptyMap(),
  val lastOrderDate: LocalDateTime? = null,
) {
  val totalSpent: BigDecimal get() = MoneyUtils.fromCents(totalSpentCents)
  val averageOrderValue: BigDecimal get() = MoneyUtils.fromCents(averageOrderValueCents)
}

/**
 * 收入统计DTO
 */
data class RevenueStatisticsDTO(
  val totalRevenueCents: Long = 0L,
  val netRevenueCents: Long = 0L,
  val refundsAmountCents: Long = 0L,
  val revenueByPaymentMethodCents: Map<String, Long> = emptyMap(),
  val revenueByHourCents: Map<String, Long> = emptyMap(),
  val revenueByDayCents: Map<String, Long> = emptyMap(),
) {
  val totalRevenue: BigDecimal get() = MoneyUtils.fromCents(totalRevenueCents)
  val netRevenue: BigDecimal get() = MoneyUtils.fromCents(netRevenueCents)
  val refundsAmount: BigDecimal get() = MoneyUtils.fromCents(refundsAmountCents)
  val revenueByPaymentMethod: Map<String, BigDecimal>
    get() = revenueByPaymentMethodCents.mapValues { MoneyUtils.fromCents(it.value) }
  val revenueByHour: Map<String, BigDecimal>
    get() = revenueByHourCents.mapValues { MoneyUtils.fromCents(it.value) }
  val revenueByDay: Map<String, BigDecimal>
    get() = revenueByDayCents.mapValues { MoneyUtils.fromCents(it.value) }
}

/**
 * 产品销售排行DTO
 */
data class ProductSalesRankingDTO(
  val productId: Long,
  val productName: String,
  val totalSales: Int = 0,
  val totalRevenueCents: Long = 0L,
  val growthRateCents: Long = 0L,
) {
  val totalRevenue: BigDecimal get() = MoneyUtils.fromCents(totalRevenueCents)
  val growthRate: BigDecimal get() = MoneyUtils.fromCents(growthRateCents)
}

/**
 * 用户消费排行DTO
 */
data class UserSpendingRankingDTO(
  val userId: Long,
  val nickname: String,
  val totalSpentCents: Long = 0L,
  val orderCount: Int = 0,
  val averageOrderValueCents: Long = 0L,
) {
  val totalSpent: BigDecimal get() = MoneyUtils.fromCents(totalSpentCents)
  val averageOrderValue: BigDecimal get() = MoneyUtils.fromCents(averageOrderValueCents)
}

/**
 * 地区销售DTO
 */
data class RegionalSalesDTO(
  val region: String,
  val orderCount: Int = 0,
  val revenueCents: Long = 0L,
  val averageOrderValueCents: Long = 0L,
) {
  val revenue: BigDecimal get() = MoneyUtils.fromCents(revenueCents)
  val averageOrderValue: BigDecimal get() = MoneyUtils.fromCents(averageOrderValueCents)
}

/**
 * 实时统计DTO
 */
data class RealTimeStatisticsDTO(
  val onlineUsers: Int = 0,
  val activeOrders: Int = 0,
  val pendingPayments: Int = 0,
  val deliveriesInProgress: Int = 0,
  val todayOrders: Int = 0,
  val todayRevenueCents: Long = 0L,
  val timestamp: LocalDateTime = LocalDateTime.now(),
) {
  val todayRevenue: BigDecimal get() = MoneyUtils.fromCents(todayRevenueCents)
}

/**
 * 配送员效率DTO
 */
data class WorkerEfficiencyDTO(
  val workerId: Long,
  val nickname: String,
  val totalDeliveries: Int = 0,
  val averageDeliveryTime: Int = 0,
  val successRate: BigDecimal = BigDecimal.ZERO, // Percentage, keep as BigDecimal
  val totalDistance: BigDecimal = BigDecimal.ZERO, // Distance, keep as BigDecimal
  val totalEarningsCents: Long = 0L,
) {
  val totalEarnings: BigDecimal get() = MoneyUtils.fromCents(totalEarningsCents)
}

/**
 * 支付方式统计DTO
 */
data class PaymentMethodStatsDTO(
  val paymentMethod: PaymentMethod,
  val count: Int = 0,
  val amountCents: Long = 0L,
  val percentage: BigDecimal = BigDecimal.ZERO, // Percentage, keep as BigDecimal
) {
  val amount: BigDecimal get() = MoneyUtils.fromCents(amountCents)
}

/**
 * 趋势数据DTO
 */
data class TrendDataDTO(
  val date: String,
  val valueCents: Long = 0L,
  val changeRate: BigDecimal = BigDecimal.ZERO, // Percentage, keep as BigDecimal
) {
  val value: BigDecimal get() = MoneyUtils.fromCents(valueCents)
}
