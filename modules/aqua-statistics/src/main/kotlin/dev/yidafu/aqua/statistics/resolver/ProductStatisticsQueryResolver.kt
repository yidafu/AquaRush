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

package dev.yidafu.aqua.statistics.resolver

import dev.yidafu.aqua.common.graphql.generated.ProductStatistics
import dev.yidafu.aqua.statistics.service.impl.ProductStatisticsServiceImpl
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

/**
 * 商品统计 GraphQL 查询解析器
 */
@Controller
class ProductStatisticsQueryResolver(
  private val productStatisticsService: ProductStatisticsServiceImpl,
) {
  @QueryMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun productStatistics(): ProductStatistics {
    return productStatisticsService.getProductStatistics()
  }
}