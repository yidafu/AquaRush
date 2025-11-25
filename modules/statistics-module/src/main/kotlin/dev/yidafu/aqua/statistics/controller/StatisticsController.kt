package dev.yidafu.aqua.statistics.controller

import dev.yidafu.aqua.common.ApiResponse
import dev.yidafu.aqua.statistics.service.StatisticsService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/statistics")
class StatisticsController(
    private val statisticsService: StatisticsService
) {
    
    @GetMapping("/orders")
    fun getOrderStatistics(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): ApiResponse<StatisticsService.OrderStatistics> {
        val statistics = statisticsService.getOrderStatistics(startDate, endDate)
        return ApiResponse.success(statistics)
    }
    
    @GetMapping("/daily")
    fun getDailyStatistics(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): ApiResponse<List<StatisticsService.DailyStatistics>> {
        val statistics = statisticsService.getDailyStatistics(startDate, endDate)
        return ApiResponse.success(statistics)
    }
    
    @GetMapping("/weekly")
    fun getWeeklyStatistics(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): ApiResponse<List<StatisticsService.DailyStatistics>> {
        val statistics = statisticsService.getWeeklyStatistics(startDate, endDate)
        return ApiResponse.success(statistics)
    }
    
    @GetMapping("/monthly")
    fun getMonthlyStatistics(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): ApiResponse<List<StatisticsService.DailyStatistics>> {
        val statistics = statisticsService.getMonthlyStatistics(startDate, endDate)
        return ApiResponse.success(statistics)
    }
}
