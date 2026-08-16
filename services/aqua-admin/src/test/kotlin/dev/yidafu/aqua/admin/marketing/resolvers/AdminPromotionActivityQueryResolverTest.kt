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

import dev.yidafu.aqua.api.service.marketing.PromotionActivityQueryService
import dev.yidafu.aqua.common.domain.model.PromotionActivityModel
import dev.yidafu.aqua.common.graphql.generated.PromotionActivityDisplayStatus
import dev.yidafu.aqua.common.graphql.generated.PromotionActivityListInput
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

class AdminPromotionActivityQueryResolverTest :
  FunSpec({
    var promotionActivityQueryService: PromotionActivityQueryService = mockk()
    var resolver: AdminPromotionActivityQueryResolver = mockk()

    beforeTest {
      promotionActivityQueryService = mockk(relaxed = true)
      resolver = AdminPromotionActivityQueryResolver(promotionActivityQueryService)
    }
    test("promotionActivities should list with display status and tiers") {
      // Given
      every { promotionActivityQueryService.listActivities("满", PageRequest.of(0, 20)) } returns
        PageImpl(
          listOf(
            PromotionActivityModel(
              id = 10L,
              name = "满100减10",
              validFrom = LocalDateTime.now().minusDays(1),
              validTo = LocalDateTime.now().plusDays(30),
            ),
          ),
        )
      every { promotionActivityQueryService.listTiers(10L) } returns emptyList()

      // When
      val result = resolver.promotionActivities(PromotionActivityListInput(keyword = "满"))

      // Then
      result.content.size shouldBe 1
      result.content[0].currentStatus shouldBe PromotionActivityDisplayStatus.ONGOING
      verify { promotionActivityQueryService.listActivities("满", PageRequest.of(0, 20)) }
    }
    test("promotionActivity should return null when missing") {
      // Given
      every { promotionActivityQueryService.getActivity(999L) } returns null

      // When
      val result = resolver.promotionActivity(999L)

      // Then
      result.shouldBeNull()
    }
  })
