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

package dev.yidafu.aqua.admin.ops.resolvers

import dev.yidafu.aqua.api.service.ops.BannerQueryService
import dev.yidafu.aqua.common.graphql.generated.Banner
import dev.yidafu.aqua.common.graphql.generated.BannerListInput
import dev.yidafu.aqua.common.graphql.generated.BannerVoPage
import dev.yidafu.aqua.ops.banner.mapper.BannerListInputMapper
import dev.yidafu.aqua.ops.banner.mapper.BannerMapper
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

/**
 * 轮播图查询 Resolver(管理后台,PERMISSION_OPS_CONFIG_READ)
 * 与 PRD 12-operations-config.md §F-OPS-001 保持一致。
 */
@Controller
class AdminBannerQueryResolver(
  private val bannerQueryService: BannerQueryService,
) {
  /**
   * 分页查询轮播图列表(按排序号升序)
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_OPS_CONFIG_READ')")
  fun banners(
    @Argument input: BannerListInput?,
  ): BannerVoPage {
    val request =
      input?.let { BannerListInputMapper.map(it) } ?: dev.yidafu.aqua.api.dto
        .BannerListRequest()
    val page = bannerQueryService.listBanners(request)
    return BannerVoPage(
      content = page.content.map { BannerMapper.map(it) },
      totalElements = page.totalElements.toInt(),
      totalPages = page.totalPages,
      size = page.size,
      number = page.number,
    )
  }

  /**
   * 根据 ID 查询轮播图
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_OPS_CONFIG_READ')")
  fun banner(
    @Argument id: Long,
  ): Banner? = bannerQueryService.getBanner(id)?.let { BannerMapper.map(it) }
}
