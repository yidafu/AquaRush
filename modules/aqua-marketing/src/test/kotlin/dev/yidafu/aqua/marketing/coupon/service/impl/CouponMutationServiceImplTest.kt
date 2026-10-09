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

import dev.yidafu.aqua.api.service.user.UserQueryApiService
import dev.yidafu.aqua.common.domain.model.CouponTemplateModel
import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.domain.model.enums.CouponTemplateStatus
import dev.yidafu.aqua.common.graphql.generated.CouponIssuanceType
import dev.yidafu.aqua.common.graphql.generated.CouponScope
import dev.yidafu.aqua.common.graphql.generated.CouponType
import dev.yidafu.aqua.common.graphql.generated.CreateCouponTemplateInput
import dev.yidafu.aqua.common.graphql.generated.IssueCouponsInput
import dev.yidafu.aqua.common.graphql.generated.IssueTargetType
import dev.yidafu.aqua.common.graphql.generated.UpdateCouponTemplateInput
import dev.yidafu.aqua.common.graphql.generated.ValidityType
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.marketing.coupon.domain.exception.CouponNotClaimableException
import dev.yidafu.aqua.marketing.coupon.domain.exception.CouponTemplateNotFoundException
import dev.yidafu.aqua.marketing.coupon.domain.repository.CouponTemplateRepository
import dev.yidafu.aqua.marketing.coupon.domain.repository.UserCouponRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import java.util.Optional
import dev.yidafu.aqua.common.domain.model.enums.CouponIssuanceType as DomainCouponIssuanceType
import dev.yidafu.aqua.common.domain.model.enums.CouponScope as DomainCouponScope
import dev.yidafu.aqua.common.domain.model.enums.CouponType as DomainCouponType
import dev.yidafu.aqua.common.domain.model.enums.ValidityType as DomainValidityType

/** 优惠券模板变更服务单元测试(F-MKT-003):建券/编辑/停用/定向发放。 */
class CouponMutationServiceImplTest :
  FunSpec({
    var couponTemplateRepository: CouponTemplateRepository = mockk()
    var userCouponRepository: UserCouponRepository = mockk()
    var userQueryApiService: UserQueryApiService = mockk()
    var bizLogger: BizLogger = mockk()
    var service: CouponMutationServiceImpl = mockk()
    val now: LocalDateTime = LocalDateTime.of(2026, 8, 14, 10, 0)

    fun template(
      id: Long = 1L,
      status: CouponTemplateStatus = CouponTemplateStatus.ACTIVE,
    ): CouponTemplateModel =
      CouponTemplateModel(
        id = id,
        name = "满50减10",
        couponType = DomainCouponType.CASH,
        thresholdCents = 5000,
        discountAmountCents = 1000,
        scope = DomainCouponScope.ALL,
        validityType = DomainValidityType.FIXED_RANGE,
        validFrom = now.minusDays(1),
        validTo = now.plusDays(30),
        issuanceType = DomainCouponIssuanceType.AUTO_CLAIM,
        perUserLimit = 1,
        status = status,
      )

    fun stubSaveReturningId(id: Long = 1L) {
      every { couponTemplateRepository.save(any()) } answers {
        (firstArg() as CouponTemplateModel).apply { this.id = id }
      }
    }

    fun issueTemplate(perUserLimit: Int = 1): CouponTemplateModel =
      CouponTemplateModel(
        id = 1L,
        name = "模板1",
        couponType = DomainCouponType.CASH,
        discountAmountCents = 1000,
        scope = DomainCouponScope.ALL,
        validityType = DomainValidityType.FIXED_RANGE,
        validFrom = now.minusDays(1),
        validTo = now.plusDays(30),
        issuanceType = DomainCouponIssuanceType.TARGETED,
        perUserLimit = perUserLimit,
        status = CouponTemplateStatus.ACTIVE,
      )

    // 定向发放用模板(默认限领 1 张)

    beforeTest {
      couponTemplateRepository = mockk()
      userCouponRepository = mockk()
      userQueryApiService = mockk()
      bizLogger = mockk(relaxed = true)
      service = CouponMutationServiceImpl(couponTemplateRepository, userCouponRepository, userQueryApiService, bizLogger)
    }
    test("createTemplate saves active template and logs") {
      stubSaveReturningId()
      val input =
        CreateCouponTemplateInput(
          name = "满50减10",
          couponType = CouponType.CASH,
          thresholdCents = 5000,
          discountAmountCents = 1000,
          scope = CouponScope.ALL,
          validityType = ValidityType.FIXED_RANGE,
          validFrom = now.minusDays(1),
          validTo = now.plusDays(30),
          issuanceType = CouponIssuanceType.AUTO_CLAIM,
          perUserLimit = 1,
        )

      val saved = service.createTemplate(adminId = 1L, adminName = "admin", input = input)

      saved.id shouldBe 1L
      saved.status shouldBe CouponTemplateStatus.ACTIVE
      saved.name shouldBe "满50减10"
      verify {
        bizLogger.logMarketingOperation(
          targetId = 1L,
          operation = "COUPON_TEMPLATE_CREATED",
          result = "SUCCESS",
          userId = 1L,
          username = "admin",
          additionalData = any(),
        )
      }
    }
    test("createTemplate rejects cash without amount") {
      shouldThrow<IllegalArgumentException> {
        service.createTemplate(
          1L,
          "admin",
          CreateCouponTemplateInput(
            name = "无面额券",
            couponType = CouponType.CASH,
            scope = CouponScope.ALL,
            validityType = ValidityType.FIXED_RANGE,
            validFrom = now.minusDays(1),
            validTo = now.plusDays(30),
            issuanceType = CouponIssuanceType.AUTO_CLAIM,
            perUserLimit = 1,
          ),
        )
      }
      verify(exactly = 0) { couponTemplateRepository.save(any()) }
    }
    test("createTemplate rejects discount without rate") {
      shouldThrow<IllegalArgumentException> {
        service.createTemplate(
          1L,
          "admin",
          CreateCouponTemplateInput(
            name = "无折扣率券",
            couponType = CouponType.DISCOUNT,
            scope = CouponScope.ALL,
            validityType = ValidityType.FIXED_RANGE,
            validFrom = now.minusDays(1),
            validTo = now.plusDays(30),
            issuanceType = CouponIssuanceType.AUTO_CLAIM,
            perUserLimit = 1,
          ),
        )
      }
    }
    test("createTemplate rejects product scope without productId") {
      shouldThrow<IllegalArgumentException> {
        service.createTemplate(
          1L,
          "admin",
          CreateCouponTemplateInput(
            name = "指定商品券",
            couponType = CouponType.CASH,
            discountAmountCents = 1000,
            scope = CouponScope.PRODUCT,
            validityType = ValidityType.FIXED_RANGE,
            validFrom = now.minusDays(1),
            validTo = now.plusDays(30),
            issuanceType = CouponIssuanceType.AUTO_CLAIM,
            perUserLimit = 1,
          ),
        )
      }
    }
    test("createTemplate rejects invalid fixed range") {
      shouldThrow<IllegalArgumentException> {
        service.createTemplate(
          1L,
          "admin",
          CreateCouponTemplateInput(
            name = "区间券",
            couponType = CouponType.CASH,
            discountAmountCents = 1000,
            scope = CouponScope.ALL,
            validityType = ValidityType.FIXED_RANGE,
            validFrom = now.plusDays(30),
            validTo = now.plusDays(1),
            issuanceType = CouponIssuanceType.AUTO_CLAIM,
            perUserLimit = 1,
          ),
        )
      }
    }
    test("createTemplate rejects invalid validDays") {
      shouldThrow<IllegalArgumentException> {
        service.createTemplate(
          1L,
          "admin",
          CreateCouponTemplateInput(
            name = "天数券",
            couponType = CouponType.CASH,
            discountAmountCents = 1000,
            scope = CouponScope.ALL,
            validityType = ValidityType.DAYS_AFTER_CLAIM,
            validDays = 0,
            issuanceType = CouponIssuanceType.AUTO_CLAIM,
            perUserLimit = 1,
          ),
        )
      }
    }
    test("updateTemplate updates fields") {
      val existing = template()
      every { couponTemplateRepository.findById(1L) } returns Optional.of(existing)
      stubSaveReturningId(1L)
      val input =
        UpdateCouponTemplateInput(
          name = "改名券",
          couponType = CouponType.DISCOUNT,
          thresholdCents = 10000,
          discountRatePermille = 9000,
          scope = CouponScope.ALL,
          validityType = ValidityType.DAYS_AFTER_CLAIM,
          validDays = 30,
          perUserLimit = 2,
        )

      val updated = service.updateTemplate(1L, "admin", 1L, input)

      updated.name shouldBe "改名券"
      updated.couponType shouldBe DomainCouponType.DISCOUNT
      updated.discountRatePermille shouldBe 9000
      verify {
        bizLogger.logMarketingOperation(
          targetId = 1L,
          operation = "COUPON_TEMPLATE_UPDATED",
          result = "SUCCESS",
          userId = 1L,
          username = "admin",
          additionalData = any(),
        )
      }
    }
    test("updateTemplate not found throws") {
      every { couponTemplateRepository.findById(99L) } returns Optional.empty()

      shouldThrow<CouponTemplateNotFoundException> {
        service.updateTemplate(
          1L,
          "admin",
          99L,
          UpdateCouponTemplateInput(
            name = "x",
            couponType = CouponType.CASH,
            discountAmountCents = 1000,
            scope = CouponScope.ALL,
            validityType = ValidityType.FIXED_RANGE,
            validFrom = now.minusDays(1),
            validTo = now.plusDays(30),
            perUserLimit = 1,
          ),
        )
      }
    }
    test("disableTemplate sets disabled status") {
      every { couponTemplateRepository.findById(1L) } returns Optional.of(template())
      stubSaveReturningId(1L)

      val disabled = service.disableTemplate(1L, "admin", 1L)

      disabled.status shouldBe CouponTemplateStatus.DISABLED
      verify {
        bizLogger.logMarketingOperation(
          targetId = 1L,
          operation = "COUPON_TEMPLATE_DISABLED",
          result = "SUCCESS",
          userId = 1L,
          username = "admin",
          additionalData = any(),
        )
      }
    }
    test("disableTemplate not found throws") {
      every { couponTemplateRepository.findById(99L) } returns Optional.empty()

      shouldThrow<CouponTemplateNotFoundException> {
        service.disableTemplate(1L, "admin", 99L)
      }
    }
    test("issueCoupons to users skips over limit") {
      every { couponTemplateRepository.findById(1L) } returns Optional.of(issueTemplate(perUserLimit = 1))
      every { userCouponRepository.countByUserIdAndCouponTemplateId(10L, 1L) } returns 0
      every { userCouponRepository.countByUserIdAndCouponTemplateId(11L, 1L) } returns 1
      every { userCouponRepository.save(any()) } answers { firstArg() }

      val issued =
        service.issueCoupons(
          1L,
          "admin",
          IssueCouponsInput(couponTemplateId = 1L, targetType = IssueTargetType.USER, userIds = listOf(10L, 11L)),
        )

      issued shouldBe 1
      verify(exactly = 1) { userCouponRepository.save(any()) }
    }
    test("issueCoupons to group members") {
      val groupMember = UserModel(id = 20L, wechatOpenId = "wx20", email = "u20@x.com")
      every { couponTemplateRepository.findById(1L) } returns Optional.of(issueTemplate(perUserLimit = 5))
      every { userQueryApiService.findByCustomerGroupId(7L) } returns listOf(groupMember)
      every { userCouponRepository.countByUserIdAndCouponTemplateId(20L, 1L) } returns 0
      every { userCouponRepository.save(any()) } answers { firstArg() }

      val issued =
        service.issueCoupons(
          1L,
          "admin",
          IssueCouponsInput(couponTemplateId = 1L, targetType = IssueTargetType.GROUP, customerGroupId = 7L),
        )

      issued shouldBe 1
      verify { userCouponRepository.save(match { it.userId == 20L }) }
    }
    test("issueCoupons to group without groupId throws") {
      every { couponTemplateRepository.findById(1L) } returns Optional.of(template())

      shouldThrow<IllegalArgumentException> {
        service.issueCoupons(
          1L,
          "admin",
          IssueCouponsInput(couponTemplateId = 1L, targetType = IssueTargetType.GROUP),
        )
      }
    }
    test("issueCoupons disabled template throws") {
      every { couponTemplateRepository.findById(1L) } returns Optional.of(template(status = CouponTemplateStatus.DISABLED))

      shouldThrow<CouponNotClaimableException> {
        service.issueCoupons(
          1L,
          "admin",
          IssueCouponsInput(couponTemplateId = 1L, targetType = IssueTargetType.USER, userIds = listOf(10L)),
        )
      }
    }
    test("issueCoupons missing template throws") {
      every { couponTemplateRepository.findById(99L) } returns Optional.empty()

      shouldThrow<CouponTemplateNotFoundException> {
        service.issueCoupons(
          1L,
          "admin",
          IssueCouponsInput(couponTemplateId = 99L, targetType = IssueTargetType.USER, userIds = listOf(10L)),
        )
      }
    }
    test("issueCoupons empty target issues zero") {
      every { couponTemplateRepository.findById(1L) } returns Optional.of(issueTemplate(perUserLimit = 1))
      every { userQueryApiService.findByCustomerGroupId(7L) } returns emptyList()

      val issued =
        service.issueCoupons(
          1L,
          "admin",
          IssueCouponsInput(couponTemplateId = 1L, targetType = IssueTargetType.GROUP, customerGroupId = 7L),
        )

      issued shouldBe 0
      (issued == 0) shouldBe true
    }
  })
