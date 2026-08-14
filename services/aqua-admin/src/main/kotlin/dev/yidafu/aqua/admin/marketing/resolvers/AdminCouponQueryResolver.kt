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

import dev.yidafu.aqua.api.service.marketing.CouponQueryService
import dev.yidafu.aqua.common.graphql.generated.CouponTemplate
import dev.yidafu.aqua.common.graphql.generated.CouponTemplateListInput
import dev.yidafu.aqua.common.graphql.generated.CouponTemplateVoPage
import dev.yidafu.aqua.marketing.coupon.mapper.CouponTemplateMapper
import dev.yidafu.aqua.marketing.coupon.mapper.CouponTemplateStatusInputMapper
import org.springframework.data.domain.PageRequest
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

/**
 * 优惠券模板查询 Resolver(管理后台,PERMISSION_MKT_READ)
 * 与 PRD 10-marketing.md §F-MKT-003 保持一致;列表附已领/已核销统计。
 */
@Controller
class AdminCouponQueryResolver(
  private val couponQueryService: CouponQueryService,
) {
  /**
   * 分页查询优惠券模板列表(附已领/已核销统计)
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_MKT_READ')")
  fun couponTemplates(
    @Argument input: CouponTemplateListInput?,
  ): CouponTemplateVoPage {
    val status =
      input?.status?.let { CouponTemplateStatusInputMapper.map(it) }
    val page =
      couponQueryService.listTemplates(
        keyword = input?.keyword,
        status = status,
        pageable = PageRequest.of(input?.page ?: 0, input?.size ?: 20),
      )
    return CouponTemplateVoPage(
      content =
        page.content.map { template ->
          val templateId = template.id ?: 0L
          CouponTemplateMapper.toGraphQL(
            model = template,
            receivedCount = couponQueryService.receivedCount(templateId).toInt(),
            usedCount = couponQueryService.usedCount(templateId).toInt(),
          )
        },
      number = page.number,
      size = page.size,
      totalElements = page.totalElements.toInt(),
      totalPages = page.totalPages,
    )
  }

  /**
   * 根据 ID 查询优惠券模板
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_MKT_READ')")
  fun couponTemplate(
    @Argument id: Long,
  ): CouponTemplate? {
    val template = couponQueryService.getTemplate(id) ?: return null
    return CouponTemplateMapper.toGraphQL(
      model = template,
      receivedCount = couponQueryService.receivedCount(id).toInt(),
      usedCount = couponQueryService.usedCount(id).toInt(),
    )
  }
}
