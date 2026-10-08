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

package dev.yidafu.aqua.ops.banner.domain.exception

/**
 * 轮播图未找到异常
 */
class BannerNotFoundException(
  bannerId: Long,
) : RuntimeException("Banner not found with ID: $bannerId")

/**
 * 排序号重复异常(R-OPS-01:排序号唯一)
 */
class DuplicateSortOrderException(
  sortOrder: Int,
) : RuntimeException("Duplicate sort order: $sortOrder")

/**
 * 时间窗非法异常(开始时间必须早于结束时间)
 */
class InvalidTimeWindowException(
  message: String,
) : RuntimeException(message)
