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

import dev.yidafu.aqua.common.domain.model.enums.DepositPriceStatus

/**
 * 押金价目新增/编辑入参(R-MON-03,设计02 §5.1 upsertDepositPrice):
 * id 空=新增;规格唯一;押金 0..9999900 分(0~99999 元)。
 */
data class UpsertDepositPriceRequest(
  val id: Long? = null,
  val specification: String,
  val depositCents: Long,
  val status: DepositPriceStatus = DepositPriceStatus.ENABLED,
  val sortOrder: Int = 999,
)
