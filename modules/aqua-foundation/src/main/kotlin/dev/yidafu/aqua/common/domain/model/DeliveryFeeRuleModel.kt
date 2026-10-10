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
 * 配送费规则配置实体(单行配置,固定 id = 1)
 * 与 PRD 12-operations-config.md §F-OPS-005 / 04-order.md §R-MON-02 保持一致(楼层口径):
 * 免费楼层 + 楼层分档加价(tiers,2026-10 分档修订);surchargeCents 保留为无档位时的兜底单档。
 * 未配置时按默认规则(免费4楼 + 默认三档,见 [DEFAULT_TIERS])。金额以分存储与服务端计算(R-MON-01)。
 */
open class DeliveryFeeRuleModel(
  var id: Long? = null,
  var freeFloorLimit: Int = DEFAULT_FREE_FLOOR_LIMIT,
  var surchargeCents: Long = DEFAULT_SURCHARGE_CENTS,
  var tiers: List<DeliveryFeeTier>? = DEFAULT_TIERS,
  override var deletedAt: LocalDateTime? = null,
  override var deletedBy: Long? = null,
  val createdAt: LocalDateTime = LocalDateTime.now(),
  var updatedAt: LocalDateTime = LocalDateTime.now(),
) : SoftDeletable {
  companion object {
    /** 默认免费楼层:4楼及以下免运费 */
    const val DEFAULT_FREE_FLOOR_LIMIT = 4

    /** 默认兜底单档加价:2元(仅 tiers 为空时使用) */
    const val DEFAULT_SURCHARGE_CENTS = 200L

    /** 默认档位(R-MON-02 默认示例:免费楼层以上 2 层内 +2 元 / 再上 3 层 +5 元 / 更高 +8 元) */
    val DEFAULT_TIERS =
      listOf(
        DeliveryFeeTier(fromFloor = 5, toFloor = 6, feeCents = 200L),
        DeliveryFeeTier(fromFloor = 7, toFloor = 9, feeCents = 500L),
        DeliveryFeeTier(fromFloor = 10, toFloor = null, feeCents = 800L),
      )
  }
}
