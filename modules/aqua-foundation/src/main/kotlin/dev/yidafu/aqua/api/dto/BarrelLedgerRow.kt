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
 * 桶账台账行(R-PRO-05/F-PRO-012):按客户维度实时聚合
 * 在租/已交付/已回收/欠桶/押金合计(分)。
 */
data class BarrelLedgerRow(
  val userId: Long,
  val nickname: String?,
  val phone: String?,
  val heldBucketCount: Long,
  val depositHeldCents: Long,
  val depositRefundedCents: Long,
  val deliveredBucketCount: Long,
  val recoveredBucketCount: Long,
  val owedBucketCount: Long,
)
