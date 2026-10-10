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

import dev.yidafu.aqua.common.domain.model.enums.DepositPriceStatus
import java.time.LocalDateTime

/**
 * 水桶费价目(G06:一次性塑料水桶费——独立价目表,按规格配置,与商品解耦):
 * 用户端「买水桶」按启用价目选购,独立支付购桶(与商品订单不捆绑);价目停用不影响已生成购桶单。
 */
open class DepositPriceModel(
  var id: Long? = null,
  var specification: String = "",
  var priceCents: Long = 0,
  var status: DepositPriceStatus = DepositPriceStatus.ENABLED,
  var sortOrder: Int = 999,
  val createdAt: LocalDateTime = LocalDateTime.now(),
  var updatedAt: LocalDateTime = LocalDateTime.now(),
  override var deletedAt: LocalDateTime? = null,
  override var deletedBy: Long? = null,
) : SoftDeletable
