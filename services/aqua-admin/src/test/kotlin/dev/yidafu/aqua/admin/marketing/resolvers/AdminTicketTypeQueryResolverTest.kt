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

import dev.yidafu.aqua.api.service.marketing.TicketTypeQueryService
import dev.yidafu.aqua.common.domain.model.TicketTypeModel
import dev.yidafu.aqua.common.domain.model.enums.TicketTypeKind
import dev.yidafu.aqua.common.domain.model.enums.TicketTypeStatus
import dev.yidafu.aqua.common.domain.model.enums.ValidityType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

class AdminTicketTypeQueryResolverTest :
  FunSpec({
    var ticketTypeQueryService: TicketTypeQueryService = mockk()
    var resolver: AdminTicketTypeQueryResolver = mockk()

    fun ticketType(): TicketTypeModel =
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

    beforeTest {
      ticketTypeQueryService = mockk(relaxed = true)
      resolver = AdminTicketTypeQueryResolver(ticketTypeQueryService)
    }
    test("ticketTypes should list with defaults") {
      // Given
      every { ticketTypeQueryService.listTicketTypes(null, null, PageRequest.of(0, 20)) } returns
        PageImpl(listOf(ticketType()))

      // When
      val result = resolver.ticketTypes(null)

      // Then
      result.content.size shouldBe 1
      result.content[0].name shouldBe "通兑19L"
      verify { ticketTypeQueryService.listTicketTypes(null, null, PageRequest.of(0, 20)) }
    }
    test("ticketType should return null when missing") {
      // Given
      every { ticketTypeQueryService.getTicketType(999L) } returns null

      // When
      val result = resolver.ticketType(999L)

      // Then
      result.shouldBeNull()
    }
    test("ticketType should map by id") {
      // Given
      every { ticketTypeQueryService.getTicketType(20L) } returns ticketType()

      // When
      val result = resolver.ticketType(20L)

      // Then
      result!!.packageCount shouldBe 10
      verify { ticketTypeQueryService.getTicketType(20L) }
    }
  })
