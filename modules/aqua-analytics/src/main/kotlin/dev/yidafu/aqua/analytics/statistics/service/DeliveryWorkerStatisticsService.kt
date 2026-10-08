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

package dev.yidafu.aqua.analytics.statistics.service

import dev.yidafu.aqua.analytics.statistics.dto.DeliveryWorkerRankingItemDTO
import dev.yidafu.aqua.analytics.statistics.dto.DeliveryWorkerStatisticsDTO

/**
 * 送水员统计服务接口
 */
interface DeliveryWorkerStatisticsService {
  /**
   * 获取送水员统计数据
   */
  fun getDeliveryWorkerStatistics(): DeliveryWorkerStatisticsDTO

  /**
   * 获取送水员排行榜
   */
  fun getDeliveryWorkerRanking(limit: Int): List<DeliveryWorkerRankingItemDTO>
}
