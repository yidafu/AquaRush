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
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.time.LocalDateTime

class AdminFlashSaleMutationResolverTest :
  FunSpec({
    var flashSaleActivityMutationService: FlashSaleActivityMutationService = mockk()
    var productQueryApiService: ProductQueryApiService = mockk()
    var resolver: AdminFlashSaleMutationResolver = mockk()

    fun activity(): FlashSaleActivityModel =
      FlashSaleActivityModel(
        id = 10L,
        name = "限时特价",
        productId = 1L,
        salePriceCents = 1500,
        validFrom = LocalDateTime.now().minusDays(1),
        validTo = LocalDateTime.now().plusDays(30),
        status = PromotionActivityStatus.ACTIVE,
      )

    fun principal(): UserPrincipal =
      UserPrincipal(
        id = 1L,
        _username = "admin",
        userType = "ADMIN",
        _authorities = listOf(SimpleGrantedAuthority("ROLE_ADMIN")),
      )

    beforeTest {
      flashSaleActivityMutationService = mockk(relaxed = true)
      productQueryApiService = mockk(relaxed = true)
      resolver = AdminFlashSaleMutationResolver(flashSaleActivityMutationService, productQueryApiService)
    }
    test("createFlashSaleActivity should delegate and compute status") {
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
      result.name shouldBe "限时特价"
      result.currentStatus shouldBe PromotionActivityDisplayStatus.ONGOING
      verify { flashSaleActivityMutationService.createActivity(1L, "admin", any()) }
    }
    test("disableFlashSaleActivity should compute disabled status") {
      // Given
      every { flashSaleActivityMutationService.disableActivity(1L, "admin", 10L) } returns
        activity().apply { status = PromotionActivityStatus.DISABLED }

      // When
      val result = resolver.disableFlashSaleActivity(10L, principal())

      // Then
      result.currentStatus shouldBe PromotionActivityDisplayStatus.DISABLED
    }
    test("updateFlashSaleActivity should delegate") {
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
      result.currentStatus shouldBe PromotionActivityDisplayStatus.ONGOING
      verify { flashSaleActivityMutationService.updateActivity(1L, "admin", 10L, any()) }
    }
  })
