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

package dev.yidafu.aqua.api.service.delivery

import dev.yidafu.aqua.common.domain.model.DeliveryWorkerModel
import dev.yidafu.aqua.common.domain.model.OrderModel

/**
 * 配送员查询服务接口
 */
interface DeliveryWorkerQueryService {
  /**
   * 根据ID获取配送员
   */
  fun getWorkerById(workerId: Long): DeliveryWorkerModel

  /**
   * 获取所有配送员
   */
  fun getAllWorkers(): List<DeliveryWorkerModel>

  /**
   * 获取在线配送员
   */
  fun getOnlineWorkers(): List<DeliveryWorkerModel>

  /**
   * 获取配送员的进行中任务
   */
  fun getWorkerActiveTasks(adminId: Long): List<OrderModel>

  /**
   * 获取配送员的活跃任务数量
   */
  fun getWorkerActiveTaskCount(adminId: Long): Int
}
