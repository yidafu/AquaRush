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
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.product.domain.model.Product
import dev.yidafu.aqua.common.graphql.generated.ProductStatus
import dev.yidafu.aqua.product.service.ProductService
import org.slf4j.LoggerFactory
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import org.springframework.transaction.annotation.Transactional
import jakarta.validation.Valid
import java.math.BigDecimal

/**
 * 管理端产品变更解析器
 * 提供产品管理的完整变更功能，仅管理员可访问
 */
@AdminService
@Controller
class AdminProductMutationResolver(
    private val productService: ProductService
) {
    private val logger = LoggerFactory.getLogger(AdminProductMutationResolver::class.java)

    /**
     * 创建新产品（管理员功能）
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    fun createProduct(@Valid input: CreateProductInput): Product {
        try {
            // 验证输入
            validateCreateProductInput(input)

            val product = productService.createProduct(
                name = input.name,
                price = input.price,
                coverImageUrl = input.coverImageUrl ?: "",
                detailImages = input.detailImages?.joinToString(","),
                description = input.description,
                stock = input.stock
            )

            logger.info("Successfully created product: ${product.id} - ${product.name}")
            return product
        } catch (e: Exception) {
            logger.error("Failed to create product", e)
            throw BadRequestException("创建产品失败: ${e.message}")
        }
    }

    /**
     * 更新产品信息（管理员功能）
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    fun updateProduct(id: Long, @Valid input: UpdateProductInput): Product {
        try {
            // 验证产品存在
            val existingProduct = productService.findById(id)
                ?: throw BadRequestException("产品不存在: $id")

            // 验证输入
            validateUpdateProductInput(input)

            val updatedProduct = productService.updateProduct(
                productId = id,
                name = input.name,
                price = input.price,
                coverImageUrl = input.coverImageUrl,
                detailImages = input.detailImages?.joinToString(","),
                description = input.description,
                stock = input.stock
            )

            logger.info("Successfully updated product: $id - ${updatedProduct.name}")
            return updatedProduct
        } catch (e: Exception) {
            logger.error("Failed to update product", e)
            throw BadRequestException("更新产品失败: ${e.message}")
        }
    }

    /**
     * 删除产品（管理员功能）
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    fun deleteProduct(id: Long): Boolean {
        return try {
            // 验证产品存在
            val existingProduct = productService.findById(id)
                ?: throw BadRequestException("产品不存在: $id")

            // 检查产品是否有关联的订单 - 简化版本，暂时移除此检查
            // TODO: 实现订单关联检查

            productService.productRepository.deleteById(id)
            logger.info("Successfully deleted product: $id")
            true
        } catch (e: Exception) {
            logger.error("Failed to delete product", e)
            throw BadRequestException("删除产品失败: ${e.message}")
        }
    }

    /**
     * 批量调整产品库存（管理员功能）
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    fun batchAdjustStock(input: BatchStockAdjustmentInput): BatchStockAdjustmentResult {
        try {
            val results = mutableListOf<StockAdjustmentResult>()

            input.adjustments.forEach { adjustment ->
                try {
                    val success = when (adjustment.type) {
                        StockAdjustmentType.INCREASE -> {
                            productService.increaseStock(adjustment.productId, adjustment.quantity)
                            true
                        }
                        StockAdjustmentType.DECREASE -> {
                            productService.decreaseStock(adjustment.productId, adjustment.quantity)
                            true
                        }
                        StockAdjustmentType.SET -> {
                            val product = productService.findById(adjustment.productId)
                            if (product != null) {
                                val targetStock = adjustment.quantity
                                val currentStock = product.stock
                                if (targetStock > currentStock) {
                                    productService.increaseStock(adjustment.productId, targetStock - currentStock)
                                } else {
                                    productService.decreaseStock(adjustment.productId, currentStock - targetStock)
                                }
                                true
                            } else {
                                false
                            }
                        }
                    }

                    results.add(StockAdjustmentResult(
                        productId = adjustment.productId,
                        success = success,
                        message = if (success) "调整成功" else "调整失败"
                    ))
                } catch (e: Exception) {
                    logger.warn("Failed to adjust stock for product ${adjustment.productId}", e)
                    results.add(StockAdjustmentResult(
                        productId = adjustment.productId,
                        success = false,
                        message = "调整失败: ${e.message}"
                    ))
                }
            }

            val successCount = results.count { it.success }
            val totalCount = results.size

            return BatchStockAdjustmentResult(
                success = successCount == totalCount,
                successCount = successCount,
                failureCount = totalCount - successCount,
                results = results
            )
        } catch (e: Exception) {
            logger.error("Failed to batch adjust stock", e)
            throw BadRequestException("批量调整库存失败: ${e.message}")
        }
    }

    /**
     * 上线产品（管理员功能）
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    fun onlineProduct(id: Long): Product {
        return try {
            val product = productService.findById(id)
                ?: throw BadRequestException("产品不存在: $id")

            if (product.status == ProductStatus.Online) {
                throw BadRequestException("产品已上线")
            }

            productService.updateProductStatus(id, ProductStatus.Online)
            logger.info("Successfully online product: $id")
            productService.findById(id)!!
        } catch (e: Exception) {
            logger.error("Failed to online product", e)
            throw BadRequestException("上线产品失败: ${e.message}")
        }
    }

    /**
     * 下线产品（管理员功能）
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    fun offlineProduct(id: Long): Product {
        return try {
            val product = productService.findById(id)
                ?: throw BadRequestException("产品不存在: $id")

            if (product.status == ProductStatus.Offline) {
                throw BadRequestException("产品已下线")
            }

            productService.updateProductStatus(id, ProductStatus.Offline)
            logger.info("Successfully offline product: $id")
            productService.findById(id)!!
        } catch (e: Exception) {
            logger.error("Failed to offline product", e)
            throw BadRequestException("下线产品失败: ${e.message}")
        }
    }

    /**
     * 验证创建产品输入
     */
    private fun validateCreateProductInput(input: CreateProductInput) {
        if (input.name.isBlank()) {
            throw BadRequestException("产品名称不能为空")
        }
        if (input.name.length > 100) {
            throw BadRequestException("产品名称长度不能超过100个字符")
        }
        if (input.price <= BigDecimal.ZERO) {
            throw BadRequestException("产品价格必须大于0")
        }
        if (input.stock < 0) {
            throw BadRequestException("产品库存不能为负数")
        }
        if (input.description?.length ?: 0 > 2000) {
            throw BadRequestException("产品描述长度不能超过2000个字符")
        }
    }

    /**
     * 验证更新产品输入
     */
    private fun validateUpdateProductInput(input: UpdateProductInput) {
        input.name?.let { name ->
            if (name.isBlank()) {
                throw BadRequestException("产品名称不能为空")
            }
            if (name.length > 100) {
                throw BadRequestException("产品名称长度不能超过100个字符")
            }
        }

        input.price?.let { price ->
            if (price <= BigDecimal.ZERO) {
                throw BadRequestException("产品价格必须大于0")
            }
        }

        input.stock?.let { stock ->
            if (stock < 0) {
                throw BadRequestException("产品库存不能为负数")
            }
        }

        input.description?.let { description ->
            if (description.length > 2000) {
                throw BadRequestException("产品描述长度不能超过2000个字符")
            }
        }
    }

    companion object {
        /**
         * 产品操作输入类型
         */
        data class CreateProductInput(
            val name: String,
            val price: BigDecimal,
            val coverImageUrl: String?,
            val detailImages: List<String>?,
            val description: String?,
            val stock: Int,
            val category: String?
        )

        data class UpdateProductInput(
            val name: String?,
            val price: BigDecimal?,
            val coverImageUrl: String?,
            val detailImages: List<String>?,
            val description: String?,
            val stock: Int?,
            val category: String?
        )

        data class StockAdjustmentInput(
            val productId: Long,
            val quantity: Int,
            val type: StockAdjustmentType
        )

        data class BatchStockAdjustmentInput(
            val adjustments: List<StockAdjustmentInput>
        )

        data class StockAdjustmentResult(
            val productId: Long,
            val success: Boolean,
            val message: String
        )

        data class BatchStockAdjustmentResult(
            val success: Boolean,
            val successCount: Int,
            val failureCount: Int,
            val results: List<StockAdjustmentResult>
        )

        enum class StockAdjustmentType {
            INCREASE, DECREASE, SET
        }
    }
}