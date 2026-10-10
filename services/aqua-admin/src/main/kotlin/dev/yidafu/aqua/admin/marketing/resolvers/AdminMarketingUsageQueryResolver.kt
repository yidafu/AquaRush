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

import dev.yidafu.aqua.admin.order.mapper.AdminOrderMapper
import dev.yidafu.aqua.api.dto.MarketingUsagePage
import dev.yidafu.aqua.api.service.marketing.MarketingUsageQueryService
import dev.yidafu.aqua.common.graphql.generated.MarketingUsageListInput
import dev.yidafu.aqua.common.graphql.generated.MarketingUsageSummaryVo
import dev.yidafu.aqua.common.graphql.generated.MarketingUsageVo
import dev.yidafu.aqua.common.graphql.generated.OrderVoPage
import dev.yidafu.aqua.common.graphql.util.toPageInfo
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

/**
 * 营销方式使用情况查询 Resolver(管理后台,PERMISSION_MKT_READ):
 * 营销方式详情页「营销使用情况 + 相关订单记录」(PRD 10-marketing.md F-MKT-003~009)。
 * 口径见 MarketingUsageKind / MarketingUsageSummaryVo;限时特价/新人价按商品+活动价+时间窗近似匹配,
 * 赠品让利按当前目录价估算。
 */
@Controller
class AdminMarketingUsageQueryResolver(
  private val marketingUsageQueryService: MarketingUsageQueryService,
  private val adminOrderMapper: AdminOrderMapper,
) {
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_MKT_READ')")
  fun couponTemplateUsage(
    @Argument couponTemplateId: Long,
    @Argument input: MarketingUsageListInput?,
  ): MarketingUsageVo =
    marketingUsageQueryService
      .couponTemplateUsage(couponTemplateId, input?.page ?: 0, input?.size ?: 20)
      .toMarketingUsageVo()

  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_MKT_READ')")
  fun promotionActivityUsage(
    @Argument promotionActivityId: Long,
    @Argument input: MarketingUsageListInput?,
  ): MarketingUsageVo =
    marketingUsageQueryService
      .promotionActivityUsage(promotionActivityId, input?.page ?: 0, input?.size ?: 20)
      .toMarketingUsageVo()

  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_MKT_READ')")
  fun flashSaleActivityUsage(
    @Argument flashSaleActivityId: Long,
    @Argument input: MarketingUsageListInput?,
  ): MarketingUsageVo =
    marketingUsageQueryService
      .flashSaleActivityUsage(flashSaleActivityId, input?.page ?: 0, input?.size ?: 20)
      .toMarketingUsageVo()

  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_MKT_READ')")
  fun giftActivityUsage(
    @Argument giftActivityId: Long,
    @Argument input: MarketingUsageListInput?,
  ): MarketingUsageVo =
    marketingUsageQueryService
      .giftActivityUsage(giftActivityId, input?.page ?: 0, input?.size ?: 20)
      .toMarketingUsageVo()

  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_MKT_READ')")
  fun firstOrderActivityUsage(
    @Argument firstOrderActivityId: Long,
    @Argument input: MarketingUsageListInput?,
  ): MarketingUsageVo =
    marketingUsageQueryService
      .firstOrderActivityUsage(firstOrderActivityId, input?.page ?: 0, input?.size ?: 20)
      .toMarketingUsageVo()

  private fun MarketingUsagePage.toMarketingUsageVo(): MarketingUsageVo {
    val (orderList, pageInfo) = orders.toPageInfo { adminOrderMapper.map(it) }
    return MarketingUsageVo(
      summary =
        MarketingUsageSummaryVo(
          orderCount = summary.orderCount.toInt(),
          benefitCents = summary.benefitCents,
          usageCount = summary.usageCount.toInt(),
        ),
      orders =
        OrderVoPage(
          content = orderList,
          totalElements = pageInfo.total,
          totalPages = pageInfo.totalPages,
          size = pageInfo.pageSize,
          number = pageInfo.pageNum,
        ),
    )
  }
}
