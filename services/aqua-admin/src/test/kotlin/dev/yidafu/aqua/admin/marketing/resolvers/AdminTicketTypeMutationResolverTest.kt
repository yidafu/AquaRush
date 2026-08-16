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

import dev.yidafu.aqua.api.service.marketing.TicketTypeMutationService
import dev.yidafu.aqua.common.domain.model.TicketTypeModel
import dev.yidafu.aqua.common.domain.model.enums.TicketTypeKind
import dev.yidafu.aqua.common.domain.model.enums.TicketTypeStatus
import dev.yidafu.aqua.common.domain.model.enums.ValidityType
import dev.yidafu.aqua.common.graphql.generated.CreateTicketTypeInput
import dev.yidafu.aqua.common.graphql.generated.UpdateTicketTypeInput
import dev.yidafu.aqua.common.security.UserPrincipal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.time.LocalDateTime
import dev.yidafu.aqua.common.graphql.generated.TicketTypeKind as GraphQLTicketTypeKind
import dev.yidafu.aqua.common.graphql.generated.ValidityType as GraphQLValidityType

class AdminTicketTypeMutationResolverTest :
  FunSpec({
    var ticketTypeMutationService: TicketTypeMutationService = mockk()
    var resolver: AdminTicketTypeMutationResolver = mockk()

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

    fun principal(): UserPrincipal =
      UserPrincipal(
        id = 1L,
        _username = "admin",
        userType = "ADMIN",
        _authorities = listOf(SimpleGrantedAuthority("ROLE_ADMIN")),
      )

    beforeTest {
      ticketTypeMutationService = mockk(relaxed = true)
      resolver = AdminTicketTypeMutationResolver(ticketTypeMutationService)
    }
    test("createTicketType should delegate") {
      // Given
      every { ticketTypeMutationService.createTicketType(1L, "admin", any()) } returns ticketType()
      val input =
        CreateTicketTypeInput(
          giftCount = 0,
          name = "通兑19L",
          packageCount = 10,
          packagePriceCents = 10000,
          perUserLimit = 0,
          ticketType = GraphQLTicketTypeKind.UNIVERSAL,
          validityType = GraphQLValidityType.FIXED_RANGE,
        )

      // When
      val result = resolver.createTicketType(input, principal())

      // Then
      result.name shouldBe "通兑19L"
      verify { ticketTypeMutationService.createTicketType(1L, "admin", any()) }
    }
    test("updateTicketType should delegate") {
      // Given
      every { ticketTypeMutationService.updateTicketType(1L, "admin", 20L, any()) } returns ticketType()
      val input =
        UpdateTicketTypeInput(
          giftCount = 0,
          name = "通兑19L",
          packageCount = 10,
          packagePriceCents = 10000,
          perUserLimit = 0,
          ticketType = GraphQLTicketTypeKind.UNIVERSAL,
          validityType = GraphQLValidityType.FIXED_RANGE,
        )

      // When
      val result = resolver.updateTicketType(20L, input, principal())

      // Then
      result.name shouldBe "通兑19L"
      verify { ticketTypeMutationService.updateTicketType(1L, "admin", 20L, any()) }
    }
    test("disableTicketType should delegate") {
      // Given
      every { ticketTypeMutationService.disableTicketType(1L, "admin", 20L) } returns ticketType()

      // When
      val result = resolver.disableTicketType(20L, principal())

      // Then
      result.name shouldBe "通兑19L"
      verify { ticketTypeMutationService.disableTicketType(1L, "admin", 20L) }
    }
  })
