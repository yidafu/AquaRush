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

package dev.yidafu.aqua.client.ops.resolvers

import dev.yidafu.aqua.api.service.ops.BannerQueryApiService
import dev.yidafu.aqua.common.annotation.ClientService
import dev.yidafu.aqua.common.graphql.generated.Banner
import dev.yidafu.aqua.ops.banner.mapper.BannerMapper
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

/**
 * 用户端运营配置查询 Resolver
 * 首页轮播位动态读取(F-OPS-001 用户端消费);时间窗过期/停用自动过滤由服务端完成(R-OPS-01)。
 */
@ClientService
@Controller
class ClientOpsQueryResolver(
  private val bannerQueryApiService: BannerQueryApiService,
) {
  /**
   * 当前生效轮播图列表(按排序号升序;未配置/无生效条目返回空列表,用户端隐藏轮播位)
   */
  @QueryMapping
  @PreAuthorize("isAuthenticated()")
  fun activeBanners(): List<Banner> = bannerQueryApiService.getActiveBanners().map { BannerMapper.map(it) }
}
