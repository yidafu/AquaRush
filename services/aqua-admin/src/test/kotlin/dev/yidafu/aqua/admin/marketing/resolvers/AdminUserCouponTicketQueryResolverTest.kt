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

import dev.yidafu.aqua.api.service.marketing.CouponQueryApiService
import dev.yidafu.aqua.api.service.marketing.TicketQueryApiService
import dev.yidafu.aqua.api.service.marketing.TicketTypeQueryService
import dev.yidafu.aqua.common.domain.model.TicketTypeModel
import dev.yidafu.aqua.common.domain.model.UserCouponModel
import dev.yidafu.aqua.common.domain.model.UserTicketModel
import dev.yidafu.aqua.common.domain.model.enums.CouponScope
import dev.yidafu.aqua.common.domain.model.enums.CouponType
import dev.yidafu.aqua.common.domain.model.enums.TicketTypeKind
import dev.yidafu.aqua.common.domain.model.enums.TicketTypeStatus
import dev.yidafu.aqua.common.domain.model.enums.UserCouponStatus
import dev.yidafu.aqua.common.domain.model.enums.UserTicketStatus
import dev.yidafu.aqua.common.domain.model.enums.ValidityType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import dev.yidafu.aqua.common.graphql.generated.UserCouponStatus as GraphQLUserCouponStatus
import dev.yidafu.aqua.common.graphql.generated.UserTicketStatus as GraphQLUserTicketStatus

class AdminUserCouponTicketQueryResolverTest {
  private lateinit var couponQueryApiService: CouponQueryApiService
  private lateinit var ticketQueryApiService: TicketQueryApiService
  private lateinit var ticketTypeQueryService: TicketTypeQueryService
  private lateinit var resolver: AdminUserCouponTicketQueryResolver

  private fun userCoupon(): UserCouponModel =
    UserCouponModel(
      id = 1L,
      userId = 1L,
      couponTemplateId = 10L,
      status = UserCouponStatus.RECEIVED,
      name = "满50减10",
      couponType = CouponType.CASH,
      thresholdCents = 5000,
      discountAmountCents = 1000,
      discountRatePermille = null,
      scope = CouponScope.ALL,
      productId = null,
      expireTime = LocalDateTime.now().plusDays(30),
    )

  private fun userTicket(): UserTicketModel =
    UserTicketModel(
      id = 1L,
      userId = 1L,
      ticketTypeId = 20L,
      purchaseId = 1L,
      status = UserTicketStatus.UNUSED,
      expireTime = LocalDateTime.now().plusDays(30),
      isGift = false,
    )

  private fun ticketType(): TicketTypeModel =
    TicketTypeModel(
      id = 20L,
      name = "通兑19L",
      ticketType = TicketTypeKind.UNIVERSAL,
      productId = null,
      specification = "19L",
      packageCount = 10,
      packagePriceCents = 10000,
      giftCount = 0,
      validityType = ValidityType.FIXED_RANGE,
      validFrom = LocalDateTime.now().minusDays(1),
      validTo = LocalDateTime.now().plusDays(365),
      perUserLimit = 0,
      status = TicketTypeStatus.ACTIVE,
    )

  @BeforeEach
  fun setUp() {
    couponQueryApiService = mockk(relaxed = true)
    ticketQueryApiService = mockk(relaxed = true)
    ticketTypeQueryService = mockk(relaxed = true)
    resolver = AdminUserCouponTicketQueryResolver(couponQueryApiService, ticketQueryApiService, ticketTypeQueryService)
  }

  @Test
  fun `userCoupons should map user coupons`() {
    // Given
    every { couponQueryApiService.findUserCoupons(1L, UserCouponStatus.RECEIVED) } returns listOf(userCoupon())

    // When
    val result = resolver.userCoupons(1L, GraphQLUserCouponStatus.RECEIVED)

    // Then
    assertEquals(1, result.size)
    assertEquals("满50减10", result[0].name)
    assertEquals(false, result[0].usable)
    verify { couponQueryApiService.findUserCoupons(1L, UserCouponStatus.RECEIVED) }
  }

  @Test
  fun `userTickets should resolve ticket type info`() {
    // Given
    every { ticketQueryApiService.findUserTickets(1L, UserTicketStatus.UNUSED) } returns listOf(userTicket())
    every { ticketTypeQueryService.getTicketType(20L) } returns ticketType()

    // When
    val result = resolver.userTickets(1L, GraphQLUserTicketStatus.UNUSED)

    // Then
    assertEquals(1, result.size)
    assertEquals("通兑19L", result[0].ticketTypeName)
    assertEquals("19L", result[0].specification)
    verify { ticketQueryApiService.findUserTickets(1L, UserTicketStatus.UNUSED) }
  }

  @Test
  fun `userTickets should fall back to default name when ticket type missing`() {
    // Given
    every { ticketQueryApiService.findUserTickets(1L, null) } returns listOf(userTicket())
    every { ticketTypeQueryService.getTicketType(20L) } returns null

    // When
    val result = resolver.userTickets(1L, null)

    // Then
    assertEquals("水票", result[0].ticketTypeName)
  }
}
