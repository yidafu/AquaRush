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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package dev.yidafu.aqua.admin.product.resolvers

import dev.yidafu.aqua.common.annotation.AdminService
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.enums.ProductModelStatus
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.graphql.generated.CreateProductInput
import dev.yidafu.aqua.common.graphql.generated.ProductStatus
import dev.yidafu.aqua.common.graphql.generated.UpdateProductInput
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.product.service.impl.ProductServiceImpl
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import org.springframework.transaction.annotation.Transactional

/**
 * 管理端产品变更解析器
 * 提供产品管理的完整变更功能，仅管理员可访问
 */
@AdminService
@Controller
class AdminProductMutationResolver(
  private val productService: ProductServiceImpl,
  private val bizLogger: BizLogger,
) {
  private val logger = LoggerFactory.getLogger(AdminProductMutationResolver::class.java)

  /**
   * 创建新产品（管理员功能）
   */
  @PreAuthorize("hasAuthority('PERMISSION_PRODUCT_WRITE')")
  @Transactional
  @MutationMapping
  fun createProduct(
    @Argument @Valid input: CreateProductInput,
  ): ProductModel {
    try {
      // 验证输入
      validateCreateProductInput(input)

      val product = productService.createProduct(input)

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
  @PreAuthorize("hasAuthority('PERMISSION_PRODUCT_WRITE')")
  @Transactional
  @MutationMapping
  fun updateProduct(
    @Argument id: Long,
    @Argument @Valid input: UpdateProductInput,
  ): ProductModel {
    try {
      // 验证产品存在
      val existingProduct =
        productService.findById(id)
          ?: throw BadRequestException("产品不存在: $id")

      // 验证输入
      validateUpdateProductInput(input)

      val updatedProduct =
        productService.updateProduct(
          productId = id,
          name = input.name,
          priceCents = input.priceCents,
          coverImageUrl = input.coverImageUrl,
          // detailImages字段在ProductUpdateRequestInput中不存在，使用imageGallery替代
          // detailImages = input.detailImages?.joinToString(","),
          description = input.detailContent,
          stock = input.stock,
          subtitle = input.subtitle,
          originalPriceCents = input.originalPriceCents,
          imageGallery = input.imageGallery,
          specification = input.specification,
          waterSource = input.waterSource,
          mineralContent = input.mineralContent,
          salesVolume = input.salesVolume,
          sortOrder = input.sortOrder,
          tags = input.tags,
          detailContent = input.detailContent,
          certificateImages = input.certificateImages,
          status = input.status,
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
  @PreAuthorize("hasAuthority('PERMISSION_PRODUCT_WRITE')")
  @Transactional
  fun deleteProduct(id: Long): Boolean =
    try {
      // 验证产品存在
      val existingProduct =
        productService.findById(id)
          ?: throw BadRequestException("产品不存在: $id")

      // 检查产品是否有关联的订单 - 简化版本，暂时移除此检查
      // TODO: 实现订单关联检查

      productService.productRepository.deleteById(id)
      logger.info("Successfully deleted product: $id")

      // 业务日志(F-LOG-003 PRODUCT 域):产品删除
      val (operatorId, operatorName) = bizLogger.resolveOperator()
      bizLogger.logProductOperation(
        productId = id,
        operation = "PRODUCT_DELETED",
        result = "SUCCESS",
        userId = operatorId,
        username = operatorName,
        additionalData = mapOf("name" to existingProduct.name),
      )
      true
    } catch (e: Exception) {
      logger.error("Failed to delete product", e)
      throw BadRequestException("删除产品失败: ${e.message}")
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
    if (input.priceCents <= 0) {
      throw BadRequestException("产品价格必须大于0")
    }
    if (input.stock < 0) {
      throw BadRequestException("产品库存不能为负数")
    }
    if (input.detailContent?.length ?: 0 > 2000) {
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

    input.priceCents?.let { price ->
      if (price <= 0) {
        throw BadRequestException("产品价格必须大于0")
      }
    }

    input.stock?.let { stock ->
      if (stock < 0) {
        throw BadRequestException("产品库存不能为负数")
      }
    }

    input.detailContent?.let { detailContent ->
      if (detailContent.length > 2000) {
        throw BadRequestException("产品描述长度不能超过2000个字符")
      }
    }
  }

  /**
   * 上线产品（管理员功能）
   */
  @PreAuthorize("hasAuthority('PERMISSION_PRODUCT_WRITE')")
  @Transactional
  fun onlineProduct(id: Long): ProductModel =
    try {
      val product =
        productService.findById(id)
          ?: throw BadRequestException("产品不存在: $id")

      if (product.status == ProductModelStatus.ONLINE) {
        throw BadRequestException("产品已上线")
      }

      productService.updateProductStatus(id, ProductStatus.ONLINE)
      logger.info("Successfully online product: $id")
      productService.findById(id)!!
    } catch (e: Exception) {
      logger.error("Failed to online product", e)
      throw BadRequestException("上线产品失败: ${e.message}")
    }

  /**
   * 下线产品（管理员功能）
   */
  @PreAuthorize("hasAuthority('PERMISSION_PRODUCT_WRITE')")
  @Transactional
  fun offlineProduct(id: Long): ProductModel =
    try {
      val product =
        productService.findById(id)
          ?: throw BadRequestException("产品不存在: $id")

      if (product.status == ProductModelStatus.OFFLINE) {
        throw BadRequestException("产品已下线")
      }

      productService.updateProductStatus(id, ProductStatus.OFFLINE)
      logger.info("Successfully offline product: $id")
      productService.findById(id)!!
    } catch (e: Exception) {
      logger.error("Failed to offline product", e)
      throw BadRequestException("下线产品失败: ${e.message}")
    }

  /**
   * 更新产品状态（管理员功能）
   */
  @PreAuthorize("hasAuthority('PERMISSION_PRODUCT_WRITE')")
  @Transactional
  fun updateProductStatus(
    productId: Long,
    status: ProductStatus,
  ): ProductModel =
    try {
      val product =
        productService.findById(productId)
          ?: throw BadRequestException("产品不存在: $productId")

      productService.updateProductStatus(productId, status)
      logger.info("Successfully updated product status: $productId to $status")
      productService.findById(productId)!!
    } catch (e: Exception) {
      logger.error("Failed to update product status", e)
      throw BadRequestException("更新产品状态失败: ${e.message}")
    }
}
