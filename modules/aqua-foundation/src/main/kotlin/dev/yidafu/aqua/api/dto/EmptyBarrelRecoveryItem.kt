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

package dev.yidafu.aqua.api.dto

/**
 * 空桶回收登记行(F-ORD-011/R-MON-03):完成配送时按规格登记回收空桶数,
 * 欠桶 = 已交付 − 已回收按规格口径(R-PRO-04);非强制,可为空列表。
 */
data class EmptyBarrelRecoveryItem(
  val specification: String,
  val count: Int,
)
