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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package dev.yidafu.aqua.admin.product.resolvers

import dev.yidafu.aqua.admin.product.resolvers.AdminProductMutationResolver.Companion.CreateProductInput
import dev.yidafu.aqua.admin.product.resolvers.AdminProductMutationResolver.Companion.UpdateProductInput
import dev.yidafu.aqua.admin.product.resolvers.AdminProductMutationResolver.Companion.StockAdjustmentInput
import dev.yidafu.aqua.common.annotation.AdminService
import org.springframework.data.domain.PageImpl
import dev.yidafu.aqua.product.domain.model.Product
import dev.yidafu.aqua.common.graphql.generated.ProductStatus
import dev.yidafu.aqua.product.service.ProductService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

/**
 * 管理端产品查询解析器
 * 提供产品管理的完整查询功能，仅管理员可访问
 */
@AdminService
@Controller
class AdminProductQueryResolver(
    private val productService: ProductService
) {

    /**
     * 查询所有产品（管理员功能，包括下线产品）
     */
    @PreAuthorize("hasRole('ADMIN')")
    fun products(
        page: Int = 0,
        size: Int = 20,
        status: ProductStatus? = null,
        keyword: String? = null
    ): Page<Product> {
        val pageable: Pageable = PageRequest.of(page, size)

        return when {
            keyword != null && status != null -> {
                productService.findByNameContainingAndStatus(keyword, status, pageable)
            }
            keyword != null -> {
                productService.findByNameContaining(keyword, pageable)
            }
            status != null -> {
                productService.findByStatus(status, pageable)
            }
            else -> {
                productService.findAll(pageable)
            }
        }
    }

    /**
     * 根据ID查询产品详细信息（管理员功能）
     */
    @PreAuthorize("hasRole('ADMIN')")
    fun product(id: Long): Product? {
        return productService.findById(id)
    }

    /**
     * 查询活跃产品（管理员功能）
     */
    @PreAuthorize("hasRole('ADMIN')")
    fun activeProducts(
        page: Int = 0,
        size: Int = 20
    ): Page<Product> {
        val pageable: Pageable = PageRequest.of(page, size)
        return productService.findByStatus(ProductStatus.Online, pageable)
    }

    /**
     * 查询下线产品（管理员功能）
     */
    @PreAuthorize("hasRole('ADMIN')")
    fun offlineProducts(
        page: Int = 0,
        size: Int = 20
    ): Page<Product> {
        val pageable: Pageable = PageRequest.of(page, size)
        return productService.findByStatus(ProductStatus.Offline, pageable)
    }

    /**
     * 查询低库存产品（管理员功能）
     */
    @PreAuthorize("hasRole('ADMIN')")
    fun lowStockProducts(
        threshold: Int = 10,
        page: Int = 0,
        size: Int = 20
    ): Page<Product> {
        val pageable: Pageable = PageRequest.of(page, size)
        return productService.findLowStockProducts(threshold, pageable)
    }

    /**
     * 按分类查询产品（管理员功能）
     */
    @PreAuthorize("hasRole('ADMIN')")
    fun productsByCategory(
        category: String,
        page: Int = 0,
        size: Int = 20
    ): Page<Product> {
        val pageable: Pageable = PageRequest.of(page, size)
        return productService.findByCategory(category, pageable)
    }

    /**
     * 按价格范围查询产品（管理员功能）
     */
    @PreAuthorize("hasRole('ADMIN')")
    fun productsByPriceRange(
        minPrice: java.math.BigDecimal,
        maxPrice: java.math.BigDecimal,
        page: Int = 0,
        size: Int = 20
    ): Page<Product> {
        val pageable: Pageable = PageRequest.of(page, size)
        return productService.findByPriceBetween(minPrice, maxPrice, pageable)
    }

    /**
     * 获取产品统计信息（管理员功能）
     */
    @PreAuthorize("hasRole('ADMIN')")
    fun productStatistics(): ProductStatistics {
        val allProducts = productService.count()
        val onlineProducts = productService.countByStatus(ProductStatus.Online)
        val offlineProducts = productService.countByStatus(ProductStatus.Offline)
        val lowStockProducts = productService.countLowStockProducts(10)

        return ProductStatistics(
            totalProducts = allProducts,
            onlineProducts = onlineProducts,
            offlineProducts = offlineProducts,
            lowStockProducts = lowStockProducts,
            onlinePercentage = if (allProducts > 0) (onlineProducts.toDouble() / allProducts * 100) else 0.0
        )
    }
}

/**
 * 产品统计信息
 */
data class ProductStatistics(
    val totalProducts: Long,
    val onlineProducts: Long,
    val offlineProducts: Long,
    val lowStockProducts: Long,
    val onlinePercentage: Double
)