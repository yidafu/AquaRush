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

import dev.yidafu.aqua.api.dto.ClaimCouponRequest
import dev.yidafu.aqua.common.domain.model.CouponTemplateModel
import dev.yidafu.aqua.common.domain.model.UserCouponModel
import dev.yidafu.aqua.common.domain.model.enums.CouponIssuanceType
import dev.yidafu.aqua.common.domain.model.enums.CouponScope
import dev.yidafu.aqua.common.domain.model.enums.CouponTemplateStatus
import dev.yidafu.aqua.common.domain.model.enums.CouponType
import dev.yidafu.aqua.common.domain.model.enums.UserCouponStatus
import dev.yidafu.aqua.common.domain.model.enums.ValidityType
import dev.yidafu.aqua.marketing.coupon.domain.exception.CouponLimitExceededException
import dev.yidafu.aqua.marketing.coupon.domain.exception.CouponNotClaimableException
import dev.yidafu.aqua.marketing.coupon.domain.exception.CouponTemplateNotFoundException
import dev.yidafu.aqua.marketing.coupon.domain.repository.CouponTemplateRepository
import dev.yidafu.aqua.marketing.coupon.domain.repository.UserCouponRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.LocalDateTime
import java.util.Optional

/** 优惠券用户侧变更服务单元测试(领券/退券,AC-MKT-003)。 */
class CouponMutationApiServiceImplTest :
  FunSpec({
    var couponTemplateRepository: CouponTemplateRepository = mockk()
    var userCouponRepository: UserCouponRepository = mockk()
    var service: CouponMutationApiServiceImpl = mockk()

    fun template(
      status: CouponTemplateStatus = CouponTemplateStatus.ACTIVE,
      issuanceType: CouponIssuanceType = CouponIssuanceType.AUTO_CLAIM,
      validityType: ValidityType = ValidityType.FIXED_RANGE,
      validTo: LocalDateTime? = LocalDateTime.now().plusDays(30),
      validDays: Int? = null,
      perUserLimit: Int = 1,
    ): CouponTemplateModel =
      CouponTemplateModel(
        id = 1L,
        name = "满50减10",
        couponType = CouponType.CASH,
        thresholdCents = 5000,
        discountAmountCents = 1000,
        scope = CouponScope.ALL,
        validityType = validityType,
        validFrom = LocalDateTime.now().minusDays(1),
        validTo = validTo,
        validDays = validDays,
        issuanceType = issuanceType,
        perUserLimit = perUserLimit,
        status = status,
      )

    beforeTest {
      couponTemplateRepository = mockk()
      userCouponRepository = mockk()
      service = CouponMutationApiServiceImpl(couponTemplateRepository, userCouponRepository)
    }
    test("claimCoupon saves snapshot coupon") {
      every { couponTemplateRepository.findById(1L) } returns Optional.of(template())
      every { userCouponRepository.countByUserIdAndCouponTemplateId(1L, 1L) } returns 0
      val couponSlot = slot<UserCouponModel>()
      every { userCouponRepository.save(capture(couponSlot)) } answers { firstArg() }

      val coupon = service.claimCoupon(1L, ClaimCouponRequest(couponTemplateId = 1L))

      coupon.status shouldBe UserCouponStatus.RECEIVED
      coupon.userId shouldBe 1L
      coupon.couponTemplateId shouldBe 1L
      coupon.name shouldBe "满50减10"
      coupon.discountAmountCents shouldBe 1000
      verify(exactly = 1) { userCouponRepository.save(any()) }
    }
    test("claimCoupon missing template throws") {
      every { couponTemplateRepository.findById(99L) } returns Optional.empty()

      shouldThrow<CouponTemplateNotFoundException> {
        service.claimCoupon(1L, ClaimCouponRequest(couponTemplateId = 99L))
      }
    }
    test("claimCoupon disabled template throws") {
      every { couponTemplateRepository.findById(1L) } returns Optional.of(template(status = CouponTemplateStatus.DISABLED))

      shouldThrow<CouponNotClaimableException> {
        service.claimCoupon(1L, ClaimCouponRequest(couponTemplateId = 1L))
      }
    }
    test("claimCoupon targeted template throws") {
      every { couponTemplateRepository.findById(1L) } returns Optional.of(template(issuanceType = CouponIssuanceType.TARGETED))

      shouldThrow<CouponNotClaimableException> {
        service.claimCoupon(1L, ClaimCouponRequest(couponTemplateId = 1L))
      }
    }
    test("claimCoupon over limit throws") {
      every { couponTemplateRepository.findById(1L) } returns Optional.of(template(perUserLimit = 3))
      every { userCouponRepository.countByUserIdAndCouponTemplateId(1L, 1L) } returns 3

      shouldThrow<CouponLimitExceededException> {
        service.claimCoupon(1L, ClaimCouponRequest(couponTemplateId = 1L))
      }
    }
    test("claimCoupon expired template throws") {
      every { couponTemplateRepository.findById(1L) } returns Optional.of(template(validTo = LocalDateTime.now().minusDays(1)))
      every { userCouponRepository.countByUserIdAndCouponTemplateId(1L, 1L) } returns 0

      shouldThrow<CouponNotClaimableException> {
        service.claimCoupon(1L, ClaimCouponRequest(couponTemplateId = 1L))
      }
    }
    test("claimCoupon days after claim computes expire") {
      every { couponTemplateRepository.findById(1L) } returns
        Optional.of(template(validityType = ValidityType.DAYS_AFTER_CLAIM, validTo = null, validDays = 30))
      every { userCouponRepository.countByUserIdAndCouponTemplateId(1L, 1L) } returns 0
      val couponSlot = slot<UserCouponModel>()
      every { userCouponRepository.save(capture(couponSlot)) } answers { firstArg() }

      service.claimCoupon(1L, ClaimCouponRequest(couponTemplateId = 1L))

      val expected = couponSlot.captured.claimTime.plusDays(30)
      couponSlot.captured.expireTime shouldBe expected
    }
    test("returnCouponsByOrder delegates") {
      every { userCouponRepository.returnByOrder(99L, any()) } returns 2

      val returned = service.returnCouponsByOrder(99L)

      returned shouldBe 2
      verify { userCouponRepository.returnByOrder(99L, any()) }
    }
  })
