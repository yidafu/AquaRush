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

import dev.yidafu.aqua.api.service.marketing.FirstOrderActivityQueryService
import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.common.domain.model.FirstOrderActivityModel
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus
import dev.yidafu.aqua.common.graphql.generated.FirstOrderActivity
import dev.yidafu.aqua.common.graphql.generated.FirstOrderActivityListInput
import dev.yidafu.aqua.common.graphql.generated.FirstOrderActivityVoPage
import dev.yidafu.aqua.common.graphql.generated.PromotionActivityDisplayStatus
import dev.yidafu.aqua.marketing.firstorder.mapper.toGraphQL
import org.springframework.data.domain.PageRequest
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import java.time.LocalDateTime

@Controller
class AdminFirstOrderActivityQueryResolver(
  private val firstOrderActivityQueryService: FirstOrderActivityQueryService,
  private val productQueryApiService: ProductQueryApiService,
) {
  /**
   * 分页查询首单立减/新人专享价活动列表(附商品名与展示态)
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_MKT_READ')")
  fun firstOrderActivities(
    @Argument input: FirstOrderActivityListInput?,
  ): FirstOrderActivityVoPage {
    val page =
      firstOrderActivityQueryService.listActivities(
        keyword = input?.keyword,
        pageable = PageRequest.of(input?.page ?: 0, input?.size ?: 20),
      )
    return FirstOrderActivityVoPage(
      content = page.content.map { toFirstOrderActivity(it) },
      number = page.number,
      size = page.size,
      totalElements = page.totalElements.toInt(),
      totalPages = page.totalPages,
    )
  }

  /**
   * 根据 ID 查询首单立减/新人专享价活动
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_MKT_READ')")
  fun firstOrderActivity(
    @Argument id: Long,
  ): FirstOrderActivity? = firstOrderActivityQueryService.getActivity(id)?.let { toFirstOrderActivity(it) }

  private fun toFirstOrderActivity(model: FirstOrderActivityModel): FirstOrderActivity {
    val now = LocalDateTime.now()
    val currentStatus =
      when {
        model.status == PromotionActivityStatus.DISABLED -> PromotionActivityDisplayStatus.DISABLED
        now.isBefore(model.validFrom) -> PromotionActivityDisplayStatus.UPCOMING
        now.isAfter(model.validTo) -> PromotionActivityDisplayStatus.ENDED
        else -> PromotionActivityDisplayStatus.ONGOING
      }
    return model.toGraphQL(
      currentStatus,
      productName = model.productId?.let { productQueryApiService.findById(it)?.name },
    )
  }
}
