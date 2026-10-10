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
 * 水桶账户按规格明细(R-PRO-04/F-PRO-010):
 * 在租桶数 = HELD 押金记录桶数;欠桶 = 已交付 − 已回收;押金按在租口径。
 */
data class BarrelAccountSpec(
  val specification: String,
  val heldBucketCount: Int,
  val owedBucketCount: Int,
  val depositHeldCents: Long,
)
