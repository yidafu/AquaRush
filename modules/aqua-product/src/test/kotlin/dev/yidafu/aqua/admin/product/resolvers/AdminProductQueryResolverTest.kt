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

package dev.yidafu.aqua.admin.product.resolvers

import dev.yidafu.aqua.common.graphql.generated.*
import dev.yidafu.aqua.product.domain.model.ProductModel
import dev.yidafu.aqua.product.service.ProductService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.test.context.support.WithMockUser
import java.util.*

class AdminProductQueryResolverTest {

    @Mock
    private lateinit var productService: ProductService

    @InjectMocks
    private lateinit var adminProductQueryResolver: AdminProductQueryResolver

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `productStatistics should return product statistics`() {
        // Given
        val expectedStatistics = ProductStatistics(
            totalProducts = 10,
            onlineProducts = 7,
            offlineProducts = 3,
            lowStockProducts = 2,
            totalValue = 50000L,
            averagePrice = 5000L
        )

        whenever(productService.getProductStatistics()).thenReturn(expectedStatistics)

        // When
        val result = adminProductQueryResolver.productStatistics()

        // Then
        assert(result.totalProducts == 10)
        assert(result.onlineProducts == 7)
        assert(result.offlineProducts == 3)
        assert(result.lowStockProducts == 2)
        assert(result.totalValue == 50000L)
        assert(result.averagePrice == 5000L)
        verify(productService).getProductStatistics()
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `lowStockProducts should return low stock alerts`() {
        // Given
        val lowStockProduct = createTestProduct(1L, "Low Stock Product", 1000L, 5)
        val threshold = 10

        whenever(productService.getLowStockProducts(threshold)).thenReturn(listOf(lowStockProduct))

        // When
        val result = adminProductQueryResolver.lowStockProducts(threshold)

        // Then
        assert(result.size == 1)
        val alert = result[0]
        assert(alert.productId == 1L)
        assert(alert.productName == "Low Stock Product")
        assert(alert.currentStock == 5)
        assert(alert.threshold == threshold)
        verify(productService).getLowStockProducts(threshold)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `lowStockProducts should use default threshold`() {
        // Given
        whenever(productService.getLowStockProducts(10)).thenReturn(emptyList())

        // When
        val result = adminProductQueryResolver.lowStockProducts()

        // Then
        assert(result.isEmpty())
        verify(productService).getLowStockProducts(10) // Default threshold
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `productsPaginated should return paginated products`() {
        // Given
        val productListInput = ProductListInput(
            page = 0,
            size = 20,
            sort = "createdAt,desc",
            search = "test",
            status = ProductStatus.ONLINE,
            minPrice = 1000L,
            maxPrice = 5000L,
            minStock = 1,
            maxStock = 100
        )

        val products = listOf(
            createTestProduct(1L, "Test Product 1", 2000L, 50),
            createTestProduct(2L, "Test Product 2", 3000L, 30)
        )

        val page = PageImpl(products, mock<Pageable>(), 2)

        whenever(
            productService.findByNameContainingAndStatus(
                eq("test"),
                eq(ProductStatus.ONLINE),
                any()
            )
        ).thenReturn(page)

        // When
        val result = adminProductQueryResolver.productsPaginated(productListInput)

        // Then
        assert(result.content.size == 2)
        assert(result.totalElements == 2L)
        assert(result.totalPages == 1)
        assert(result.size == 20)
        assert(result.number == 0)
        assert(result.first == true)
        assert(result.last == true)
        assert(result.empty == false)

        verify(productService).findByNameContainingAndStatus(
            eq("test"),
            eq(ProductStatus.ONLINE),
            any()
        )
    }

    @Test
    @WithMockUser(roles = ["USER"]) // Not ADMIN
    fun `productStatistics should throw AccessDeniedException for non-admin users`() {
        // Given
        whenever(productService.getProductStatistics()).thenReturn(
            ProductStatistics(0, 0, 0, 0, 0L, 0L)
        )

        // When & Then
        // Note: In a real test environment, you would use @WithMockUser with ADMIN role
        // This test demonstrates the security check
        assertThrows<AccessDeniedException> {
            // This would typically be caught by Spring Security before method execution
            adminProductQueryResolver.productStatistics()
        }
    }

    private fun createTestProduct(
        id: Long,
        name: String,
        priceCents: Long,
        stock: Int,
        status: ProductStatus = ProductStatus.OFFLINE
    ): ProductModel {
        return ProductModel(
            id = id,
            name = name,
            price = priceCents,
            coverImageUrl = "http://example.com/image.jpg",
            detailImages = null,
            description = "Test description",
            stock = stock,
            status = status
        )
    }
}