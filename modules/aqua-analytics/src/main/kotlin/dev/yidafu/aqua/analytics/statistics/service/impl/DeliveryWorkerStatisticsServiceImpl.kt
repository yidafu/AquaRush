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

package dev.yidafu.aqua.analytics.statistics.service.impl

import dev.yidafu.aqua.analytics.statistics.domain.repository.StatisticsDeliveryWorkerRepositoryCustom
import dev.yidafu.aqua.analytics.statistics.dto.DeliveryWorkerRankingItemDTO
import dev.yidafu.aqua.analytics.statistics.dto.DeliveryWorkerStatisticsDTO
import dev.yidafu.aqua.analytics.statistics.service.DeliveryWorkerStatisticsService
import dev.yidafu.aqua.api.service.admin.AdminService
import dev.yidafu.aqua.api.service.delivery.DeliveryWorkerStatisticsQueryApiService
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDate

@Service
class DeliveryWorkerStatisticsServiceImpl(
  private val adminService: AdminService,
  private val deliveryWorkerStatisticsQueryApiService: DeliveryWorkerStatisticsQueryApiService,
  private val statisticsDeliveryWorkerRepository: StatisticsDeliveryWorkerRepositoryCustom,
) : DeliveryWorkerStatisticsService {
  override fun getDeliveryWorkerStatistics(): DeliveryWorkerStatisticsDTO {
    // 送水员总数(账号单表,配送能力角色条件 count,避免全表加载)
    val totalWorkers = adminService.countDeliveryCapable()

    // 今日在岗送水员数
    val today = LocalDate.now()
    val todayStart = today.atStartOfDay()
    val todayActiveWorkers = statisticsDeliveryWorkerRepository.countTodayActiveWorkers(todayStart)

    // 今日配送中订单数
    val deliveringOrders = statisticsDeliveryWorkerRepository.countDeliveringOrders()

    // 今日已完成订单数
    val todayCompletedOrders = statisticsDeliveryWorkerRepository.countCompletedOrdersSince(todayStart)

    // 人均单量 = 今日完成订单总数 / 今日在岗送水员数(F-STA-004,G36;在岗为 0 时为 null)
    val averageOrdersPerWorker =
      if (todayActiveWorkers > 0) todayCompletedOrders.toFloat() / todayActiveWorkers.toFloat() else null

    // 平均履约时长(分钟)= Σ(completedAt − 派单时间) / 今日完成订单数(F-STA-004,G36);
    // 派单时间 = 最早 DELIVERY_ASSIGNED 操作时间,回退 deliveryStartedAt(出发时间),再回退 createdAt;
    // 无今日完成订单时为 null
    val completedOrders = statisticsDeliveryWorkerRepository.findCompletedOrderTimingsSince(todayStart)
    val assignedAtByOrderId =
      statisticsDeliveryWorkerRepository.findEarliestAssignedAtByOrderIds(completedOrders.map { it.orderId })
    val averageFulfillmentMinutes =
      if (completedOrders.isEmpty()) {
        null
      } else {
        val totalSeconds =
          completedOrders.sumOf { timing ->
            val start =
              assignedAtByOrderId[timing.orderId]
                ?: timing.deliveryStartedAt
                ?: timing.createdAt
            Duration.between(start, timing.completedAt).seconds.coerceAtLeast(0L)
          }
        val averageSeconds = totalSeconds.toDouble() / completedOrders.size
        (averageSeconds / 60.0).toFloat()
      }

    return DeliveryWorkerStatisticsDTO(
      totalWorkers = totalWorkers.toLong(),
      todayActiveWorkers = todayActiveWorkers,
      deliveringOrders = deliveringOrders,
      todayCompletedOrders = todayCompletedOrders,
      averageOrdersPerWorker = averageOrdersPerWorker,
      averageFulfillmentMinutes = averageFulfillmentMinutes,
    )
  }

  override fun getDeliveryWorkerRanking(limit: Int): List<DeliveryWorkerRankingItemDTO> {
    val today = LocalDate.now()

    // 今日完成单数按送水员分组(业绩口径 R-ORD-11:待确认收货+已完成,按送达时间归属;
    // P3 迁移:原 QueryDSL 查询搬入 StatisticsDeliveryWorkerRepository)
    val todayOrderCounts =
      statisticsDeliveryWorkerRepository.countTodayCompletedOrdersByWorker(today.atStartOfDay())

    // Get all workers and their statistics(账号单表,配送能力角色)
    val workers = adminService.findAllDeliveryCapable()

    return workers
      .map { worker ->
        val stats = deliveryWorkerStatisticsQueryApiService.findByDeliveryWorkerId(worker.id!!)
        DeliveryWorkerRankingItemDTO(
          workerId = worker.id!!,
          name = worker.realName ?: worker.username,
          todayCompletedOrders = todayOrderCounts[worker.id] ?: 0,
          rating = stats?.averageRating?.toFloat() ?: 0f,
          totalEarnings = worker.earningCents ?: 0L,
        )
      }.sortedByDescending { it.todayCompletedOrders }
      .take(limit)
  }
}
