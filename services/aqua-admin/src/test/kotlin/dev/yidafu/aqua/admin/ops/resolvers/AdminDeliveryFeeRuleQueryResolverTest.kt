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

import dev.yidafu.aqua.api.service.ops.DeliveryFeeRuleQueryService
import dev.yidafu.aqua.common.domain.model.DeliveryFeeRuleModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AdminDeliveryFeeRuleQueryResolverTest {
  private lateinit var deliveryFeeRuleQueryService: DeliveryFeeRuleQueryService
  private lateinit var resolver: AdminDeliveryFeeRuleQueryResolver

  @BeforeEach
  fun setUp() {
    deliveryFeeRuleQueryService = mockk(relaxed = true)
    resolver = AdminDeliveryFeeRuleQueryResolver(deliveryFeeRuleQueryService)
  }

  @Test
  fun `deliveryFeeRule should return effective rule`() {
    // Given
    every { deliveryFeeRuleQueryService.getEffectiveDeliveryFeeRule() } returns
      DeliveryFeeRuleModel(freeFloorLimit = 4, surchargeCents = 200)

    // When
    val result = resolver.deliveryFeeRule()

    // Then
    assertEquals(4, result.freeFloorLimit)
    assertEquals(200L, result.surchargeCents)
    verify { deliveryFeeRuleQueryService.getEffectiveDeliveryFeeRule() }
  }
}
