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

import dev.yidafu.aqua.api.service.marketing.GiftActivityMutationService
import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.common.domain.model.GiftActivityModel
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus
import dev.yidafu.aqua.common.graphql.generated.CreateGiftActivityInput
import dev.yidafu.aqua.common.graphql.generated.GiftActivityType
import dev.yidafu.aqua.common.graphql.generated.PromotionActivityDisplayStatus
import dev.yidafu.aqua.common.graphql.generated.UpdateGiftActivityInput
import dev.yidafu.aqua.common.security.UserPrincipal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.time.LocalDateTime
import dev.yidafu.aqua.common.graphql.generated.PromotionScope as GraphQLPromotionScope

class AdminGiftActivityMutationResolverTest :
  FunSpec({
    var giftActivityMutationService: GiftActivityMutationService = mockk()
    var productQueryApiService: ProductQueryApiService = mockk()
    var resolver: AdminGiftActivityMutationResolver = mockk()

    fun activity(): GiftActivityModel =
      GiftActivityModel(
        id = 10L,
        name = "买赠活动",
        giftProductId = 2L,
        giftQuantity = 1,
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
      giftActivityMutationService = mockk(relaxed = true)
      productQueryApiService = mockk(relaxed = true)
      resolver = AdminGiftActivityMutationResolver(giftActivityMutationService, productQueryApiService)
    }
    test("createGiftActivity should delegate and compute status") {
      // Given
      every { giftActivityMutationService.createActivity(1L, "admin", any()) } returns activity()
      val input =
        CreateGiftActivityInput(
          giftProductId = 2L,
          giftQuantity = 1,
          giftType = GiftActivityType.BUY_GIFT,
          name = "买赠活动",
          scope = GraphQLPromotionScope.ALL,
          validFrom = LocalDateTime.now(),
          validTo = LocalDateTime.now().plusDays(30),
        )

      // When
      val result = resolver.createGiftActivity(input, principal())

      // Then
      result.name shouldBe "买赠活动"
      result.currentStatus shouldBe PromotionActivityDisplayStatus.ONGOING
      verify { giftActivityMutationService.createActivity(1L, "admin", any()) }
    }
    test("disableGiftActivity should compute disabled status") {
      // Given
      every { giftActivityMutationService.disableActivity(1L, "admin", 10L) } returns
        activity().apply { status = PromotionActivityStatus.DISABLED }

      // When
      val result = resolver.disableGiftActivity(10L, principal())

      // Then
      result.currentStatus shouldBe PromotionActivityDisplayStatus.DISABLED
    }
    test("updateGiftActivity should delegate") {
      // Given
      every { giftActivityMutationService.updateActivity(1L, "admin", 10L, any()) } returns activity()
      val input =
        UpdateGiftActivityInput(
          giftProductId = 2L,
          giftQuantity = 1,
          giftType = GiftActivityType.BUY_GIFT,
          name = "买赠活动",
          scope = GraphQLPromotionScope.ALL,
          validFrom = LocalDateTime.now(),
          validTo = LocalDateTime.now().plusDays(30),
        )

      // When
      val result = resolver.updateGiftActivity(10L, input, principal())

      // Then
      result.currentStatus shouldBe PromotionActivityDisplayStatus.ONGOING
      verify { giftActivityMutationService.updateActivity(1L, "admin", 10L, any()) }
    }
  })
