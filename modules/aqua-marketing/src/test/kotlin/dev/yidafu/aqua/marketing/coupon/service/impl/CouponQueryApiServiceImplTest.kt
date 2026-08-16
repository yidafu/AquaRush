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
import dev.yidafu.aqua.common.domain.model.UserCouponModel
import dev.yidafu.aqua.common.domain.model.enums.CouponIssuanceType
import dev.yidafu.aqua.common.domain.model.enums.CouponTemplateStatus
import dev.yidafu.aqua.common.domain.model.enums.UserCouponStatus
import dev.yidafu.aqua.marketing.coupon.domain.repository.CouponTemplateRepository
import dev.yidafu.aqua.marketing.coupon.domain.repository.UserCouponRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime
import java.util.Optional

/** 优惠券用户侧查询服务单元测试(领券中心/我的券包/可用券)。 */
class CouponQueryApiServiceImplTest :
  FunSpec({
    var couponTemplateRepository: CouponTemplateRepository = mockk()
    var userCouponRepository: UserCouponRepository = mockk()
    var service: CouponQueryApiServiceImpl = mockk()

    fun template(
      id: Long,
      issuanceType: CouponIssuanceType,
    ): CouponTemplateModel =
      CouponTemplateModel(
        id = id,
        name = "券$id",
        issuanceType = issuanceType,
        status = CouponTemplateStatus.ACTIVE,
      )

    fun coupon(
      id: Long,
      expireTime: LocalDateTime,
      status: UserCouponStatus = UserCouponStatus.RECEIVED,
    ): UserCouponModel =
      UserCouponModel(
        id = id,
        userId = 1L,
        couponTemplateId = 1L,
        status = status,
        expireTime = expireTime,
      )

    beforeTest {
      couponTemplateRepository = mockk()
      userCouponRepository = mockk()
      service = CouponQueryApiServiceImpl(couponTemplateRepository, userCouponRepository)
    }
    test("findClaimableTemplates filters") {
      every { couponTemplateRepository.findByStatus(CouponTemplateStatus.ACTIVE) } returns
        listOf(template(1L, CouponIssuanceType.AUTO_CLAIM), template(2L, CouponIssuanceType.TARGETED))

      val result = service.findClaimableTemplates(1L)

      result.size shouldBe 1
      result[0].id shouldBe 1L
    }
    test("findUserCoupons by status") {
      every { userCouponRepository.findByUserId(1L) } returns listOf(coupon(1L, LocalDateTime.now().plusDays(1)))
      every { userCouponRepository.findByUserIdAndStatus(1L, UserCouponStatus.USED) } returns
        listOf(coupon(2L, LocalDateTime.now().plusDays(1), UserCouponStatus.USED))

      service.findUserCoupons(1L, null).size shouldBe 1
      service.findUserCoupons(1L, UserCouponStatus.USED).size shouldBe 1
    }
    test("findUsableCoupons filters expired") {
      val now = LocalDateTime.now()
      every {
        userCouponRepository.findByUserIdAndStatusIn(1L, listOf(UserCouponStatus.RECEIVED, UserCouponStatus.RETURNED))
      } returns
        listOf(
          coupon(1L, now.plusDays(1)),
          coupon(2L, now.minusDays(1)),
          coupon(3L, now.plusDays(1), UserCouponStatus.RETURNED),
        )

      val result = service.findUsableCoupons(1L)

      result.map { it.id } shouldBe listOf(1L, 3L)
    }
    test("findUserCouponByIdAndUser checks ownership") {
      every { userCouponRepository.findById(1L) } returns
        Optional.of(UserCouponModel(id = 1L, userId = 9L, couponTemplateId = 1L, expireTime = LocalDateTime.now().plusDays(1)))
      every { userCouponRepository.findById(2L) } returns Optional.of(coupon(2L, LocalDateTime.now().plusDays(1)))
      every { userCouponRepository.findById(3L) } returns Optional.empty()

      service.findUserCouponByIdAndUser(1L, 1L).shouldBeNull()
      service.findUserCouponByIdAndUser(1L, 2L)?.id shouldBe 2L
      service.findUserCouponByIdAndUser(1L, 3L).shouldBeNull()
    }
    test("countUserCouponsByTemplate delegates") {
      every { userCouponRepository.countByUserIdAndCouponTemplateId(1L, 5L) } returns 2

      service.countUserCouponsByTemplate(1L, 5L) shouldBe 2
    }
  })
