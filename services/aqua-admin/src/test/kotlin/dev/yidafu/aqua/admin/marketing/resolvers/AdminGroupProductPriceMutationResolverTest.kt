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

import dev.yidafu.aqua.api.service.marketing.GroupProductPriceMutationService
import dev.yidafu.aqua.common.graphql.generated.GroupProductPriceItemInput
import dev.yidafu.aqua.common.graphql.generated.SaveGroupProductPricesInput
import dev.yidafu.aqua.common.security.UserPrincipal
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.core.authority.SimpleGrantedAuthority
import kotlin.test.assertTrue

class AdminGroupProductPriceMutationResolverTest {
  private lateinit var groupProductPriceMutationService: GroupProductPriceMutationService
  private lateinit var resolver: AdminGroupProductPriceMutationResolver

  private fun principal(): UserPrincipal =
    UserPrincipal(
      id = 1L,
      _username = "admin",
      userType = "ADMIN",
      _authorities = listOf(SimpleGrantedAuthority("ROLE_ADMIN")),
    )

  @BeforeEach
  fun setUp() {
    groupProductPriceMutationService = mockk(relaxed = true)
    resolver = AdminGroupProductPriceMutationResolver(groupProductPriceMutationService)
  }

  @Test
  fun `saveGroupProductPrices should delegate and return true`() {
    // Given
    every { groupProductPriceMutationService.savePrices(1L, "admin", any()) } returns true
    val input =
      SaveGroupProductPricesInput(
        customerGroupId = 10L,
        items = listOf(GroupProductPriceItemInput(productId = 1L, priceCents = 1900)),
      )

    // When
    val result = resolver.saveGroupProductPrices(input, principal())

    // Then
    assertTrue(result)
    verify { groupProductPriceMutationService.savePrices(1L, "admin", any()) }
  }
}
