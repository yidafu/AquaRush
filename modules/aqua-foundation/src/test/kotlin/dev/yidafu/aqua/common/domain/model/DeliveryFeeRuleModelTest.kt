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

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * 配送费规则计费单元测试(R-MON-02 楼层分档 + R-OPS-03 起送门槛/距离阶梯,G96):
 * 楼层档命中/回退、距离直线距离档命中、两者叠加、自提豁免、坐标缺失降级、门槛判定边界。
 */
class DeliveryFeeRuleModelTest :
  FunSpec({
    // 门店坐标:深圳南山(约);地址 A 距门店约 1km(同点微偏),地址 B 约 4.4km(经度差 0.04°)
    val storeLng = 113.93
    val storeLat = 22.53
    val nearLng = 113.9304
    val nearLat = 22.5304
    val midLng = 113.97
    val midLat = 22.53

    fun rule(
      minimumOrderCents: Long = 0L,
      distanceTiers: List<DeliveryDistanceTier>? = null,
      storeLongitude: Double? = null,
      storeLatitude: Double? = null,
    ) = DeliveryFeeRuleModel(
      minimumOrderCents = minimumOrderCents,
      distanceTiers = distanceTiers,
      storeLongitude = storeLongitude,
      storeLatitude = storeLatitude,
    )

    val threeTierRule =
      rule(
        distanceTiers =
          listOf(
            DeliveryDistanceTier(upToKm = 3.0, feeCents = 0L),
            DeliveryDistanceTier(upToKm = 5.0, feeCents = 300L),
            DeliveryDistanceTier(upToKm = null, feeCents = 600L),
          ),
        storeLongitude = storeLng,
        storeLatitude = storeLat,
      )

    test("haversine distance sanity: near address ≈ 0.06km, mid address ≈ 4.0km") {
      threeTierRule.feeCentsForDistance(nearLng, nearLat) shouldBe 0L
      threeTierRule.feeCentsForDistance(midLng, midLat) shouldBe 300L
    }

    test("distance tier unbounded matches beyond last bounded tier") {
      // 经度差 0.1° ≈ 10.7km(赤道)× cos(22.5°) ≈ 9.8km → 落入不封顶档
      threeTierRule.feeCentsForDistance(storeLng + 0.1, storeLat) shouldBe 600L
    }

    test("missing store or address coordinates degrade to zero distance fee") {
      threeTierRule.feeCentsForDistance(null, nearLat) shouldBe 0L
      threeTierRule.feeCentsForDistance(nearLng, null) shouldBe 0L
      rule(distanceTiers = threeTierRule.distanceTiers).feeCentsForDistance(nearLng, nearLat) shouldBe 0L
      rule().feeCentsForDistance(nearLng, nearLat) shouldBe 0L
    }

    test("floor fee and distance fee stack (R-MON-02 + R-OPS-03)") {
      // 7 楼命中默认 7-9 档 +500;4.4km 命中 3-5km 档 +300 → 合计 800
      threeTierRule.feeCentsForDelivery(7, midLng, midLat, isSelfCollect = false) shouldBe 800L
      // 免费楼层内 + 近距离 → 0
      threeTierRule.feeCentsForDelivery(3, nearLng, nearLat, isSelfCollect = false) shouldBe 0L
      // 免费楼层内 + 中距离 → 仅距离档
      threeTierRule.feeCentsForDelivery(3, midLng, midLat, isSelfCollect = false) shouldBe 300L
      // 超免费楼层 + 近距离 → 仅楼层档
      threeTierRule.feeCentsForDelivery(12, nearLng, nearLat, isSelfCollect = false) shouldBe 800L
    }

    test("self collect is fee free") {
      threeTierRule.feeCentsForDelivery(12, midLng, midLat, isSelfCollect = true) shouldBe 0L
    }

    test("floor null (平房) still charges distance tier") {
      threeTierRule.feeCentsForDelivery(null, midLng, midLat, isSelfCollect = false) shouldBe 300L
    }

    test("minimum order disabled by default (0)") {
      rule().belowMinimumOrder(0L) shouldBe false
      rule().belowMinimumOrder(1L) shouldBe false
    }

    test("belowMinimumOrder boundaries: below / equal / above") {
      val r = rule(minimumOrderCents = 3000L)
      r.belowMinimumOrder(2999L) shouldBe true
      r.belowMinimumOrder(3000L) shouldBe false
      r.belowMinimumOrder(3001L) shouldBe false
    }
  })
