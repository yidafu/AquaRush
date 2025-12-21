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

package dev.yidafu.aqua.statistics.controller

import dev.yidafu.aqua.api.service.StatisticsService
import dev.yidafu.aqua.common.ApiResponse
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/statistics")
class StatisticsController(
  private val statisticsService: StatisticsService,
) {
  @GetMapping("/orders")
  fun getOrderStatistics(
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
  ): ApiResponse<StatisticsService.OrderStatistics> {
    val statistics = statisticsService.getOrderStatistics(startDate, endDate)
    return ApiResponse.success(statistics)
  }

  @GetMapping("/daily")
  fun getDailyStatistics(
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
  ): ApiResponse<List<StatisticsService.DailyStatistics>> {
    val statistics = statisticsService.getDailyStatistics(startDate, endDate)
    return ApiResponse.success(statistics)
  }

  @GetMapping("/weekly")
  fun getWeeklyStatistics(
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
  ): ApiResponse<List<StatisticsService.DailyStatistics>> {
    val statistics = statisticsService.getWeeklyStatistics(startDate, endDate)
    return ApiResponse.success(statistics)
  }

  @GetMapping("/monthly")
  fun getMonthlyStatistics(
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
  ): ApiResponse<List<StatisticsService.DailyStatistics>> {
    val statistics = statisticsService.getMonthlyStatistics(startDate, endDate)
    return ApiResponse.success(statistics)
  }
}
