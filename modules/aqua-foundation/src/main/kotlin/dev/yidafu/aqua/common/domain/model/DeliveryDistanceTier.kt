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

/**
 * 距离阶梯运费档位(R-OPS-03 距离阶梯运费,与楼层分档互补、叠加计算):
 * 收货地址与门店直线距离 ≤ [upToKm] 时加收 [feeCents](分);[upToKm] = null 表示不封顶(必须为最后一档)。
 * 档位按距离上限升序、区间不重叠(服务层保存时校验);空列表 = 不启用距离计费。
 */
data class DeliveryDistanceTier(
  val upToKm: Double?,
  val feeCents: Long,
)
