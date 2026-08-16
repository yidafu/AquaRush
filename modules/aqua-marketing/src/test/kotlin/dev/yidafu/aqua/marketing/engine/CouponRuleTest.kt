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

package dev.yidafu.aqua.marketing.engine

import dev.yidafu.aqua.api.service.marketing.CouponQueryApiService
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.UserCouponModel
import dev.yidafu.aqua.common.domain.model.enums.CouponScope
import dev.yidafu.aqua.common.domain.model.enums.CouponType
import dev.yidafu.aqua.common.domain.model.enums.UserCouponStatus
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.marketing.coupon.domain.repository.UserCouponRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime

/** 优惠券规则单元测试(R-MKT-03/07 叠加③,聚焦引擎测试未覆盖分支)。 */
class CouponRuleTest :
  FunSpec({
    var couponQueryApiService: CouponQueryApiService = mockk()
    var userCouponRepository: UserCouponRepository = mockk()
    var rule: CouponRule = mockk()
    val now: LocalDateTime = LocalDateTime.of(2026, 8, 14, 10, 0)

    fun ctx(
      preferredCouponId: Long? = null,
      orderId: Long? = null,
      productIds: List<Long> = listOf(1L),
    ): PricingContext =
      PricingContext(
        userId = 1L,
        items =
          productIds.map { productId ->
            PricingItemContext(
              productId = productId,
              quantity = 5,
              product = ProductModel(id = productId, name = "水", price = 2100, specification = "19L"),
            )
          },
        isSelfCollect = false,
        preferredCouponId = preferredCouponId,
        ticketRedeemCount = 0,
        now = now,
        orderId = orderId,
      )

    fun state(cashSubtotal: Long = 10500): PricingState = PricingState(cashSubtotalCents = cashSubtotal)

    fun coupon(
      id: Long,
      type: CouponType,
      amount: Long? = null,
      rate: Long? = null,
      thresholdCents: Long? = null,
      scope: CouponScope = CouponScope.ALL,
      productId: Long? = null,
    ): UserCouponModel =
      UserCouponModel(
        id = id,
        userId = 1L,
        couponTemplateId = 100L + id,
        status = UserCouponStatus.RECEIVED,
        name = "券$id",
        couponType = type,
        thresholdCents = thresholdCents,
        discountAmountCents = amount,
        discountRatePermille = rate,
        scope = scope,
        productId = productId,
        expireTime = now.plusDays(30),
      )

    beforeTest {
      couponQueryApiService = mockk()
      userCouponRepository = mockk()
      rule = CouponRule(couponQueryApiService, userCouponRepository)
    }
    test("discount coupon computes on balance after promotion") {
      every { couponQueryApiService.findUsableCoupons(1L) } returns listOf(coupon(1L, CouponType.DISCOUNT, rate = 9000))
      val s = state(cashSubtotal = 10500)
      s.promotionDiscountCents = 500

      rule.evaluate(ctx(), s)

      // 余额 10000 × 10% = 1000
      s.couponDiscountCents shouldBe 1000
      s.selectedCouponId shouldBe 1L
    }
    test("discount coupon without rate discounts nothing") {
      every { couponQueryApiService.findUsableCoupons(1L) } returns listOf(coupon(1L, CouponType.DISCOUNT, rate = null))
      val s = state()

      rule.evaluate(ctx(), s)

      s.couponDiscountCents shouldBe 0
      s.selectedCouponId shouldBe 1L
    }
    test("no threshold cash coupon always eligible") {
      every { couponQueryApiService.findUsableCoupons(1L) } returns listOf(coupon(1L, CouponType.NO_THRESHOLD_CASH, amount = 500))
      val s = state(cashSubtotal = 600)

      rule.evaluate(ctx(), s)

      s.couponDiscountCents shouldBe 500
      s.selectedCouponId shouldBe 1L
    }
    test("preferred unusable coupon not selected") {
      every { couponQueryApiService.findUsableCoupons(1L) } returns
        listOf(coupon(1L, CouponType.CASH, amount = 1000, thresholdCents = 20000))
      val s = state(cashSubtotal = 10500)

      rule.evaluate(ctx(preferredCouponId = 1L), s)

      s.selectedCouponId.shouldBeNull()
      s.couponDiscountCents shouldBe 0
    }
    test("preferred missing coupon not selected") {
      every { couponQueryApiService.findUsableCoupons(1L) } returns
        listOf(coupon(1L, CouponType.CASH, amount = 1000, thresholdCents = 5000))
      val s = state()

      rule.evaluate(ctx(preferredCouponId = 999L), s)

      s.selectedCouponId.shouldBeNull()
    }
    test("applyForOrder redeems coupon") {
      every { couponQueryApiService.findUsableCoupons(1L) } returns
        listOf(coupon(1L, CouponType.CASH, amount = 1000, thresholdCents = 5000))
      every { userCouponRepository.redeemAtomically(1L, 1L, 99L, UserCouponStatus.RECEIVED, UserCouponStatus.USED, now) } returns 1
      val s = state()

      rule.evaluate(ctx(preferredCouponId = 1L, orderId = 99L), s)

      s.selectedCouponId shouldBe 1L
      verify { userCouponRepository.redeemAtomically(1L, 1L, 99L, UserCouponStatus.RECEIVED, UserCouponStatus.USED, now) }
    }
    test("applyForOrder redeem failure rolls back") {
      every { couponQueryApiService.findUsableCoupons(1L) } returns
        listOf(coupon(1L, CouponType.CASH, amount = 1000, thresholdCents = 5000))
      every { userCouponRepository.redeemAtomically(1L, 1L, 99L, UserCouponStatus.RECEIVED, UserCouponStatus.USED, now) } returns 0
      val s = state()

      shouldThrow<BadRequestException> {
        rule.evaluate(ctx(preferredCouponId = 1L, orderId = 99L), s)
      }
    }
    test("no coupons leaves options empty") {
      every { couponQueryApiService.findUsableCoupons(1L) } returns emptyList()
      val s = state()

      rule.evaluate(ctx(), s)

      s.couponOptions.size shouldBe 0
      s.selectedCouponId.shouldBeNull()
      s.couponDiscountCents shouldBe 0
    }
  })
