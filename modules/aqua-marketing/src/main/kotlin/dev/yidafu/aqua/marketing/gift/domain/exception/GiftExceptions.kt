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

package dev.yidafu.aqua.marketing.gift.domain.exception

/**
 * 买赠/满赠活动异常(PRD 10-marketing.md §F-MKT-008)
 */
class GiftActivityNotFoundException(
  activityId: Long,
) : RuntimeException("买赠/满赠活动不存在: $activityId")

/**
 * 活动配置校验异常(门槛/范围/赠品 R-MKT-09)
 */
class InvalidGiftActivityException(
  message: String,
) : RuntimeException(message)
