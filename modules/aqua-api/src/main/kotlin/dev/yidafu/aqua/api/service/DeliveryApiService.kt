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

import dev.yidafu.aqua.api.common.PagedResponse
import dev.yidafu.aqua.api.dto.*
import dev.yidafu.aqua.common.domain.model.DeliverWorkerStatus
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 配送API服务接口
 */
interface DeliveryApiService {
  /**
   * 注册配送员
   */
  fun registerDeliveryWorker(request: CreateDeliveryWorkerRequest): DeliveryWorkerDTO

  /**
   * 更新配送员位置
   */
  fun updateWorkerLocation(
    workerId: Long,
    latitude: BigDecimal,
    longitude: BigDecimal,
  ): DeliveryWorkerDTO

  /**
   * 获取配送员信息
   */
  fun getDeliveryWorker(workerId: Long): DeliveryWorkerDTO?

  /**
   * 获取配送员列表
   */
  fun getDeliveryWorkers(
    page: Int = 0,
    size: Int = 20,
  ): PagedResponse<DeliveryWorkerDTO>

  /**
   * 获取附近可用的配送员
   */
  fun getAvailableWorkers(
    latitude: BigDecimal,
    longitude: BigDecimal,
    radiusKm: Int = 3,
  ): List<DeliveryWorkerDTO>

  /**
   * 更新配送员工作状态
   */
  fun updateWorkerStatus(
      workerId: Long,
      status: DeliverWorkerStatus,
  ): DeliveryWorkerDTO

  /**
   * 配送员接单
   */
  fun acceptOrder(
    workerId: Long,
    orderId: Long,
  ): OrderDTO

  /**
   * 配送员拒单
   */
  fun rejectOrder(
    workerId: Long,
    orderId: Long,
    reason: String,
  ): OrderDTO

  /**
   * 更新订单配送状态
   */
  fun updateDeliveryStatus(
    orderId: Long,
    status: DeliveryStatus,
    estimatedTime: LocalDateTime? = null,
  ): OrderDTO

  /**
   * 确认订单送达
   */
  fun confirmDelivery(
    orderId: Long,
    deliveryProof: String? = null,
  ): OrderDTO

  /**
   * 获取配送员当前订单
   */
  fun getCurrentOrders(workerId: Long): List<OrderDTO>

  /**
   * 获取配送员历史订单
   */
  fun getOrderHistory(
    workerId: Long,
    page: Int = 0,
    size: Int = 20,
  ): PagedResponse<OrderDTO>

  /**
   * 获取配送统计信息
   */
  fun getDeliveryStatistics(workerId: Long): DeliveryStatisticsDTO

  /**
   * 计算配送费用
   */
  fun calculateDeliveryFee(
    distanceKm: BigDecimal,
    orderAmount: BigDecimal,
  ): BigDecimal

  /**
   * 获取配送员轨迹
   */
  fun getDeliveryRoute(orderId: Long): List<LocationDTO>

  /**
   * 批量分配配送员
   */
  fun batchAssignWorkers(orderIds: List<Long>): Map<Long, DeliveryWorkerDTO>

  /**
   * 检查配送超时
   */
  fun checkDeliveryTimeout(): List<OrderDTO>
}

/**
 * 配送统计DTO
 */
data class DeliveryStatisticsDTO(
  val totalDeliveries: Int = 0,
  val successfulDeliveries: Int = 0,
  val failedDeliveries: Int = 0,
  val averageDeliveryTime: Int = 0, // 分钟
  val totalDistance: BigDecimal = BigDecimal.ZERO, // 公里
  val averageRating: BigDecimal = BigDecimal.ZERO,
  val totalEarnings: BigDecimal = BigDecimal.ZERO,
)

/**
 * 位置DTO
 */
data class LocationDTO(
  val latitude: BigDecimal,
  val longitude: BigDecimal,
  val timestamp: LocalDateTime,
)
