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

package dev.yidafu.aqua.statistics.service.impl

import com.querydsl.jpa.impl.JPAQueryFactory
import dev.yidafu.aqua.statistics.dto.DeliveryWorkerRankingItemDTO
import dev.yidafu.aqua.statistics.dto.DeliveryWorkerStatisticsDTO
import dev.yidafu.aqua.statistics.service.DeliveryWorkerStatisticsService
import dev.yidafu.aqua.common.domain.model.QOrderModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.delivery.domain.repository.DeliveryWorkerRepository
import dev.yidafu.aqua.review.domain.repository.DeliveryWorkerStatisticsRepository
import dev.yidafu.aqua.statistics.model.repository.StatisticsDeliveryWorkerRepositoryCustom
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class DeliveryWorkerStatisticsServiceImpl(
  private val deliveryWorkerRepository: DeliveryWorkerRepository,
  private val deliveryWorkerStatisticsRepository: DeliveryWorkerStatisticsRepository,
  private val statisticsDeliveryWorkerRepository: StatisticsDeliveryWorkerRepositoryCustom,
) : DeliveryWorkerStatisticsService {
  @PersistenceContext
  private lateinit var entityManager: EntityManager

  private val queryFactory: JPAQueryFactory by lazy {
    JPAQueryFactory(entityManager)
  }

  override fun getDeliveryWorkerStatistics(): DeliveryWorkerStatisticsDTO {
    // 送水员总数
    val totalWorkers = deliveryWorkerRepository.count()

    // 今日在岗送水员数
    val today = LocalDate.now()
    val todayStart = today.atStartOfDay()
    val todayActiveWorkers = statisticsDeliveryWorkerRepository.countTodayActiveWorkers(todayStart)

    // 今日配送中订单数
    val deliveringOrders = statisticsDeliveryWorkerRepository.countDeliveringOrders()

    // 今日已完成订单数
    val todayCompletedOrders = statisticsDeliveryWorkerRepository.countCompletedOrdersSince(todayStart)

    return DeliveryWorkerStatisticsDTO(
      totalWorkers = totalWorkers,
      todayActiveWorkers = todayActiveWorkers,
      deliveringOrders = deliveringOrders,
      todayCompletedOrders = todayCompletedOrders,
    )
  }

  override fun getDeliveryWorkerRanking(limit: Int): List<DeliveryWorkerRankingItemDTO> {
    val orderModel = QOrderModel.orderModel

    val today = LocalDate.now()
    val todayStart = today.atStartOfDay()

    // Get today's completed order counts per worker
    val todayOrderCounts =
      queryFactory
        .select(orderModel.deliveryWorkerId, orderModel.count())
        .from(orderModel)
        .where(
          orderModel.status.eq(OrderModelStatus.COMPLETED),
          orderModel.updatedAt.goe(todayStart),
          orderModel.deliveryWorkerId.isNotNull,
        ).groupBy(orderModel.deliveryWorkerId)
        .fetch()
        .associate { it.get(orderModel.deliveryWorkerId) to it.get(orderModel.count())?.toInt() }

    // Get all workers and their statistics
    val workers = deliveryWorkerRepository.findAll()

    return workers
      .map { worker ->
        val stats = deliveryWorkerStatisticsRepository.findByDeliveryWorkerId(worker.id!!)
        DeliveryWorkerRankingItemDTO(
          workerId = worker.id!!,
          name = worker.name,
          todayCompletedOrders = todayOrderCounts[worker.id] ?: 0,
          rating = stats?.averageRating?.toFloat() ?: 0f,
          totalEarnings = worker.earningCents ?: 0L,
        )
      }.sortedByDescending { it.todayCompletedOrders }
      .take(limit)
  }
}
