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

import dev.yidafu.aqua.api.service.marketing.CouponQueryService
import dev.yidafu.aqua.common.domain.model.CouponTemplateModel
import dev.yidafu.aqua.common.domain.model.enums.CouponIssuanceType
import dev.yidafu.aqua.common.domain.model.enums.CouponScope
import dev.yidafu.aqua.common.domain.model.enums.CouponTemplateStatus
import dev.yidafu.aqua.common.domain.model.enums.CouponType
import dev.yidafu.aqua.common.domain.model.enums.ValidityType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AdminCouponQueryResolverTest {
  private lateinit var couponQueryService: CouponQueryService
  private lateinit var resolver: AdminCouponQueryResolver

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

  @BeforeEach
  fun setUp() {
    couponQueryService = mockk(relaxed = true)
    resolver = AdminCouponQueryResolver(couponQueryService)
  }

  @Test
  fun `couponTemplates should list with counts`() {
    // Given
    every { couponQueryService.listTemplates(null, null, PageRequest.of(0, 20)) } returns
      PageImpl(listOf(couponTemplate()))
    every { couponQueryService.receivedCount(10L) } returns 3L
    every { couponQueryService.usedCount(10L) } returns 1L

    // When
    val result = resolver.couponTemplates(null)

    // Then
    assertEquals(1, result.content.size)
    assertEquals(3, result.content[0].receivedCount)
    assertEquals(1, result.content[0].usedCount)
    verify { couponQueryService.listTemplates(null, null, PageRequest.of(0, 20)) }
  }

  @Test
  fun `couponTemplate should return null when missing`() {
    // Given
    every { couponQueryService.getTemplate(999L) } returns null

    // When
    val result = resolver.couponTemplate(999L)

    // Then
    assertNull(result)
  }

  @Test
  fun `couponTemplate should map template with counts`() {
    // Given
    every { couponQueryService.getTemplate(10L) } returns couponTemplate()
    every { couponQueryService.receivedCount(10L) } returns 4L
    every { couponQueryService.usedCount(10L) } returns 2L

    // When
    val result = resolver.couponTemplate(10L)

    // Then
    assertEquals("满50减10", result!!.name)
    assertEquals(4, result.receivedCount)
    verify { couponQueryService.getTemplate(10L) }
  }
}
