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

import dev.yidafu.aqua.common.domain.model.DepositRecordModel
import org.springframework.data.domain.Page

/**
 * 水桶账户(F-PRO-010/R-PRO-04):按规格在租/欠桶/押金合计 + 押金记录分页。
 */
data class BarrelAccount(
  val specs: List<BarrelAccountSpec>,
  // 押金合计(在租,分)
  val depositHeldCents: Long,
  // 欠桶合计
  val owedBucketCount: Int,
  // 押金记录分页(最近在前)
  val records: Page<DepositRecordModel>,
)
