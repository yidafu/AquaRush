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

package dev.yidafu.aqua.marketing.firstorder.domain.exception

/**
 * 首单立减/新人专享价活动异常(PRD 10-marketing.md §F-MKT-009)
 */
class FirstOrderActivityNotFoundException(
  activityId: Long,
) : RuntimeException("首单/新人优惠活动不存在: $activityId")

/**
 * 活动配置校验异常(类型字段/新人价低于目录价/重叠 R-MKT-10)
 */
class InvalidFirstOrderActivityException(
  message: String,
) : RuntimeException(message)
