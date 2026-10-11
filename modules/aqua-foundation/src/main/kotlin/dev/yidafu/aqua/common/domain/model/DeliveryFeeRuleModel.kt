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
 * R-OPS-03(G96)扩充:起送门槛 [minimumOrderCents](0=不启用)、距离阶梯运费 [distanceTiers]
 * (与楼层分档互补、叠加计算,基准坐标 [storeLongitude]/[storeLatitude],未配置不启用距离计费)。
 * 未配置时按默认规则(免费4楼 + 默认三档,见 [DEFAULT_TIERS])。金额以分存储与服务端计算(R-MON-01)。
 */
open class DeliveryFeeRuleModel(
  var id: Long? = null,
  var freeFloorLimit: Int = DEFAULT_FREE_FLOOR_LIMIT,
  var surchargeCents: Long = DEFAULT_SURCHARGE_CENTS,
  var tiers: List<DeliveryFeeTier>? = DEFAULT_TIERS,
  var minimumOrderCents: Long = DEFAULT_MINIMUM_ORDER_CENTS,
  var distanceTiers: List<DeliveryDistanceTier>? = null,
  var storeLongitude: Double? = null,
  var storeLatitude: Double? = null,
  override var deletedAt: LocalDateTime? = null,
  override var deletedBy: Long? = null,
  val createdAt: LocalDateTime = LocalDateTime.now(),
  var updatedAt: LocalDateTime = LocalDateTime.now(),
) : SoftDeletable {
  /**
   * 楼层配送费计算(R-MON-02 分档口径,2026-10 修订):
   * 自提免运费;楼层为空(平房)或 ≤ 免费楼层免运费;超免费楼层按 tiers 命中,tiers 缺失回退 surchargeCents。
   * 下单(OrderMutationServiceImpl)与确认订单预览(ClientOrderPreviewResolver)必须共用本方法,
   * 保证预览金额与实付一致;规则配置变更即时生效,已生成订单按下单快照。
   */
  fun feeCentsForFloor(
    floor: Int?,
    isSelfCollect: Boolean,
  ): Long {
    if (isSelfCollect) return 0
    if (floor == null || floor <= freeFloorLimit) return 0
    val tier = tiers?.firstOrNull { t -> floor >= t.fromFloor && (t.toFloor == null || floor <= t.toFloor!!) }
    return tier?.feeCents ?: surchargeCents
  }

  /**
   * 配送费合计(R-MON-02 + R-OPS-03):楼层分档 + 距离阶梯**叠加计算**;
   * 自提免运费;距离阶梯仅在门店坐标与收货地址坐标均配置时生效,否则距离档不加收。
   * 下单与确认订单预览必须共用本方法(预览金额 = 实付),费用总额落 orders.delivery_fee_cents 快照。
   */
  fun feeCentsForDelivery(
    floor: Int?,
    addressLongitude: Double?,
    addressLatitude: Double?,
    isSelfCollect: Boolean,
  ): Long {
    if (isSelfCollect) return 0
    return feeCentsForFloor(floor, false) + feeCentsForDistance(addressLongitude, addressLatitude)
  }

  /**
   * 距离阶梯加价(R-OPS-03):门店坐标或地址坐标缺失 → 0(不加收);
   * 直线距离(Haversine)落入首个 upToKm 档位取其加价,超出全部有界档位且无不封顶档 → 0。
   */
  fun feeCentsForDistance(
    addressLongitude: Double?,
    addressLatitude: Double?,
  ): Long {
    val tiers = distanceTiers?.takeIf { it.isNotEmpty() } ?: return 0
    val storeLng = storeLongitude ?: return 0
    val storeLat = storeLatitude ?: return 0
    if (addressLongitude == null || addressLatitude == null) return 0
    val km = haversineKm(storeLat, storeLng, addressLatitude, addressLongitude)
    return tiers.firstOrNull { it.upToKm == null || km <= it.upToKm }?.feeCents ?: 0L
  }

  /**
   * 起送门槛判定(R-OPS-03):[minimumOrderCents] = 0 不启用;
   * 判定口径 = 订单商品金额(成交单价 × 数量小计,优惠前,与确认订单页「商品金额」行一致)。
   */
  fun belowMinimumOrder(productAmountCents: Long): Boolean = minimumOrderCents > 0 && productAmountCents < minimumOrderCents

  /** 两点球面直线距离(km,Haversine,地球平均半径 6371km) */
  private fun haversineKm(
    lat1: Double,
    lng1: Double,
    lat2: Double,
    lng2: Double,
  ): Double {
    val rad = Math.PI / 180.0
    val dLat = (lat2 - lat1) * rad
    val dLng = (lng2 - lng1) * rad
    val a =
      Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos(lat1 * rad) * Math.cos(lat2 * rad) * Math.sin(dLng / 2) * Math.sin(dLng / 2)
    return 6371.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
  }

  companion object {
    /** 默认免费楼层:4楼及以下免运费 */
    const val DEFAULT_FREE_FLOOR_LIMIT = 4

    /** 默认兜底单档加价:2元(仅 tiers 为空时使用) */
    const val DEFAULT_SURCHARGE_CENTS = 200L

    /** 默认起送门槛:0 = 不启用(R-OPS-03) */
    const val DEFAULT_MINIMUM_ORDER_CENTS = 0L

    /** 默认档位(R-MON-02 默认示例:免费楼层以上 2 层内 +2 元 / 再上 3 层 +5 元 / 更高 +8 元) */
    val DEFAULT_TIERS =
      listOf(
        DeliveryFeeTier(fromFloor = 5, toFloor = 6, feeCents = 200L),
        DeliveryFeeTier(fromFloor = 7, toFloor = 9, feeCents = 500L),
        DeliveryFeeTier(fromFloor = 10, toFloor = null, feeCents = 800L),
      )
  }
}
