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

package dev.yidafu.aqua.product.domain.repository

import dev.yidafu.aqua.common.graphql.generated.ProductStatus
import dev.yidafu.aqua.product.domain.model.Product
import org.springframework.data.jpa.domain.Specification

class ProductSpecifications {
    companion object {
        fun byId(id: Long): Specification<Product> {
            return Specification { root, _, cb ->
                cb.equal(root.get<Long>("id"), id)
            }
        }

        fun byStatus(status: ProductStatus): Specification<Product> {
            return Specification { root, _, cb ->
                cb.equal(root.get<Enum<*>>("status"), status)
            }
        }

        fun stockGreaterThanOrEqualTo(minStock: Int): Specification<Product> {
            return Specification { root, _, cb ->
                cb.greaterThanOrEqualTo(root.get<Int>("stock"), minStock)
            }
        }
    }
}
