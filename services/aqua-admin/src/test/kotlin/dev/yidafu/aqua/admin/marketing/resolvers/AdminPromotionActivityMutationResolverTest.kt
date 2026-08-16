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

import dev.yidafu.aqua.api.service.marketing.PromotionActivityMutationService
import dev.yidafu.aqua.api.service.marketing.PromotionActivityQueryService
import dev.yidafu.aqua.common.domain.model.PromotionActivityModel
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus
import dev.yidafu.aqua.common.graphql.generated.CreatePromotionActivityInput
import dev.yidafu.aqua.common.graphql.generated.PromotionActivityDisplayStatus
import dev.yidafu.aqua.common.graphql.generated.PromotionActivityType
import dev.yidafu.aqua.common.graphql.generated.PromotionTierInput
import dev.yidafu.aqua.common.graphql.generated.UpdatePromotionActivityInput
import dev.yidafu.aqua.common.security.UserPrincipal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.time.LocalDateTime
import dev.yidafu.aqua.common.graphql.generated.PromotionScope as GraphQLPromotionScope

class AdminPromotionActivityMutationResolverTest :
  FunSpec({
    var promotionActivityMutationService: PromotionActivityMutationService = mockk()
    var promotionActivityQueryService: PromotionActivityQueryService = mockk()
    var resolver: AdminPromotionActivityMutationResolver = mockk()

    fun activity(): PromotionActivityModel =
      PromotionActivityModel(
        id = 10L,
        name = "满100减10",
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

    fun createInput(): CreatePromotionActivityInput =
      CreatePromotionActivityInput(
        activityType = PromotionActivityType.FULL_REDUCE,
        name = "满100减10",
        scope = GraphQLPromotionScope.ALL,
        tiers = listOf(PromotionTierInput(thresholdCents = 10000, discountAmountCents = 1000)),
        validFrom = LocalDateTime.now(),
        validTo = LocalDateTime.now().plusDays(30),
      )

    beforeTest {
      promotionActivityMutationService = mockk(relaxed = true)
      promotionActivityQueryService = mockk(relaxed = true)
      resolver = AdminPromotionActivityMutationResolver(promotionActivityMutationService, promotionActivityQueryService)
    }
    test("createPromotionActivity should delegate and compute status") {
      // Given
      every { promotionActivityMutationService.createActivity(1L, "admin", any()) } returns activity()
      every { promotionActivityQueryService.listTiers(10L) } returns emptyList()

      // When
      val result = resolver.createPromotionActivity(createInput(), principal())

      // Then
      result.name shouldBe "满100减10"
      result.currentStatus shouldBe PromotionActivityDisplayStatus.ONGOING
      verify { promotionActivityMutationService.createActivity(1L, "admin", any()) }
    }
    test("updatePromotionActivity should delegate") {
      // Given
      every { promotionActivityMutationService.updateActivity(1L, "admin", 10L, any()) } returns activity()
      every { promotionActivityQueryService.listTiers(10L) } returns emptyList()
      val input =
        UpdatePromotionActivityInput(
          activityType = PromotionActivityType.FULL_REDUCE,
          name = "满100减10",
          scope = GraphQLPromotionScope.ALL,
          tiers = listOf(PromotionTierInput(thresholdCents = 10000, discountAmountCents = 1000)),
          validFrom = LocalDateTime.now(),
          validTo = LocalDateTime.now().plusDays(30),
        )

      // When
      val result = resolver.updatePromotionActivity(10L, input, principal())

      // Then
      result.currentStatus shouldBe PromotionActivityDisplayStatus.ONGOING
      verify { promotionActivityMutationService.updateActivity(1L, "admin", 10L, any()) }
    }
    test("disablePromotionActivity should compute disabled status") {
      // Given
      every { promotionActivityMutationService.disableActivity(1L, "admin", 10L) } returns
        activity().apply { status = PromotionActivityStatus.DISABLED }
      every { promotionActivityQueryService.listTiers(10L) } returns emptyList()

      // When
      val result = resolver.disablePromotionActivity(10L, principal())

      // Then
      result.currentStatus shouldBe PromotionActivityDisplayStatus.DISABLED
    }
  })
