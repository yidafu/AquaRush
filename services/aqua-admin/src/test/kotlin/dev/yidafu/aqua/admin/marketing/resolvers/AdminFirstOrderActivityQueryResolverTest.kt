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

import dev.yidafu.aqua.api.service.marketing.FirstOrderActivityQueryService
import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.common.domain.model.FirstOrderActivityModel
import dev.yidafu.aqua.common.graphql.generated.FirstOrderActivityListInput
import dev.yidafu.aqua.common.graphql.generated.PromotionActivityDisplayStatus
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

class AdminFirstOrderActivityQueryResolverTest :
  FunSpec({
    var firstOrderActivityQueryService: FirstOrderActivityQueryService = mockk()
    var productQueryApiService: ProductQueryApiService = mockk()
    var resolver: AdminFirstOrderActivityQueryResolver = mockk()

    beforeTest {
      firstOrderActivityQueryService = mockk(relaxed = true)
      productQueryApiService = mockk(relaxed = true)
      resolver = AdminFirstOrderActivityQueryResolver(firstOrderActivityQueryService, productQueryApiService)
    }
    test("firstOrderActivities should list with display status") {
      // Given
      every { firstOrderActivityQueryService.listActivities("首单", PageRequest.of(0, 20)) } returns
        PageImpl(
          listOf(
            FirstOrderActivityModel(
              id = 10L,
              name = "首单立减10元",
              validFrom = LocalDateTime.now().minusDays(1),
              validTo = LocalDateTime.now().plusDays(30),
            ),
          ),
        )

      // When
      val result = resolver.firstOrderActivities(FirstOrderActivityListInput(keyword = "首单"))

      // Then
      result.content.size shouldBe 1
      result.content[0].currentStatus shouldBe PromotionActivityDisplayStatus.ONGOING
      verify { firstOrderActivityQueryService.listActivities("首单", PageRequest.of(0, 20)) }
    }
    test("firstOrderActivity should return null when missing") {
      // Given
      every { firstOrderActivityQueryService.getActivity(999L) } returns null

      // When
      val result = resolver.firstOrderActivity(999L)

      // Then
      result.shouldBeNull()
    }
  })
