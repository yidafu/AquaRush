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
import dev.yidafu.aqua.common.graphql.generated.DeliveryFeeTierInput
import dev.yidafu.aqua.common.graphql.generated.SaveDeliveryFeeRuleInput
import dev.yidafu.aqua.common.security.UserPrincipal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.security.core.authority.SimpleGrantedAuthority

class AdminDeliveryFeeRuleMutationResolverTest :
  FunSpec({
    var deliveryFeeRuleMutationService: DeliveryFeeRuleMutationService = mockk()
    var resolver: AdminDeliveryFeeRuleMutationResolver = mockk()

    fun principal(): UserPrincipal =
      UserPrincipal(
        id = 1L,
        _username = "admin",
        userType = "ADMIN",
        _authorities = listOf(SimpleGrantedAuthority("ROLE_ADMIN")),
      )

    beforeTest {
      deliveryFeeRuleMutationService = mockk(relaxed = true)
      resolver = AdminDeliveryFeeRuleMutationResolver(deliveryFeeRuleMutationService)
    }
    test("saveDeliveryFeeRule should delegate") {
      // Given
      every { deliveryFeeRuleMutationService.saveDeliveryFeeRule(1L, "admin", any()) } returns
        DeliveryFeeRuleModel(freeFloorLimit = 4, surchargeCents = 200)
      val input =
        SaveDeliveryFeeRuleInput(
          freeFloorLimit = 4,
          surchargeCents = 200,
          tiers =
            listOf(
              DeliveryFeeTierInput(feeCents = 200, fromFloor = 5, toFloor = 6),
              DeliveryFeeTierInput(feeCents = 500, fromFloor = 7, toFloor = 9),
              DeliveryFeeTierInput(feeCents = 800, fromFloor = 10, toFloor = null),
            ),
        )

      // When
      val result = resolver.saveDeliveryFeeRule(input, principal())

      // Then
      result.freeFloorLimit shouldBe 4
      result.surchargeCents shouldBe 200L
      verify { deliveryFeeRuleMutationService.saveDeliveryFeeRule(1L, "admin", any()) }
    }
  })
