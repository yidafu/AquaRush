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

package dev.yidafu.aqua.client.product.resolvers

import dev.yidafu.aqua.client.product.resolvers.ClientProductQueryResolver.Companion.ProductSearchInput
import dev.yidafu.aqua.common.annotation.ClientService
import dev.yidafu.aqua.common.graphql.generated.ProductStatus
import dev.yidafu.aqua.product.domain.model.ProductModel
import dev.yidafu.aqua.product.service.ProductService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

/**
 * 客户端产品查询解析器
 * 提供产品浏览查询功能，仅限访问上线产品
 */
@ClientService
@Controller
class ClientProductQueryResolver(
    private val productService: ProductService
) {

    /**
     * 根据ID查询产品详情（仅限上线产品）
     */
    @PreAuthorize("isAuthenticated()")
    fun product(id: Long): ProductModel? {
        val product = productService.findById(id)
        return product?.takeIf { it.status == ProductStatus.Online }
    }

    /**
     * 查询活跃的上线产品（分页）
     */
    @PreAuthorize("isAuthenticated()")
    fun activeProducts(
        page: Int = 0,
        size: Int = 20,
        sortBy: String = "createdAt",
        sortDirection: String = "desc"
    ): Page<ProductModel> {
        val pageable: Pageable = PageRequest.of(
            page,
            size,
            Sort.by(if (sortDirection.lowercase() == "asc") Direction.ASC else Direction.DESC, sortBy)
        )
        return productService.findOnlineProducts(pageable)
    }

    /**
     * 按关键词搜索产品
     */
    @PreAuthorize("isAuthenticated()")
    fun searchProducts(input: ProductSearchInput): Page<ProductModel> {
        val pageable: Pageable = PageRequest.of(
            input.page ?: 0,
            input.size ?: 20,
            Sort.by(
                if ((input.sortDirection ?: "desc").lowercase() == "asc") Direction.ASC
                else Direction.DESC,
                input.sortBy ?: "createdAt"
            )
        )

        return when {
            input.category != null && input.keyword != null -> {
                productService.findByCategoryAndNameContainingAndStatus(
                    input.category,
                    input.keyword,
                    pageable
                )
            }
            input.category != null -> {
                productService.findByCategoryAndStatus(input.category, pageable)
            }
            input.keyword != null -> {
                productService.findByNameContainingAndStatus(input.keyword, pageable)
            }
            else -> {
                productService.findByStatus(ProductStatus.Online, pageable)
            }
        }
    }

    /**
     * 按分类查询产品
     */
    @PreAuthorize("isAuthenticated()")
    fun productsByCategory(
        category: String,
        page: Int = 0,
        size: Int = 20,
        sortBy: String = "name",
        sortDirection: String = "asc"
    ): Page<ProductModel> {
        val pageable: Pageable = PageRequest.of(
            page,
            size,
            Sort.by(
                if (sortDirection.lowercase() == "asc") Direction.ASC
                else Direction.DESC,
                sortBy
            )
        )
        return productService.findByCategoryAndStatus(category, pageable)
    }

    /**
     * 按价格范围查询产品
     */
    @PreAuthorize("isAuthenticated()")
    fun productsByPriceRange(
        minPrice: java.math.BigDecimal,
        maxPrice: java.math.BigDecimal,
        page: Int = 0,
        size: Int = 20
    ): Page<ProductModel> {
        val pageable: Pageable = PageRequest.of(page, size)
        return productService.findByPriceBetweenAndStatus(minPrice, maxPrice, pageable)
    }

    /**
     * 查询热销产品
     */
    @PreAuthorize("isAuthenticated()")
    fun popularProducts(
        limit: Int = 10,
        page: Int = 0,
        size: Int = 20
    ): Page<ProductModel> {
        val pageable: Pageable = PageRequest.of(page, size)
        return productService.findPopularProducts(pageable, limit)
    }

    /**
     * 查询新产品
     */
    @PreAuthorize("isAuthenticated()")
    fun newProducts(
        days: Int = 7,
        page: Int = 0,
        size: Int = 20
    ): Page<ProductModel> {
        val pageable: Pageable = PageRequest.of(page, size)
        return productService.findNewProducts(pageable, days)
    }

    /**
     * 查询推荐产品
     */
    @PreAuthorize("isAuthenticated()")
    fun recommendedProducts(
        limit: Int = 10,
        page: Int = 0,
        size: Int = 20
    ): Page<ProductModel> {
        val pageable: Pageable = PageRequest.of(page, size)
        return productService.findRecommendedProducts(pageable, limit)
    }

    /**
     * 获取产品分类列表
     */
    @PreAuthorize("isAuthenticated()")
    fun productCategories(): List<String> {
        return productService.findAllCategories()
    }

    /**
     * 获取价格区间统计
     */
    @PreAuthorize("isAuthenticated()")
    fun priceRanges(): List<PriceRange> {
        return productService.getPriceRangeStatistics()
    }

    /**
     * 检查产品库存
     */
    @PreAuthorize("isAuthenticated()")
    fun checkProductStock(productId: Long): StockInfo {
        val product = productService.findById(productId)
            ?: throw IllegalArgumentException("产品不存在: $productId")

        return StockInfo(
            productId = productId,
            productName = product.name,
            currentStock = product.stock,
            isAvailable = product.stock > 0 && product.status == ProductStatus.Online,
            lowStockWarning = product.stock <= 5
        )
    }

    companion object {
        /**
         * 产品搜索输入类型
         */
        data class ProductSearchInput(
            val keyword: String?,
            val category: String?,
            val minPrice: java.math.BigDecimal?,
            val maxPrice: java.math.BigDecimal?,
            val page: Int?,
            val size: Int?,
            val sortBy: String?,
            val sortDirection: String?
        )

        /**
         * 价格区间
         */
        data class PriceRange(
            val min: java.math.BigDecimal,
            val max: java.math.BigDecimal,
            val count: Long,
            val label: String
        )

        /**
         * 库存信息
         */
        data class StockInfo(
            val productId: Long,
            val productName: String,
            val currentStock: Int,
            val isAvailable: Boolean,
            val lowStockWarning: Boolean
        )
    }
}
