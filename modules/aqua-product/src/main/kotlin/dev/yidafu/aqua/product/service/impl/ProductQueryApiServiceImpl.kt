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

import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.enums.ProductModelStatus
import dev.yidafu.aqua.common.graphql.generated.ProductStatus
import dev.yidafu.aqua.product.domain.repository.ProductRepository
import org.springframework.stereotype.Service

/**
 * 跨模块产品查询服务实现
 * 供其他模块使用
 */
@Service
class ProductQueryApiServiceImpl(
  private val productRepository: ProductRepository,
) : ProductQueryApiService {
  override fun findById(id: Long): ProductModel? = productRepository.findById(id).orElse(null)

  override fun findByIds(ids: List<Long>): List<ProductModel> = productRepository.findAllById(ids)

  override fun findOnlineProducts(): List<ProductModel> = productRepository.findByStatus(ProductStatus.ONLINE)

  override fun findByStatus(status: ProductModelStatus): List<ProductModel> = productRepository.findByStatus(status.toGraphql())

  override fun existsById(id: Long): Boolean = productRepository.existsById(id)
}

// Extension function to convert ProductModelStatus to ProductStatus
fun ProductModelStatus.toGraphql(): ProductStatus =
  when (this) {
    ProductModelStatus.ONLINE -> ProductStatus.ONLINE
    ProductModelStatus.OFFLINE -> ProductStatus.OFFLINE
    ProductModelStatus.OUT_OF_STOCK -> ProductStatus.OUT_OF_STOCK
  }
