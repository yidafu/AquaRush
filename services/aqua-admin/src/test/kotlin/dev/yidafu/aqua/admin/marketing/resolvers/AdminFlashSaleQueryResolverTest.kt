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

import dev.yidafu.aqua.api.service.marketing.FlashSaleActivityQueryService
import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.common.domain.model.FlashSaleActivityModel
import dev.yidafu.aqua.common.graphql.generated.FlashSaleActivityListInput
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

class AdminFlashSaleQueryResolverTest :
  FunSpec({
    var flashSaleActivityQueryService: FlashSaleActivityQueryService = mockk()
    var productQueryApiService: ProductQueryApiService = mockk()
    var resolver: AdminFlashSaleQueryResolver = mockk()

    beforeTest {
      flashSaleActivityQueryService = mockk(relaxed = true)
      productQueryApiService = mockk(relaxed = true)
      resolver = AdminFlashSaleQueryResolver(flashSaleActivityQueryService, productQueryApiService)
    }
    test("flashSaleActivities should list with display status") {
      // Given
      every { flashSaleActivityQueryService.listActivities("特价", PageRequest.of(0, 20)) } returns
        PageImpl(
          listOf(
            FlashSaleActivityModel(
              id = 10L,
              name = "限时特价",
              productId = 1L,
              salePriceCents = 1500,
              validFrom = LocalDateTime.now().minusDays(1),
              validTo = LocalDateTime.now().plusDays(30),
            ),
          ),
        )

      // When
      val result = resolver.flashSaleActivities(FlashSaleActivityListInput(keyword = "特价"))

      // Then
      result.content.size shouldBe 1
      result.content[0].currentStatus shouldBe PromotionActivityDisplayStatus.ONGOING
      verify { flashSaleActivityQueryService.listActivities("特价", PageRequest.of(0, 20)) }
    }
    test("flashSaleActivity should return null when missing") {
      // Given
      every { flashSaleActivityQueryService.getActivity(999L) } returns null

      // When
      val result = resolver.flashSaleActivity(999L)

      // Then
      result.shouldBeNull()
    }
  })
