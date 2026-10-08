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
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

/** 优惠券快照构造单元测试(R-MKT-03 参数快照)。 */
class CouponSnapshotSupportTest :
  FunSpec({
    val now: LocalDateTime = LocalDateTime.of(2026, 8, 14, 10, 0)

    fun template(
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

    test("computeExpireTime fixed range uses validTo") {
      val expire = CouponSnapshotSupport.computeExpireTime(template(validTo = now.plusDays(10)), now)

      expire shouldBe now.plusDays(10)
    }
    test("computeExpireTime fixed range missing validTo falls back") {
      val expire = CouponSnapshotSupport.computeExpireTime(template(validTo = null), now)

      expire shouldBe now
    }
    test("computeExpireTime days after claim") {
      val expire =
        CouponSnapshotSupport.computeExpireTime(template(validityType = ValidityType.DAYS_AFTER_CLAIM, validTo = null, validDays = 15), now)

      expire shouldBe now.plusDays(15)
    }
    test("computeExpireTime days after claim missing days falls back") {
      val expire =
        CouponSnapshotSupport.computeExpireTime(
          template(validityType = ValidityType.DAYS_AFTER_CLAIM, validTo = null, validDays = null),
          now,
        )

      expire shouldBe now
    }
    test("computeExpireTime other type falls back") {
      val expire =
        CouponSnapshotSupport.computeExpireTime(template(validityType = ValidityType.DAYS_AFTER_PURCHASE, validTo = null), now)

      expire shouldBe now
    }
    test("buildUserCoupon snapshots template params") {
      val t = template()
      val coupon = CouponSnapshotSupport.buildUserCoupon(userId = 7L, template = t, claimTime = now)

      coupon.userId shouldBe 7L
      coupon.couponTemplateId shouldBe 1L
      coupon.status shouldBe UserCouponStatus.RECEIVED
      coupon.name shouldBe "测试券"
      coupon.couponType shouldBe CouponType.CASH
      coupon.thresholdCents shouldBe 5000
      coupon.discountAmountCents shouldBe 1000
      coupon.scope shouldBe CouponScope.ALL
      coupon.expireTime shouldBe now.plusDays(30)
      coupon.claimTime shouldBe now
      coupon.createdAt shouldBe now
    }
  })
