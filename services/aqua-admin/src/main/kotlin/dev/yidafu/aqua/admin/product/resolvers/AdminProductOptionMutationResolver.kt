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

package dev.yidafu.aqua.admin.product.resolvers

import dev.yidafu.aqua.api.service.product.ProductOptionMutationService
import dev.yidafu.aqua.common.graphql.generated.CreateProductOptionInput
import dev.yidafu.aqua.common.graphql.generated.ProductOption
import dev.yidafu.aqua.common.graphql.generated.UpdateProductOptionInput
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.product.mapper.toGraphQL
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

/**
 * 商品属性选项字典写操作 Resolver(管理后台,PERMISSION_PRODUCT_WRITE)
 * 与 PRD 03-product.md §F-PRO-009 保持一致:规格/水源地选项的增改删。
 */
@Controller
class AdminProductOptionMutationResolver(
  private val productOptionMutationService: ProductOptionMutationService,
) {
  private val logger = LoggerFactory.getLogger(AdminProductOptionMutationResolver::class.java)

  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_PRODUCT_WRITE')")
  fun createProductOption(
    @Argument @Valid input: CreateProductOptionInput,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): ProductOption {
    logger.info("Admin {} creating product option: {}", principal.username, input.value)
    val option =
      productOptionMutationService.createOption(
        adminId = principal.id,
        adminName = principal.username,
        input = input,
      )
    return option.toGraphQL()
  }

  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_PRODUCT_WRITE')")
  fun updateProductOption(
    @Argument id: Long,
    @Argument @Valid input: UpdateProductOptionInput,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): ProductOption {
    logger.info("Admin {} updating product option {}", principal.username, id)
    val option =
      productOptionMutationService.updateOption(
        adminId = principal.id,
        adminName = principal.username,
        id = id,
        input = input,
      )
    return option.toGraphQL()
  }

  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_PRODUCT_WRITE')")
  fun deleteProductOption(
    @Argument id: Long,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): Boolean {
    logger.info("Admin {} deleting product option {}", principal.username, id)
    return productOptionMutationService.deleteOption(principal.id, principal.username, id)
  }
}
