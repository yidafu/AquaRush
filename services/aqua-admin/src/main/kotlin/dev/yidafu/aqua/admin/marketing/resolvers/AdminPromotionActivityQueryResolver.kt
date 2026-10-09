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

import dev.yidafu.aqua.api.service.marketing.PromotionActivityQueryService
import dev.yidafu.aqua.common.domain.model.PromotionActivityModel
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus
import dev.yidafu.aqua.common.graphql.generated.PromotionActivity
import dev.yidafu.aqua.common.graphql.generated.PromotionActivityDisplayStatus
import dev.yidafu.aqua.common.graphql.generated.PromotionActivityListInput
import dev.yidafu.aqua.common.graphql.generated.PromotionActivityVoPage
import dev.yidafu.aqua.marketing.promotion.mapper.toGraphQL
import org.springframework.data.domain.PageRequest
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import java.time.LocalDateTime

/**
 * 满减/折扣活动查询 Resolver(管理后台,PERMISSION_MKT_READ)
 * 与 PRD 10-marketing.md §F-MKT-004 保持一致;展示态由停用状态与时间窗计算。
 */
@Controller
class AdminPromotionActivityQueryResolver(
  private val promotionActivityQueryService: PromotionActivityQueryService,
) {
  /**
   * 分页查询活动列表(附档位与展示态)
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_MKT_READ')")
  fun promotionActivities(
    @Argument input: PromotionActivityListInput?,
  ): PromotionActivityVoPage {
    val page =
      promotionActivityQueryService.listActivities(
        keyword = input?.keyword,
        pageable = PageRequest.of(input?.page ?: 0, input?.size ?: 20),
      )
    return PromotionActivityVoPage(
      content = page.content.map { toPromotionActivity(it) },
      number = page.number,
      size = page.size,
      totalElements = page.totalElements.toInt(),
      totalPages = page.totalPages,
    )
  }

  /**
   * 根据 ID 查询活动
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_MKT_READ')")
  fun promotionActivity(
    @Argument id: Long,
  ): PromotionActivity? = promotionActivityQueryService.getActivity(id)?.let { toPromotionActivity(it) }

  private fun toPromotionActivity(model: PromotionActivityModel): PromotionActivity {
    val tiers = promotionActivityQueryService.listTiers(model.id ?: 0L)
    val now = LocalDateTime.now()
    val currentStatus =
      when {
        model.status == PromotionActivityStatus.DISABLED -> PromotionActivityDisplayStatus.DISABLED
        now.isBefore(model.validFrom) -> PromotionActivityDisplayStatus.UPCOMING
        now.isAfter(model.validTo) -> PromotionActivityDisplayStatus.ENDED
        else -> PromotionActivityDisplayStatus.ONGOING
      }
    return model.toGraphQL(tiers, currentStatus)
  }
}
