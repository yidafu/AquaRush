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

package dev.yidafu.aqua.api.service

import dev.yidafu.aqua.api.dto.DeliveryStatisticsDTO
import dev.yidafu.aqua.api.dto.TodayStatisticsDTO
import dev.yidafu.aqua.api.dto.WeekStatisticsDTO
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus

/**
 * 配送任务查询服务接口
 */
interface DeliveryOrderQueryService {
  /**
   * 获取配送员的所有任务
   */
  fun getWorkerTasks(workerId: Long): List<OrderModel>

  /**
   * 获取配送员的所有任务
   */
  fun getOrdersByStatus(
    workerId: Long,
    status: OrderModelStatus,
  ): List<OrderModel>

  /**
   * 获取所有待分配的订单
   */
  fun getPendingDeliveryOrders(): List<OrderModel>

  /**
   * 获取配送员的已接单未开始配送的订单
   */
  fun getAssignedOrders(adminId: Long): List<OrderModel>

  /**
   * 获取配送统计数据
   */
  fun getDeliveryStatistics(): DeliveryStatisticsDTO

  /**
   * 获取配送员当日统计数据
   * @param workerId 配送员ID，如果为null则返回所有配送员的统计数据
   */
  fun getTodayStatistics(workerId: Long?): TodayStatisticsDTO

  /**
   * 获取配送员一周统计数据
   * @param workerId 配送员ID，如果为null则返回所有配送员的统计数据
   */
  fun getWeekStatistics(workerId: Long?): WeekStatisticsDTO

  /**
   * 根据ID获取订单
   */
  fun getOrderById(orderId: Long): OrderModel
}
