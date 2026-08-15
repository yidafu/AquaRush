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

import dev.yidafu.aqua.api.service.marketing.FlashSaleActivityMutationService
import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.common.domain.model.FlashSaleActivityModel
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus
import dev.yidafu.aqua.common.graphql.generated.CreateFlashSaleActivityInput
import dev.yidafu.aqua.common.graphql.generated.PromotionActivityDisplayStatus
import dev.yidafu.aqua.common.graphql.generated.UpdateFlashSaleActivityInput
import dev.yidafu.aqua.common.security.UserPrincipal
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.time.LocalDateTime
import kotlin.test.assertEquals

class AdminFlashSaleMutationResolverTest {
  private lateinit var flashSaleActivityMutationService: FlashSaleActivityMutationService
  private lateinit var productQueryApiService: ProductQueryApiService
  private lateinit var resolver: AdminFlashSaleMutationResolver

  private fun activity(): FlashSaleActivityModel =
    FlashSaleActivityModel(
      id = 10L,
      name = "限时特价",
      productId = 1L,
      salePriceCents = 1500,
      validFrom = LocalDateTime.now().minusDays(1),
      validTo = LocalDateTime.now().plusDays(30),
      status = PromotionActivityStatus.ACTIVE,
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
    flashSaleActivityMutationService = mockk(relaxed = true)
    productQueryApiService = mockk(relaxed = true)
    resolver = AdminFlashSaleMutationResolver(flashSaleActivityMutationService, productQueryApiService)
  }

  @Test
  fun `createFlashSaleActivity should delegate and compute status`() {
    // Given
    every { flashSaleActivityMutationService.createActivity(1L, "admin", any()) } returns activity()
    val input =
      CreateFlashSaleActivityInput(
        name = "限时特价",
        productId = 1L,
        salePriceCents = 1500,
        validFrom = LocalDateTime.now(),
        validTo = LocalDateTime.now().plusDays(30),
      )

    // When
    val result = resolver.createFlashSaleActivity(input, principal())

    // Then
    assertEquals("限时特价", result.name)
    assertEquals(PromotionActivityDisplayStatus.ONGOING, result.currentStatus)
    verify { flashSaleActivityMutationService.createActivity(1L, "admin", any()) }
  }

  @Test
  fun `disableFlashSaleActivity should compute disabled status`() {
    // Given
    every { flashSaleActivityMutationService.disableActivity(1L, "admin", 10L) } returns
      activity().apply { status = PromotionActivityStatus.DISABLED }

    // When
    val result = resolver.disableFlashSaleActivity(10L, principal())

    // Then
    assertEquals(PromotionActivityDisplayStatus.DISABLED, result.currentStatus)
  }

  @Test
  fun `updateFlashSaleActivity should delegate`() {
    // Given
    every { flashSaleActivityMutationService.updateActivity(1L, "admin", 10L, any()) } returns activity()
    val input =
      UpdateFlashSaleActivityInput(
        name = "限时特价",
        productId = 1L,
        salePriceCents = 1600,
        validFrom = LocalDateTime.now(),
        validTo = LocalDateTime.now().plusDays(30),
      )

    // When
    val result = resolver.updateFlashSaleActivity(10L, input, principal())

    // Then
    assertEquals(PromotionActivityDisplayStatus.ONGOING, result.currentStatus)
    verify { flashSaleActivityMutationService.updateActivity(1L, "admin", 10L, any()) }
  }
}
