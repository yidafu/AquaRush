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

package dev.yidafu.aqua.product.domain.repository

import dev.yidafu.aqua.api.dto.ProductSearchRequest
import dev.yidafu.aqua.common.domain.model.ProductModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * Custom repository for Product queries using QueryDSL
 */
interface ProductRepositoryCustom {
  /**
   * Search products with optional keyword and status filters
   * @param keyword Search keyword for product name (uses LIKE query)
   * @param status Optional status filter
   * @param pageable Pagination parameters
   * @return Page of matching products
   */
  fun searchProducts(
    query: ProductSearchRequest,
    pageable: Pageable,
  ): Page<ProductModel>
}
