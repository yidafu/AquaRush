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

import dev.yidafu.aqua.api.dto.CouponTemplateCreateRequest
import dev.yidafu.aqua.api.dto.CouponTemplateUpdateRequest
import dev.yidafu.aqua.api.dto.IssueCouponsRequest
import dev.yidafu.aqua.api.dto.IssueTargetType
import dev.yidafu.aqua.api.service.user.UserQueryApiService
import dev.yidafu.aqua.common.domain.model.CouponTemplateModel
import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.domain.model.enums.CouponIssuanceType
import dev.yidafu.aqua.common.domain.model.enums.CouponScope
import dev.yidafu.aqua.common.domain.model.enums.CouponTemplateStatus
import dev.yidafu.aqua.common.domain.model.enums.CouponType
import dev.yidafu.aqua.common.domain.model.enums.ValidityType
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.marketing.coupon.domain.exception.CouponNotClaimableException
import dev.yidafu.aqua.marketing.coupon.domain.exception.CouponTemplateNotFoundException
import dev.yidafu.aqua.marketing.coupon.domain.repository.CouponTemplateRepository
import dev.yidafu.aqua.marketing.coupon.domain.repository.UserCouponRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** 优惠券模板变更服务单元测试(F-MKT-003):建券/编辑/停用/定向发放。 */
class CouponMutationServiceImplTest {
  private lateinit var couponTemplateRepository: CouponTemplateRepository
  private lateinit var userCouponRepository: UserCouponRepository
  private lateinit var userQueryApiService: UserQueryApiService
  private lateinit var bizLogger: BizLogger
  private lateinit var service: CouponMutationServiceImpl

  private val now: LocalDateTime = LocalDateTime.of(2026, 8, 14, 10, 0)

  @BeforeEach
  fun setUp() {
    couponTemplateRepository = mockk()
    userCouponRepository = mockk()
    userQueryApiService = mockk()
    bizLogger = mockk(relaxed = true)
    service = CouponMutationServiceImpl(couponTemplateRepository, userCouponRepository, userQueryApiService, bizLogger)
  }

  private fun template(
    id: Long = 1L,
    status: CouponTemplateStatus = CouponTemplateStatus.ACTIVE,
  ): CouponTemplateModel =
    CouponTemplateModel(
      id = id,
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
      status = status,
    )

  private fun stubSaveReturningId(id: Long = 1L) {
    every { couponTemplateRepository.save(any()) } answers {
      (firstArg() as CouponTemplateModel).apply { this.id = id }
    }
  }

  @Test
  @DisplayName("建券:校验通过保存并记录业务日志(AC-MKT-003-01)")
  fun `createTemplate saves active template and logs`() {
    stubSaveReturningId()
    val request =
      CouponTemplateCreateRequest(
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

    val saved = service.createTemplate(adminId = 1L, adminName = "admin", request = request)

    assertEquals(1L, saved.id)
    assertEquals(CouponTemplateStatus.ACTIVE, saved.status)
    assertEquals("满50减10", saved.name)
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

  @Test
  @DisplayName("建券:现金券缺少面额 → IllegalArgumentException")
  fun `createTemplate rejects cash without amount`() {
    assertFailsWith<IllegalArgumentException> {
      service.createTemplate(
        1L,
        "admin",
        CouponTemplateCreateRequest(
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

  @Test
  @DisplayName("建券:折扣券缺少折扣率 → IllegalArgumentException")
  fun `createTemplate rejects discount without rate`() {
    assertFailsWith<IllegalArgumentException> {
      service.createTemplate(
        1L,
        "admin",
        CouponTemplateCreateRequest(
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

  @Test
  @DisplayName("建券:指定商品券必须选择商品")
  fun `createTemplate rejects product scope without productId`() {
    assertFailsWith<IllegalArgumentException> {
      service.createTemplate(
        1L,
        "admin",
        CouponTemplateCreateRequest(
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

  @Test
  @DisplayName("建券:固定区间必须开始早于结束")
  fun `createTemplate rejects invalid fixed range`() {
    assertFailsWith<IllegalArgumentException> {
      service.createTemplate(
        1L,
        "admin",
        CouponTemplateCreateRequest(
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

  @Test
  @DisplayName("建券:领取后N天券天数必须为正数")
  fun `createTemplate rejects invalid validDays`() {
    assertFailsWith<IllegalArgumentException> {
      service.createTemplate(
        1L,
        "admin",
        CouponTemplateCreateRequest(
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

  @Test
  @DisplayName("编辑:全量更新模板字段并记录日志")
  fun `updateTemplate updates fields`() {
    val existing = template()
    every { couponTemplateRepository.findById(1L) } returns Optional.of(existing)
    stubSaveReturningId(1L)
    val request =
      CouponTemplateUpdateRequest(
        name = "改名券",
        couponType = CouponType.DISCOUNT,
        thresholdCents = 10000,
        discountRatePermille = 9000,
        scope = CouponScope.ALL,
        validityType = ValidityType.DAYS_AFTER_CLAIM,
        validDays = 30,
        perUserLimit = 2,
      )

    val updated = service.updateTemplate(1L, "admin", 1L, request)

    assertEquals("改名券", updated.name)
    assertEquals(CouponType.DISCOUNT, updated.couponType)
    assertEquals(9000, updated.discountRatePermille)
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

  @Test
  @DisplayName("编辑:模板不存在 → CouponTemplateNotFoundException")
  fun `updateTemplate not found throws`() {
    every { couponTemplateRepository.findById(99L) } returns Optional.empty()

    assertFailsWith<CouponTemplateNotFoundException> {
      service.updateTemplate(
        1L,
        "admin",
        99L,
        CouponTemplateUpdateRequest(
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

  @Test
  @DisplayName("停用:模板状态置 DISABLED")
  fun `disableTemplate sets disabled status`() {
    every { couponTemplateRepository.findById(1L) } returns Optional.of(template())
    stubSaveReturningId(1L)

    val disabled = service.disableTemplate(1L, "admin", 1L)

    assertEquals(CouponTemplateStatus.DISABLED, disabled.status)
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

  @Test
  @DisplayName("停用:模板不存在 → CouponTemplateNotFoundException")
  fun `disableTemplate not found throws`() {
    every { couponTemplateRepository.findById(99L) } returns Optional.empty()

    assertFailsWith<CouponTemplateNotFoundException> {
      service.disableTemplate(1L, "admin", 99L)
    }
  }

  @Test
  @DisplayName("发放:按用户列表发放,达到限领数跳过(AC-MKT-003-06)")
  fun `issueCoupons to users skips over limit`() {
    every { couponTemplateRepository.findById(1L) } returns Optional.of(issueTemplate(perUserLimit = 1))
    every { userCouponRepository.countByUserIdAndCouponTemplateId(10L, 1L) } returns 0
    every { userCouponRepository.countByUserIdAndCouponTemplateId(11L, 1L) } returns 1
    every { userCouponRepository.save(any()) } answers { firstArg() }

    val issued =
      service.issueCoupons(
        1L,
        "admin",
        IssueCouponsRequest(couponTemplateId = 1L, targetType = IssueTargetType.USER, userIds = listOf(10L, 11L)),
      )

    assertEquals(1, issued)
    verify(exactly = 1) { userCouponRepository.save(any()) }
  }

  @Test
  @DisplayName("发放:按分组全体成员发放")
  fun `issueCoupons to group members`() {
    val groupMember = UserModel(id = 20L, wechatOpenId = "wx20", email = "u20@x.com")
    every { couponTemplateRepository.findById(1L) } returns Optional.of(issueTemplate(perUserLimit = 5))
    every { userQueryApiService.findByCustomerGroupId(7L) } returns listOf(groupMember)
    every { userCouponRepository.countByUserIdAndCouponTemplateId(20L, 1L) } returns 0
    every { userCouponRepository.save(any()) } answers { firstArg() }

    val issued =
      service.issueCoupons(
        1L,
        "admin",
        IssueCouponsRequest(couponTemplateId = 1L, targetType = IssueTargetType.GROUP, customerGroupId = 7L),
      )

    assertEquals(1, issued)
    verify { userCouponRepository.save(match { it.userId == 20L }) }
  }

  @Test
  @DisplayName("发放:分组发放未指定分组 → IllegalArgumentException")
  fun `issueCoupons to group without groupId throws`() {
    every { couponTemplateRepository.findById(1L) } returns Optional.of(template())

    assertFailsWith<IllegalArgumentException> {
      service.issueCoupons(
        1L,
        "admin",
        IssueCouponsRequest(couponTemplateId = 1L, targetType = IssueTargetType.GROUP),
      )
    }
  }

  @Test
  @DisplayName("发放:模板已停用 → CouponNotClaimableException")
  fun `issueCoupons disabled template throws`() {
    every { couponTemplateRepository.findById(1L) } returns Optional.of(template(status = CouponTemplateStatus.DISABLED))

    assertFailsWith<CouponNotClaimableException> {
      service.issueCoupons(
        1L,
        "admin",
        IssueCouponsRequest(couponTemplateId = 1L, targetType = IssueTargetType.USER, userIds = listOf(10L)),
      )
    }
  }

  @Test
  @DisplayName("发放:模板不存在 → CouponTemplateNotFoundException")
  fun `issueCoupons missing template throws`() {
    every { couponTemplateRepository.findById(99L) } returns Optional.empty()

    assertFailsWith<CouponTemplateNotFoundException> {
      service.issueCoupons(
        1L,
        "admin",
        IssueCouponsRequest(couponTemplateId = 99L, targetType = IssueTargetType.USER, userIds = listOf(10L)),
      )
    }
  }

  /** 定向发放用模板(默认限领 1 张) */
  private fun issueTemplate(perUserLimit: Int = 1): CouponTemplateModel =
    CouponTemplateModel(
      id = 1L,
      name = "模板1",
      couponType = CouponType.CASH,
      discountAmountCents = 1000,
      scope = CouponScope.ALL,
      validityType = ValidityType.FIXED_RANGE,
      validFrom = now.minusDays(1),
      validTo = now.plusDays(30),
      issuanceType = CouponIssuanceType.TARGETED,
      perUserLimit = perUserLimit,
      status = CouponTemplateStatus.ACTIVE,
    )

  @Test
  @DisplayName("发放:全组无人达标则发放 0 张")
  fun `issueCoupons empty target issues zero`() {
    every { couponTemplateRepository.findById(1L) } returns Optional.of(issueTemplate(perUserLimit = 1))
    every { userQueryApiService.findByCustomerGroupId(7L) } returns emptyList()

    val issued =
      service.issueCoupons(
        1L,
        "admin",
        IssueCouponsRequest(couponTemplateId = 1L, targetType = IssueTargetType.GROUP, customerGroupId = 7L),
      )

    assertEquals(0, issued)
    assertTrue(issued == 0)
  }
}
