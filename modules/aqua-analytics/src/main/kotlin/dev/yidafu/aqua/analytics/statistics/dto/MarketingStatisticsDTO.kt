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

package dev.yidafu.aqua.analytics.statistics.dto

/**
 * 营销统计 DTO(PRD 08-statistics.md §F-STA-007)
 * 第 3 层日汇总中间表(daily_marketing_facts / daily_marketing_usage_facts)查询侧两段合并结果:
 * 覆盖历史段读中间表,未覆盖日期(今日/缺口)实时聚合兜底,口径一致。
 */
data class MarketingStatisticsDTO(
  val summary: MarketingStatisticsSummaryDTO,
  val daily: List<MarketingDailyStatisticDTO>,
  val breakdown: List<MarketingUsageBreakdownRowDTO>,
)
