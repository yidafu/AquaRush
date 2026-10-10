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

import dev.yidafu.aqua.analytics.statistics.dto.LoginStatisticsDTO
import dev.yidafu.aqua.analytics.statistics.dto.OrderUserDistributionDTO
import dev.yidafu.aqua.analytics.statistics.dto.UserStatisticsDTO
import dev.yidafu.aqua.common.graphql.generated.DailyStatisticVo
import java.time.LocalDate

/**
 * 用户统计服务接口
 */
interface UserStatisticsService {
  /**
   * 获取用户统计数据
   */
  fun getUserStatistics(): UserStatisticsDTO

  /**
   * 获取每日新增用户趋势
   */
  fun getDailyNewUsers(
    startDate: LocalDate,
    endDate: LocalDate,
  ): List<DailyStatisticVo>

  /**
   * 获取用户登录统计
   */
  fun getLoginStatistics(
    startDate: LocalDate,
    endDate: LocalDate,
  ): LoginStatisticsDTO

  /**
   * 获取下单用户分布(F-STA-003,G35):近30天下单用户去重,按累计下单次数分层,
   * 下单用户占比 = 下单用户数/总用户数,复购率 = 下单≥2次用户数/下单用户数
   */
  fun getOrderUserDistribution(
    startDate: LocalDate,
    endDate: LocalDate,
  ): OrderUserDistributionDTO
}
