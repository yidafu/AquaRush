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

package dev.yidafu.aqua.marketing.coupon.service.impl

import dev.yidafu.aqua.common.domain.model.CouponTemplateModel
import dev.yidafu.aqua.common.domain.model.enums.CouponIssuanceType
import dev.yidafu.aqua.common.domain.model.enums.CouponScope
import dev.yidafu.aqua.common.domain.model.enums.CouponTemplateStatus
import dev.yidafu.aqua.common.domain.model.enums.CouponType
import dev.yidafu.aqua.common.domain.model.enums.UserCouponStatus
import dev.yidafu.aqua.common.domain.model.enums.ValidityType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals

/** 优惠券快照构造单元测试(R-MKT-03 参数快照)。 */
class CouponSnapshotSupportTest {
  private val now: LocalDateTime = LocalDateTime.of(2026, 8, 14, 10, 0)

  private fun template(
    validityType: ValidityType = ValidityType.FIXED_RANGE,
    validTo: LocalDateTime? = now.plusDays(30),
    validDays: Int? = null,
  ): CouponTemplateModel =
    CouponTemplateModel(
      id = 1L,
      name = "测试券",
      couponType = CouponType.CASH,
      thresholdCents = 5000,
      discountAmountCents = 1000,
      scope = CouponScope.ALL,
      validityType = validityType,
      validFrom = now.minusDays(1),
      validTo = validTo,
      validDays = validDays,
      issuanceType = CouponIssuanceType.AUTO_CLAIM,
      perUserLimit = 1,
      status = CouponTemplateStatus.ACTIVE,
    )

  @Test
  @DisplayName("过期时间:固定区间=validTo")
  fun `computeExpireTime fixed range uses validTo`() {
    val expire = CouponSnapshotSupport.computeExpireTime(template(validTo = now.plusDays(10)), now)

    assertEquals(now.plusDays(10), expire)
  }

  @Test
  @DisplayName("过期时间:固定区间无 validTo → 回退领取时间")
  fun `computeExpireTime fixed range missing validTo falls back`() {
    val expire = CouponSnapshotSupport.computeExpireTime(template(validTo = null), now)

    assertEquals(now, expire)
  }

  @Test
  @DisplayName("过期时间:领取后N天=claimTime+validDays")
  fun `computeExpireTime days after claim`() {
    val expire =
      CouponSnapshotSupport.computeExpireTime(template(validityType = ValidityType.DAYS_AFTER_CLAIM, validTo = null, validDays = 15), now)

    assertEquals(now.plusDays(15), expire)
  }

  @Test
  @DisplayName("过期时间:领取后N天无天数 → 回退领取时间")
  fun `computeExpireTime days after claim missing days falls back`() {
    val expire =
      CouponSnapshotSupport.computeExpireTime(template(validityType = ValidityType.DAYS_AFTER_CLAIM, validTo = null, validDays = null), now)

    assertEquals(now, expire)
  }

  @Test
  @DisplayName("过期时间:其他类型回退领取时间")
  fun `computeExpireTime other type falls back`() {
    val expire =
      CouponSnapshotSupport.computeExpireTime(template(validityType = ValidityType.DAYS_AFTER_PURCHASE, validTo = null), now)

    assertEquals(now, expire)
  }

  @Test
  @DisplayName("快照:由模板构造用户券,字段快照+状态 RECEIVED")
  fun `buildUserCoupon snapshots template params`() {
    val t = template()
    val coupon = CouponSnapshotSupport.buildUserCoupon(userId = 7L, template = t, claimTime = now)

    assertEquals(7L, coupon.userId)
    assertEquals(1L, coupon.couponTemplateId)
    assertEquals(UserCouponStatus.RECEIVED, coupon.status)
    assertEquals("测试券", coupon.name)
    assertEquals(CouponType.CASH, coupon.couponType)
    assertEquals(5000, coupon.thresholdCents)
    assertEquals(1000, coupon.discountAmountCents)
    assertEquals(CouponScope.ALL, coupon.scope)
    assertEquals(now.plusDays(30), coupon.expireTime)
    assertEquals(now, coupon.claimTime)
    assertEquals(now, coupon.createdAt)
  }
}
