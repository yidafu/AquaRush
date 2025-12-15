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

import dev.yidafu.aqua.common.annotation.AdminService
import org.springframework.data.domain.PageImpl
import dev.yidafu.aqua.common.graphql.generated.ProductStatus
import dev.yidafu.aqua.common.graphql.generated.ProductStatistics
import dev.yidafu.aqua.common.graphql.generated.LowStockAlert
import dev.yidafu.aqua.common.graphql.generated.ProductPage
import dev.yidafu.aqua.common.graphql.generated.ProductListInput
import dev.yidafu.aqua.common.graphql.util.toPageInfo
import dev.yidafu.aqua.product.domain.model.ProductModel
import dev.yidafu.aqua.product.mapper.ProductMapper
import dev.yidafu.aqua.product.service.ProductService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
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
    @QueryMapping
    fun products(
      @Argument page: Int? = 0,
      @Argument size: Int? = 20,
      @Argument status: ProductStatus? = null,
      @Argument keyword: String? = null
    ): ProductPage {
        val actualPage = page ?: 0
        val actualSize = size ?: 20
        val pageable: Pageable = PageRequest.of(actualPage, actualSize)

        val productsPage = when {
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

        val (productList, pageInfo) = productsPage.toPageInfo { ProductMapper.map(it) }
        return ProductPage(
            list = productList,
            pageInfo = pageInfo
        )
    }

    /**
     * 根据ID查询产品详细信息（管理员功能）
     */

    @PreAuthorize("hasRole('ADMIN')")
    @QueryMapping
    fun product(@Argument id: Long): ProductModel? {
        return productService.findById(id)
    }

    /**
     * 查询活跃产品（管理员功能）
     */
    @PreAuthorize("hasRole('ADMIN')")
    @QueryMapping
    fun activeProducts(
      @Argument page: Int? = 0,
     @Argument size: Int? = 20
    ): Page<ProductModel> {
        val actualPage = page ?: 0
        val actualSize = size ?: 20
        val pageable: Pageable = PageRequest.of(actualPage, actualSize)
        return productService.findByStatus(ProductStatus.ONLINE, pageable)
    }

    /**
     * 查询下线产品（管理员功能）
     */
    @PreAuthorize("hasRole('ADMIN')")
    fun offlineProducts(
        page: Int? = 0,
        size: Int? = 20
    ): Page<ProductModel> {
        val actualPage = page ?: 0
        val actualSize = size ?: 20
        val pageable: Pageable = PageRequest.of(actualPage, actualSize)
        return productService.findByStatus(ProductStatus.OFFLINE, pageable)
    }

    /**
     * 查询低库存产品（管理员功能）
     */
    @PreAuthorize("hasRole('ADMIN')")
    fun lowStockProducts(
        threshold: Int? = 10,
        page: Int? = 0,
        size: Int? = 20
    ): Page<ProductModel> {
        val actualThreshold = threshold ?: 10
        val actualPage = page ?: 0
        val actualSize = size ?: 20
        val pageable: Pageable = PageRequest.of(actualPage, actualSize)
        return productService.findLowStockProducts(actualThreshold, pageable)
    }

    /**
     * 按分类查询产品（管理员功能）
     */
    @PreAuthorize("hasRole('ADMIN')")
    @QueryMapping
    fun productsByCategory(
      @Argument category: String,
      @Argument page: Int? = 0,
      @Argument size: Int? = 20
    ): Page<ProductModel> {
        val actualPage = page ?: 0
        val actualSize = size ?: 20
        val pageable: Pageable = PageRequest.of(actualPage, actualSize)
        return productService.findByCategory(category, pageable)
    }

    /**
     * 按价格范围查询产品（管理员功能）
     */
    @PreAuthorize("hasRole('ADMIN')")
    fun productsByPriceRange(
        minPrice: java.math.BigDecimal,
        maxPrice: java.math.BigDecimal,
        page: Int? = 0,
        size: Int? = 20
    ): Page<ProductModel> {
        val actualPage = page ?: 0
        val actualSize = size ?: 20
        val pageable: Pageable = PageRequest.of(actualPage, actualSize)
        return productService.findByPriceBetween(minPrice, maxPrice, pageable)
    }

    /**
     * 获取产品统计信息（管理员功能）- GraphQL
     */
    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun productStatistics(): ProductStatistics {
        return productService.getProductStatistics()
    }

    /**
     * 获取低库存产品警报（管理员功能）- GraphQL
     */
    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun lowStockProducts(@Argument threshold: Int? = 10): List<LowStockAlert> {
        val actualThreshold = threshold ?: 10
        val lowStockProducts = productService.getLowStockProducts(actualThreshold)
        return lowStockProducts.map { product ->
            LowStockAlert(
                productId = product.id,
                productName = product.name,
                currentStock = product.stock,
                threshold = actualThreshold,
                status = product.status
            )
        }
    }

    /**
     * 分页查询产品（管理员功能）- GraphQL
     */
    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun productsPaginated(@Argument input: ProductListInput): ProductPage {
        val pageable: Pageable = PageRequest.of(input.page ?: 0, input.size ?: 20)
        val productsPage = when {
            input.search != null && input.status != null -> {
                productService.findByNameContainingAndStatus(input.search!!, input.status!!, pageable)
            }
            input.search != null -> {
                productService.findByNameContaining(input.search!!, pageable)
            }
            input.status != null -> {
                productService.findByStatus(input.status!!, pageable)
            }
            else -> {
                productService.findAll(pageable)
            }
        }

        val (products, pageInfo) = productsPage.toPageInfo { ProductMapper.map(it) }
        return ProductPage(
            list = products,
            pageInfo = pageInfo
        )
    }

    // Enhanced product management queries

    /**
     * 获取热销产品（管理员功能）
     */
    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun topSalesProducts(@Argument limit: Int? = 10): List<ProductModel> {
        return productService.getTopSalesProducts(limit ?: 10)
    }

    /**
     * 按水源地查询产品（管理员功能）
     */
    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun productsByWaterSource(@Argument waterSource: String): List<ProductModel> {
        return productService.findByWaterSource(waterSource)
    }

    /**
     * 按PH值范围查询产品（管理员功能）
     */
    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun productsByPhRange(
        @Argument minPh: java.math.BigDecimal,
        @Argument maxPh: java.math.BigDecimal
    ): List<ProductModel> {
        return productService.findByPhRange(minPh, maxPh)
    }

    /**
     * 按销量查询产品（管理员功能）
     */
    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun productsByMinSalesVolume(@Argument minVolume: Int?): List<ProductModel> {
        return productService.findBySalesVolumeGreaterThan(minVolume ?: 0)
    }

    /**
     * 按标签查询产品（管理员功能）
     */
    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun productsByTag(@Argument tag: String): List<ProductModel> {
        return productService.findByTagsContaining(tag)
    }

    /**
     * 获取水源地统计（管理员功能）
     */
    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun waterSourceStatistics(): Map<String, Long> {
        return productService.getWaterSourceStatistics()
    }

    /**
     * 获取产品规格统计（管理员功能）
     */
    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun specificationStatistics(): Map<String, Long> {
        return productService.getSpecificationStatistics()
    }

    /**
     * 获取所有活跃产品（未删除的产品）
     */
    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun allActiveProducts(): List<ProductModel> {
        return productService.findActiveProducts()
    }

    /**
     * 按状态获取活跃产品
     */
    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun activeProductsByStatus(@Argument status: ProductStatus): List<ProductModel> {
        return productService.findActiveProductsByStatus(status)
    }

    /**
     * 按销量排序产品
     */
    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun productsSortedBySalesVolume(): List<ProductModel> {
        return productService.findAllByOrderBySalesVolumeDesc()
    }

    /**
     * 按排序权重排序产品
     */
    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun productsSortedBySortOrder(): List<ProductModel> {
        return productService.findAllByOrderBySortOrderAsc()
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
