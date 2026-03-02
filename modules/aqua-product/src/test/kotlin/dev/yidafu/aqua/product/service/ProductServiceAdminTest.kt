/*
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

package dev.yidafu.aqua.product.service

import dev.yidafu.aqua.common.graphql.generated.ProductStatus
import dev.yidafu.aqua.common.graphql.generated.ProductUpdateRequest
import dev.yidafu.aqua.product.domain.model.ProductModel
import dev.yidafu.aqua.product.domain.repository.ProductRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import java.util.*

class ProductServiceAdminTest {
  @Mock
  private lateinit var productRepository: ProductRepository

  @InjectMocks
  private lateinit var productService: ProductService

  @BeforeEach
  fun setUp() {
    MockitoAnnotations.openMocks(this)
  }

  @Test
  fun `batchUpdateProducts should update multiple products successfully`() {
    // Given
    val product1 = createTestProduct(1L, "Product 1", 1000L, 50)
    val product2 = createTestProduct(2L, "Product 2", 2000L, 30)

    val updates =
      listOf(
        ProductUpdateRequest(
          id = 1L,
          name = "Updated Product 1",
          price = 1500L,
          coverImageUrl = "http://example.com/image1.jpg",
          detailImages = null,
          description = "Updated description 1",
          stock = 60,
          status = ProductStatus.ONLINE,
        ),
        ProductUpdateRequest(
          id = 2L,
          name = "Updated Product 2",
          price = 2500L,
          coverImageUrl = "http://example.com/image2.jpg",
          detailImages = null,
          description = "Updated description 2",
          stock = 40,
          status = ProductStatus.ONLINE,
        ),
      )

    whenever(productRepository.findById(1L)).thenReturn(Optional.of(product1))
    whenever(productRepository.findById(2L)).thenReturn(Optional.of(product2))
    whenever(productRepository.save(any<ProductModel>())).thenReturn(product1).thenReturn(product2)

    // When
    val result = productService.batchUpdateProducts(updates)

    // Then
    assert(result.size == 2)
    verify(productRepository, times(2)).save(any<ProductModel>())
  }

  @Test
  fun `batchUpdateProducts should throw exception when product not found`() {
    // Given
    val updates =
      listOf(
        ProductUpdateRequest(
          id = 999L,
          name = "Updated Product",
          price = 1500L,
          coverImageUrl = null,
          detailImages = null,
          description = null,
          stock = 60,
          status = ProductStatus.ONLINE,
        ),
      )

    whenever(productRepository.findById(999L)).thenReturn(Optional.empty())

    // When & Then
    assertThrows<IllegalArgumentException> {
      productService.batchUpdateProducts(updates)
    }
  }

  @Test
  fun `getProductsByStatus should return filtered products`() {
    // Given
    val onlineProduct = createTestProduct(1L, "Online Product", 1000L, 50, ProductStatus.ONLINE)
    val offlineProduct = createTestProduct(2L, "Offline Product", 2000L, 30, ProductStatus.OFFLINE)

    val allProducts = listOf(onlineProduct, offlineProduct)
    whenever(productRepository.findByStatus(ProductStatus.ONLINE)).thenReturn(listOf(onlineProduct))

    // When
    val result = productService.getProductsByStatus(ProductStatus.ONLINE)

    // Then
    assert(result.size == 1)
    assert(result[0].status == ProductStatus.ONLINE)
    verify(productRepository).findByStatus(ProductStatus.ONLINE)
  }

  @Test
  fun `getLowStockProducts should return products below threshold`() {
    // Given
    val lowStockProduct = createTestProduct(1L, "Low Stock Product", 1000L, 5)
    val normalStockProduct = createTestProduct(2L, "Normal Stock Product", 2000L, 50)

    val allProducts = listOf(lowStockProduct, normalStockProduct)
    whenever(productRepository.findAll()).thenReturn(allProducts)

    // When
    val result = productService.getLowStockProducts(threshold = 10)

    // Then
    assert(result.size == 1)
    assert(result[0].stock == 5)
    verify(productRepository).findAll()
  }

  @Test
  fun `getProductStatistics should return correct statistics`() {
    // Given
    val onlineProduct1 = createTestProduct(1L, "Online Product 1", 1000L, 50, ProductStatus.ONLINE)
    val onlineProduct2 = createTestProduct(2L, "Online Product 2", 2000L, 30, ProductStatus.ONLINE)
    val offlineProduct = createTestProduct(3L, "Offline Product", 1500L, 5, ProductStatus.OFFLINE)

    val allProducts = listOf(onlineProduct1, onlineProduct2, offlineProduct)
    whenever(productRepository.findAll()).thenReturn(allProducts)

    // When
    val result = productService.getProductStatistics()

    // Then
    assert(result.totalProducts == 3)
    assert(result.onlineProducts == 2)
    assert(result.offlineProducts == 1)
    assert(result.lowStockProducts == 1) // 5 <= 10 threshold
    assert(result.totalValue == 4500L) // 1000 + 2000 + 1500
    assert(result.averagePrice == 1500L) // 4500 / 3
    verify(productRepository).findAll()
  }

  @Test
  fun `getProductStatistics should handle empty product list`() {
    // Given
    whenever(productRepository.findAll()).thenReturn(emptyList())

    // When
    val result = productService.getProductStatistics()

    // Then
    assert(result.totalProducts == 0)
    assert(result.onlineProducts == 0)
    assert(result.offlineProducts == 0)
    assert(result.lowStockProducts == 0)
    assert(result.totalValue == 0L)
    assert(result.averagePrice == 0L)
    verify(productRepository).findAll()
  }

  private fun createTestProduct(
    id: Long,
    name: String,
    priceCents: Long,
    stock: Int,
    status: ProductStatus = ProductStatus.OFFLINE,
  ): ProductModel =
    ProductModel(
      id = id,
      name = name,
      price = priceCents,
      coverImageUrl = "http://example.com/image.jpg",
      detailImages = null,
      description = "Test description",
      stock = stock,
      status = status,
    )
}
