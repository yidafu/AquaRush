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

package dev.yidafu.aqua.client.product.resolvers

import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.enums.ProductModelStatus
import dev.yidafu.aqua.product.service.impl.ProductServiceImpl
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

class ClientProductQueryResolverTest :
  FunSpec({
    var productService: ProductServiceImpl = mockk()
    var resolver: ClientProductQueryResolver = mockk()

    fun product(): ProductModel =
      ProductModel(
        id = 1L,
        name = "桶装水19L",
        price = 2100,
        originalPrice = null,
        depositPrice = null,
        coverImageUrl = "http://x/cover.png",
        imageGallery = null,
        specification = "19L",
        waterSource = "山泉水",
        mineralContent = null,
        stock = 100,
        salesVolume = 0,
        status = ProductModelStatus.ONLINE,
        sortOrder = 1,
        tags = null,
        detailContent = null,
        certificateImages = null,
      )

    beforeTest {
      productService = mockk(relaxed = true)
      resolver = ClientProductQueryResolver(productService)
    }
    test("product should map found product") {
      // Given
      every { productService.findById(1L) } returns product()

      // When
      val result = resolver.product(1L)

      // Then
      result!!.name shouldBe "桶装水19L"
      result.priceCents shouldBe 2100L
      verify { productService.findById(1L) }
    }
    test("product should return null when missing") {
      // Given
      every { productService.findById(999L) } returns null

      // When
      val result = resolver.product(999L)

      // Then
      result.shouldBeNull()
    }
    test("activeProducts should query only ONLINE products with stock and default sort") {
      // Given
      val page = PageImpl(listOf(product()), PageRequest.of(0, 20), 1)
      every {
        productService.productsPaginated(
          match {
            it.status == ProductModelStatus.ONLINE &&
              it.minStock == 0 &&
              it.sortBy == "CREATED_AT_DESC"
          },
          PageRequest.of(0, 20),
        )
      } returns page

      // When
      val result = resolver.activeProducts(null, 0, 20)

      // Then
      result.list.size shouldBe 1
      result.list[0].name shouldBe "桶装水19L"
      result.pageInfo.total shouldBe 1
    }
    test("activeProducts should map sortBy and sortDirection") {
      // Given
      val page = PageImpl(listOf(product()), PageRequest.of(1, 5), 1)
      every {
        productService.productsPaginated(
          match { it.status == ProductModelStatus.ONLINE && it.sortBy == "PRICE_ASC" },
          PageRequest.of(1, 5),
        )
      } returns page

      // When
      val result = resolver.activeProducts("桶装水", 1, 5, "price", "asc")

      // Then
      result.list.size shouldBe 1
      verify {
        productService.productsPaginated(
          match { it.status == ProductModelStatus.ONLINE && it.sortBy == "PRICE_ASC" },
          PageRequest.of(1, 5),
        )
      }
    }
  })
