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

package dev.yidafu.aqua.admin.marketing.resolvers

import dev.yidafu.aqua.api.service.marketing.GroupProductPriceQueryService
import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.common.graphql.generated.GroupProductPriceListInput
import dev.yidafu.aqua.common.graphql.generated.GroupProductPriceVoPage
import dev.yidafu.aqua.marketing.pricing.mapper.toGraphQL
import org.springframework.data.domain.PageRequest
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

/**
 * 分组商品专属价查询 Resolver(管理后台,PERMISSION_MKT_READ)
 * 与 PRD 10-marketing.md §F-MKT-002 保持一致;商品名称/规格/目录价经商品服务补充。
 */
@Controller
class AdminGroupProductPriceQueryResolver(
  private val groupProductPriceQueryService: GroupProductPriceQueryService,
  private val productQueryApiService: ProductQueryApiService,
) {
  /**
   * 分页查询分组下已配置的专属价列表
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_MKT_READ')")
  fun groupProductPrices(
    @Argument input: GroupProductPriceListInput,
  ): GroupProductPriceVoPage {
    val page =
      groupProductPriceQueryService.listByGroup(
        groupId = input.customerGroupId,
        pageable = PageRequest.of(input.page ?: 0, input.size ?: 20),
      )
    return GroupProductPriceVoPage(
      content =
        page.content.map { model ->
          val product = productQueryApiService.findById(model.productId)
          model.toGraphQL(
            productName = product?.name,
            productSpecification = product?.specification,
            catalogPriceCents = product?.price ?: 0L,
          )
        },
      number = page.number,
      size = page.size,
      totalElements = page.totalElements.toInt(),
      totalPages = page.totalPages,
    )
  }
}
