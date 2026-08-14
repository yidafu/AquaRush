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

package dev.yidafu.aqua.marketing.flashsale.domain.exception

/**
 * 限时特价活动异常(PRD 10-marketing.md §F-MKT-005)
 */
class FlashSaleActivityNotFoundException(
  activityId: Long,
) : RuntimeException("限时特价活动不存在: $activityId")

/**
 * 特价配置校验异常(特价低于目录价、时间窗、同商品时间窗重叠 R-MKT-05)
 */
class InvalidFlashSaleActivityException(
  message: String,
) : RuntimeException(message)
