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

import dev.yidafu.aqua.api.service.marketing.FirstOrderActivityMutationService
import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.common.domain.model.FirstOrderActivityModel
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus
import dev.yidafu.aqua.common.graphql.generated.CreateFirstOrderActivityInput
import dev.yidafu.aqua.common.graphql.generated.FirstOrderActivityType
import dev.yidafu.aqua.common.graphql.generated.PromotionActivityDisplayStatus
import dev.yidafu.aqua.common.graphql.generated.UpdateFirstOrderActivityInput
import dev.yidafu.aqua.common.security.UserPrincipal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.time.LocalDateTime
import dev.yidafu.aqua.common.graphql.generated.PromotionScope as GraphQLPromotionScope

class AdminFirstOrderActivityMutationResolverTest :
  FunSpec({
    var firstOrderActivityMutationService: FirstOrderActivityMutationService = mockk()
    var productQueryApiService: ProductQueryApiService = mockk()
    var resolver: AdminFirstOrderActivityMutationResolver = mockk()

    fun activity(status: PromotionActivityStatus = PromotionActivityStatus.ACTIVE): FirstOrderActivityModel =
      FirstOrderActivityModel(
        id = 10L,
        name = "首单立减10元",
        status = status,
        validFrom = LocalDateTime.now().minusDays(1),
        validTo = LocalDateTime.now().plusDays(30),
      )

    fun principal(): UserPrincipal =
      UserPrincipal(
        id = 1L,
        _username = "admin",
        userType = "ADMIN",
        _authorities = listOf(SimpleGrantedAuthority("ROLE_ADMIN")),
      )

    fun createInput(): CreateFirstOrderActivityInput =
      CreateFirstOrderActivityInput(
        activityType = FirstOrderActivityType.FIRST_ORDER_REDUCE,
        name = "首单立减10元",
        scope = GraphQLPromotionScope.ALL,
        validFrom = LocalDateTime.now(),
        validTo = LocalDateTime.now().plusDays(30),
      )

    beforeTest {
      firstOrderActivityMutationService = mockk(relaxed = true)
      productQueryApiService = mockk(relaxed = true)
      resolver = AdminFirstOrderActivityMutationResolver(firstOrderActivityMutationService, productQueryApiService)
    }
    test("createFirstOrderActivity should delegate and compute ongoing status") {
      // Given
      every { firstOrderActivityMutationService.createActivity(1L, "admin", any()) } returns activity()

      // When
      val result = resolver.createFirstOrderActivity(createInput(), principal())

      // Then
      result.name shouldBe "首单立减10元"
      result.currentStatus shouldBe PromotionActivityDisplayStatus.ONGOING
      verify { firstOrderActivityMutationService.createActivity(1L, "admin", any()) }
    }
    test("updateFirstOrderActivity should delegate") {
      // Given
      every { firstOrderActivityMutationService.updateActivity(1L, "admin", 10L, any()) } returns activity()
      val input =
        UpdateFirstOrderActivityInput(
          activityType = FirstOrderActivityType.FIRST_ORDER_REDUCE,
          name = "首单立减10元",
          scope = GraphQLPromotionScope.ALL,
          validFrom = LocalDateTime.now(),
          validTo = LocalDateTime.now().plusDays(30),
        )

      // When
      val result = resolver.updateFirstOrderActivity(10L, input, principal())

      // Then
      result.currentStatus shouldBe PromotionActivityDisplayStatus.ONGOING
      verify { firstOrderActivityMutationService.updateActivity(1L, "admin", 10L, any()) }
    }
    test("disableFirstOrderActivity should compute disabled status") {
      // Given
      every { firstOrderActivityMutationService.disableActivity(1L, "admin", 10L) } returns activity(PromotionActivityStatus.DISABLED)

      // When
      val result = resolver.disableFirstOrderActivity(10L, principal())

      // Then
      result.currentStatus shouldBe PromotionActivityDisplayStatus.DISABLED
    }
    test("toFirstOrderActivity should compute upcoming status for future activity") {
      // Given
      every { firstOrderActivityMutationService.createActivity(1L, "admin", any()) } returns
        FirstOrderActivityModel(
          id = 10L,
          name = "首单立减10元",
          validFrom = LocalDateTime.now().plusDays(1),
          validTo = LocalDateTime.now().plusDays(30),
        )

      // When
      val result = resolver.createFirstOrderActivity(createInput(), principal())

      // Then
      result.currentStatus shouldBe PromotionActivityDisplayStatus.UPCOMING
    }
    test("toFirstOrderActivity should compute ended status for past activity") {
      // Given
      every { firstOrderActivityMutationService.createActivity(1L, "admin", any()) } returns
        FirstOrderActivityModel(
          id = 10L,
          name = "首单立减10元",
          validFrom = LocalDateTime.now().minusDays(10),
          validTo = LocalDateTime.now().minusDays(1),
        )

      // When
      val result = resolver.createFirstOrderActivity(createInput(), principal())

      // Then
      result.currentStatus shouldBe PromotionActivityDisplayStatus.ENDED
    }
  })
