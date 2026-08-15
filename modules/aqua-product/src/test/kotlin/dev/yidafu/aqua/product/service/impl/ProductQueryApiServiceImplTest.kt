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
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 跨模块商品查询服务单元测试
 */
class ProductQueryApiServiceImplTest {
  private lateinit var productRepository: ProductRepository
  private lateinit var service: ProductQueryApiServiceImpl

  @BeforeEach
  fun setUp() {
    productRepository = mockk(relaxed = true)
    service = ProductQueryApiServiceImpl(productRepository)
  }

  private fun product(id: Long): ProductModel =
    ProductModel().apply {
      this.id = id
      name = "P$id"
    }

  @Test
  fun `findById returns product`() {
    val p = product(1L)
    every { productRepository.findById(1L) } returns Optional.of(p)
    every { productRepository.findById(2L) } returns Optional.empty()

    assertEquals(p, service.findById(1L))
    assertNull(service.findById(2L))
  }

  @Test
  fun `findByIds delegates to repository`() {
    val expected = listOf(product(1L))
    every { productRepository.findAllById(listOf(1L)) } returns expected

    assertEquals(expected, service.findByIds(listOf(1L)))
  }

  @Test
  fun `findOnlineProducts filters by online status`() {
    val expected = listOf(product(1L))
    every { productRepository.findByStatus(ProductModelStatus.ONLINE) } returns expected

    assertEquals(expected, service.findOnlineProducts())
  }

  @Test
  fun `findByStatus delegates to repository`() {
    val expected = listOf(product(2L))
    every { productRepository.findByStatus(ProductModelStatus.OFFLINE) } returns expected

    assertEquals(expected, service.findByStatus(ProductModelStatus.OFFLINE))
  }

  @Test
  fun `existsById delegates to repository`() {
    every { productRepository.existsById(1L) } returns true

    assertTrue(service.existsById(1L))
    assertFalse(service.existsById(2L))
  }
}
