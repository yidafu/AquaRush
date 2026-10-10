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

package dev.yidafu.aqua.marketing.barrel.domain.repository

import dev.yidafu.aqua.common.domain.model.BarrelRecoveryRecordModel

/**
 * 空桶回收登记仓储(F-ORD-011/R-MON-03)。
 * 送水工完成配送时按规格登记,非强制;欠桶 = 已交付 − 已回收(R-PRO-04)。
 */
interface BarrelRecoveryRecordRepository {
  fun save(entity: BarrelRecoveryRecordModel): BarrelRecoveryRecordModel

  fun listByOrderId(orderId: Long): List<BarrelRecoveryRecordModel>
}
