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

package dev.yidafu.aqua.admin.ops.resolvers

import dev.yidafu.aqua.api.service.ops.DeliveryFeeRuleMutationService
import dev.yidafu.aqua.common.domain.model.DeliveryFeeRuleModel
import dev.yidafu.aqua.common.graphql.generated.SaveDeliveryFeeRuleInput
import dev.yidafu.aqua.common.security.UserPrincipal
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.core.authority.SimpleGrantedAuthority
import kotlin.test.assertEquals

class AdminDeliveryFeeRuleMutationResolverTest {
  private lateinit var deliveryFeeRuleMutationService: DeliveryFeeRuleMutationService
  private lateinit var resolver: AdminDeliveryFeeRuleMutationResolver

  private fun principal(): UserPrincipal =
    UserPrincipal(
      id = 1L,
      _username = "admin",
      userType = "ADMIN",
      _authorities = listOf(SimpleGrantedAuthority("ROLE_ADMIN")),
    )

  @BeforeEach
  fun setUp() {
    deliveryFeeRuleMutationService = mockk(relaxed = true)
    resolver = AdminDeliveryFeeRuleMutationResolver(deliveryFeeRuleMutationService)
  }

  @Test
  fun `saveDeliveryFeeRule should delegate`() {
    // Given
    every { deliveryFeeRuleMutationService.saveDeliveryFeeRule(1L, "admin", any()) } returns
      DeliveryFeeRuleModel(freeFloorLimit = 4, surchargeCents = 200)
    val input = SaveDeliveryFeeRuleInput(freeFloorLimit = 4, surchargeCents = 200)

    // When
    val result = resolver.saveDeliveryFeeRule(input, principal())

    // Then
    assertEquals(4, result.freeFloorLimit)
    assertEquals(200L, result.surchargeCents)
    verify { deliveryFeeRuleMutationService.saveDeliveryFeeRule(1L, "admin", any()) }
  }
}
