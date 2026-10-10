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

package dev.yidafu.aqua.common.domain.model

import java.time.LocalDateTime

/**
 * 空桶回收登记实体(F-ORD-011/R-MON-03):送水工完成配送时按规格登记回收空桶,
 * 非强制;欠桶数 = Σ 已交付该规格桶数(已完成订单 items)− Σ 回收登记桶数(R-PRO-04)。
 */
data class BarrelRecoveryRecordModel(
  var id: Long? = null,
  // 登记送水工(账号 id)
  var workerId: Long,
  // 关联完成配送订单
  var orderId: Long,
  var specification: String,
  var bucketCount: Int,
  val createdAt: LocalDateTime = LocalDateTime.now(),
  var updatedAt: LocalDateTime = LocalDateTime.now(),
  override var deletedAt: LocalDateTime? = null,
  override var deletedBy: Long? = null,
) : SoftDeletable
