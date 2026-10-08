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

package dev.yidafu.aqua.product.service.impl

import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.enums.ProductModelStatus
import dev.yidafu.aqua.product.domain.repository.ProductRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.util.Optional

/**
 * 跨模块商品查询服务单元测试
 */
class ProductQueryApiServiceImplTest :
  FunSpec({
    var productRepository: ProductRepository = mockk()
    var service: ProductQueryApiServiceImpl = mockk()

    fun product(id: Long): ProductModel =
      ProductModel().apply {
        this.id = id
        name = "P$id"
      }

    beforeTest {
      productRepository = mockk(relaxed = true)
      service = ProductQueryApiServiceImpl(productRepository)
    }
    test("findById returns product") {
      val p = product(1L)
      every { productRepository.findById(1L) } returns Optional.of(p)
      every { productRepository.findById(2L) } returns Optional.empty()

      service.findById(1L) shouldBe p
      service.findById(2L).shouldBeNull()
    }
    test("findByIds delegates to repository") {
      val expected = listOf(product(1L))
      every { productRepository.findAllById(listOf(1L)) } returns expected

      service.findByIds(listOf(1L)) shouldBe expected
    }
    test("findOnlineProducts filters by online status") {
      val expected = listOf(product(1L))
      every { productRepository.findByStatus(ProductModelStatus.ONLINE) } returns expected

      service.findOnlineProducts() shouldBe expected
    }
    test("findByStatus delegates to repository") {
      val expected = listOf(product(2L))
      every { productRepository.findByStatus(ProductModelStatus.OFFLINE) } returns expected

      service.findByStatus(ProductModelStatus.OFFLINE) shouldBe expected
    }
    test("existsById delegates to repository") {
      every { productRepository.existsById(1L) } returns true

      service.existsById(1L) shouldBe true
      service.existsById(2L) shouldBe false
    }
  })
