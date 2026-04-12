package dev.yidafu.aqua.statistics.mapper

import dev.yidafu.aqua.statistics.dto.DailyStatisticsDTO
import dev.yidafu.aqua.statistics.dto.OrderStatisticsDTO
import dev.yidafu.aqua.statistics.service.StatisticsService
import tech.mappie.api.ObjectMappie
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import dev.yidafu.aqua.common.graphql.generated.DailyStatistic as DailyStatisticG
import dev.yidafu.aqua.common.graphql.generated.MonthlyStatistic as MonthlyStatisticG
import dev.yidafu.aqua.common.graphql.generated.OrderStatistics as OrderStatisticsG
import dev.yidafu.aqua.common.graphql.generated.WeeklyStatistic as WeeklyStatisticG

private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

object DailyStatisticMapper : ObjectMappie<DailyStatisticsDTO, DailyStatisticG>() {
  override fun map(from: DailyStatisticsDTO): DailyStatisticG =
    mapping {
      to::revenue fromValue (from.totalAmountCents.toDouble() / 100.0).toFloat()
      to::date fromValue formatter.format(from.date)
      to::orderCount fromValue from.orderCount.toInt()
      to::orderProductCount fromValue from.orderProductCount.toInt()
    }
}

object OrderStatisticsMapper : ObjectMappie<OrderStatisticsDTO, OrderStatisticsG>() {
  override fun map(from: OrderStatisticsDTO): OrderStatisticsG =
    mapping {
      to::totalOrders fromValue from.totalOrders.toInt()
      to::totalRevenue fromValue from.totalAmountCents
      to::averageOrderValue fromValue if (from.totalOrders > 0) from.totalAmountCents / from.totalOrders else 0L
      to::dateRange fromValue
        dev.yidafu.aqua.common.graphql.generated.DateRange(
          startDate = LocalDateTime.now(),
          endDate = LocalDateTime.now(),
        )
    }
}

object WeeklyStatisticMapper : ObjectMappie<DailyStatisticsDTO, WeeklyStatisticG>() {
  override fun map(from: DailyStatisticsDTO): WeeklyStatisticG =
    mapping {
      to::weekNumber fromValue from.date.dayOfWeek.value
      to::year fromValue from.date.year
      to::startDate fromValue formatter.format(from.date)
      to::endDate fromValue formatter.format(from.date.plusDays(6))
      to::orderCount fromValue from.orderCount.toInt()
      to::orderProductCount fromValue from.orderProductCount.toInt()
      to::revenue fromValue (from.totalAmountCents.toDouble() / 100.0).toFloat()
    }
}

object MonthlyStatisticMapper : ObjectMappie<DailyStatisticsDTO, MonthlyStatisticG>() {
  override fun map(from: DailyStatisticsDTO): MonthlyStatisticG =
    mapping {
      to::month fromValue from.date.monthValue
      to::year fromValue from.date.year
      to::monthName fromValue from.date.month.name
      to::orderCount fromValue from.orderCount.toInt()
      to::orderProductCount fromValue from.orderProductCount.toInt()
      to::revenue fromValue (from.totalAmountCents.toDouble() / 100.0).toFloat()
    }
}
