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

import dev.yidafu.aqua.api.dto.ProductOptionCreateRequest
import dev.yidafu.aqua.api.dto.ProductOptionUpdateRequest
import dev.yidafu.aqua.api.service.product.ProductOptionMutationService
import dev.yidafu.aqua.common.domain.model.ProductOptionModel
import dev.yidafu.aqua.common.domain.model.enums.ProductOptionType
import dev.yidafu.aqua.product.domain.exception.DuplicateProductOptionValueException
import dev.yidafu.aqua.product.domain.exception.ProductOptionNotFoundException
import dev.yidafu.aqua.product.domain.repository.ProductOptionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 商品属性选项字典写操作服务实现(F-PRO-009,管理后台)
 * 规则:
 * - 同类型下 value 未删除行内唯一(重复抛出 DuplicateProductOptionValueException);
 * - sortOrder 为空时排到同类型末尾(当前最大 + 1);
 * - 删除为软删除,仅移除候选,不影响已引用该值的商品(商品快照值保留)。
 */
@Service
class ProductOptionMutationServiceImpl(
  private val productOptionRepository: ProductOptionRepository,
) : ProductOptionMutationService {
  @Transactional
  override fun createOption(
    adminId: Long,
    adminName: String,
    request: ProductOptionCreateRequest,
  ): ProductOptionModel {
    validateValueUnique(request.optionType, request.value, excludeId = null)
    val sortOrder = request.sortOrder ?: (productOptionRepository.findMaxSortOrder(request.optionType) ?: 0).plus(1)
    val option =
      ProductOptionModel(
        optionType = request.optionType,
        value = request.value.trim(),
        sortOrder = sortOrder,
        enabled = request.enabled,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )
    return productOptionRepository.save(option)
  }

  @Transactional
  override fun updateOption(
    adminId: Long,
    adminName: String,
    id: Long,
    request: ProductOptionUpdateRequest,
  ): ProductOptionModel {
    val existing = productOptionRepository.findById(id).orElseThrow { ProductOptionNotFoundException(id) }
    val newValue = request.value.trim()
    if (newValue != existing.value) {
      validateValueUnique(existing.optionType, newValue, excludeId = id)
    }
    existing.apply {
      value = newValue
      sortOrder = request.sortOrder ?: this.sortOrder
      enabled = request.enabled
    }
    return productOptionRepository.save(existing)
  }

  @Transactional
  override fun deleteOption(
    adminId: Long,
    adminName: String,
    id: Long,
  ): Boolean {
    val existing = productOptionRepository.findById(id).orElseThrow { ProductOptionNotFoundException(id) }
    productOptionRepository.delete(existing)
    return true
  }

  private fun validateValueUnique(
    optionType: ProductOptionType,
    value: String,
    excludeId: Long?,
  ) {
    val trimmed = value.trim()
    val existing = productOptionRepository.findByOptionTypeAndValue(optionType, trimmed)
    if (existing != null && existing.id != excludeId) {
      throw DuplicateProductOptionValueException(optionType.name, trimmed)
    }
  }
}
