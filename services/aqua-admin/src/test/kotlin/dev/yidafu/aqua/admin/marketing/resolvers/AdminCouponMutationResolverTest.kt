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

package dev.yidafu.aqua.admin.marketing.resolvers

import dev.yidafu.aqua.api.service.marketing.CouponMutationService
import dev.yidafu.aqua.api.service.marketing.CouponQueryService
import dev.yidafu.aqua.common.domain.model.CouponTemplateModel
import dev.yidafu.aqua.common.domain.model.enums.CouponIssuanceType
import dev.yidafu.aqua.common.domain.model.enums.CouponScope
import dev.yidafu.aqua.common.domain.model.enums.CouponTemplateStatus
import dev.yidafu.aqua.common.domain.model.enums.CouponType
import dev.yidafu.aqua.common.domain.model.enums.ValidityType
import dev.yidafu.aqua.common.graphql.generated.CreateCouponTemplateInput
import dev.yidafu.aqua.common.graphql.generated.IssueCouponsInput
import dev.yidafu.aqua.common.graphql.generated.IssueTargetType
import dev.yidafu.aqua.common.graphql.generated.UpdateCouponTemplateInput
import dev.yidafu.aqua.common.security.UserPrincipal
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.time.LocalDateTime
import kotlin.test.assertEquals
import dev.yidafu.aqua.common.graphql.generated.CouponIssuanceType as GraphQLCouponIssuanceType
import dev.yidafu.aqua.common.graphql.generated.CouponScope as GraphQLCouponScope
import dev.yidafu.aqua.common.graphql.generated.CouponType as GraphQLCouponType
import dev.yidafu.aqua.common.graphql.generated.ValidityType as GraphQLValidityType

class AdminCouponMutationResolverTest {
  private lateinit var couponMutationService: CouponMutationService
  private lateinit var couponQueryService: CouponQueryService
  private lateinit var resolver: AdminCouponMutationResolver

  private fun couponTemplate(): CouponTemplateModel =
    CouponTemplateModel(
      id = 10L,
      name = "满50减10",
      couponType = CouponType.CASH,
      thresholdCents = 5000,
      discountAmountCents = 1000,
      discountRatePermille = null,
      scope = CouponScope.ALL,
      productId = null,
      validityType = ValidityType.FIXED_RANGE,
      validFrom = LocalDateTime.now().minusDays(1),
      validTo = LocalDateTime.now().plusDays(30),
      validDays = null,
      issuanceType = CouponIssuanceType.AUTO_CLAIM,
      perUserLimit = 1,
      status = CouponTemplateStatus.ACTIVE,
    )

  private fun principal(): UserPrincipal =
    UserPrincipal(
      id = 1L,
      _username = "admin",
      userType = "ADMIN",
      _authorities = listOf(SimpleGrantedAuthority("ROLE_ADMIN")),
    )

  @BeforeEach
  fun setUp() {
    couponMutationService = mockk(relaxed = true)
    couponQueryService = mockk(relaxed = true)
    resolver = AdminCouponMutationResolver(couponMutationService, couponQueryService)
  }

  @Test
  fun `createCouponTemplate should delegate with operator info`() {
    // Given
    every { couponMutationService.createTemplate(1L, "admin", any()) } returns couponTemplate()
    val input =
      CreateCouponTemplateInput(
        couponType = GraphQLCouponType.CASH,
        issuanceType = GraphQLCouponIssuanceType.AUTO_CLAIM,
        name = "满50减10",
        perUserLimit = 1,
        scope = GraphQLCouponScope.ALL,
        thresholdCents = 5000,
        discountAmountCents = 1000,
        validityType = GraphQLValidityType.FIXED_RANGE,
        validFrom = LocalDateTime.now(),
        validTo = LocalDateTime.now().plusDays(30),
      )

    // When
    val result = resolver.createCouponTemplate(input, principal())

    // Then
    assertEquals("满50减10", result.name)
    verify { couponMutationService.createTemplate(1L, "admin", any()) }
  }

  @Test
  fun `updateCouponTemplate should attach received and used counts`() {
    // Given
    every { couponMutationService.updateTemplate(1L, "admin", 10L, any()) } returns couponTemplate()
    every { couponQueryService.receivedCount(10L) } returns 5L
    every { couponQueryService.usedCount(10L) } returns 2L
    val input =
      UpdateCouponTemplateInput(
        couponType = GraphQLCouponType.CASH,
        name = "满50减10",
        perUserLimit = 1,
        scope = GraphQLCouponScope.ALL,
        validityType = GraphQLValidityType.FIXED_RANGE,
      )

    // When
    val result = resolver.updateCouponTemplate(10L, input, principal())

    // Then
    assertEquals(5, result.receivedCount)
    assertEquals(2, result.usedCount)
    verify { couponMutationService.updateTemplate(1L, "admin", 10L, any()) }
  }

  @Test
  fun `disableCouponTemplate should delegate`() {
    // Given
    every { couponMutationService.disableTemplate(1L, "admin", 10L) } returns couponTemplate()
    every { couponQueryService.receivedCount(10L) } returns 0L
    every { couponQueryService.usedCount(10L) } returns 0L

    // When
    val result = resolver.disableCouponTemplate(10L, principal())

    // Then
    assertEquals(0, result.receivedCount)
    verify { couponMutationService.disableTemplate(1L, "admin", 10L) }
  }

  @Test
  fun `issueCoupons should return issued count`() {
    // Given
    every { couponMutationService.issueCoupons(1L, "admin", any()) } returns 8
    val input = IssueCouponsInput(couponTemplateId = 10L, targetType = IssueTargetType.USER, userIds = listOf(1L, 2L))

    // When
    val result = resolver.issueCoupons(input, principal())

    // Then
    assertEquals(8, result)
    verify { couponMutationService.issueCoupons(1L, "admin", any()) }
  }
}
