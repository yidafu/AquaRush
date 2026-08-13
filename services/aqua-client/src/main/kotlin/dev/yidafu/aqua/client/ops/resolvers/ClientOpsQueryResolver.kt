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
import dev.yidafu.aqua.api.service.ops.DeliveryFeeRuleQueryApiService
import dev.yidafu.aqua.api.service.ops.NoticeQueryApiService
import dev.yidafu.aqua.api.service.ops.StoreProfileQueryApiService
import dev.yidafu.aqua.common.annotation.ClientService
import dev.yidafu.aqua.common.graphql.generated.Banner
import dev.yidafu.aqua.common.graphql.generated.DeliveryFeeRule
import dev.yidafu.aqua.common.graphql.generated.Notice
import dev.yidafu.aqua.common.graphql.generated.StoreProfile
import dev.yidafu.aqua.ops.banner.mapper.BannerMapper
import dev.yidafu.aqua.ops.deliveryfeerule.mapper.DeliveryFeeRuleMapper
import dev.yidafu.aqua.ops.notice.mapper.NoticeMapper
import dev.yidafu.aqua.ops.storeprofile.mapper.StoreProfileMapper
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

/**
 * 用户端运营配置查询 Resolver
 * 轮播位(F-OPS-001)/公告栏(F-OPS-002)/店铺信息(F-OPS-004)/配送费规则(F-OPS-005)用户端消费;
 * 时间窗过期/停用自动过滤由服务端完成(R-OPS-01),未配置时按默认值兜底。
 */
@ClientService
@Controller
class ClientOpsQueryResolver(
  private val bannerQueryApiService: BannerQueryApiService,
  private val noticeQueryApiService: NoticeQueryApiService,
  private val storeProfileQueryApiService: StoreProfileQueryApiService,
  private val deliveryFeeRuleQueryApiService: DeliveryFeeRuleQueryApiService,
) {
  /**
   * 当前生效轮播图列表(按排序号升序;未配置/无生效条目返回空列表,用户端隐藏轮播位)
   */
  @QueryMapping
  @PreAuthorize("isAuthenticated()")
  fun activeBanners(): List<Banner> = bannerQueryApiService.getActiveBanners().map { BannerMapper.map(it) }

  /**
   * 当前生效公告列表(已发布且在时间窗内,置顶优先、发布时间倒序;无公告返回空列表,用户端隐藏公告栏)
   */
  @QueryMapping
  @PreAuthorize("isAuthenticated()")
  fun activeNotices(): List<Notice> = noticeQueryApiService.getActiveNotices().map { NoticeMapper.map(it) }

  /**
   * 店铺信息(未配置返回 null,用户端按默认值兜底展示)
   */
  @QueryMapping
  @PreAuthorize("isAuthenticated()")
  fun storeProfile(): StoreProfile? = storeProfileQueryApiService.getStoreProfile()?.let { StoreProfileMapper.map(it) }

  /**
   * 配送费规则(未配置返回默认规则 免费4楼/加价2元,确认订单页据此展示运费)
   */
  @QueryMapping
  @PreAuthorize("isAuthenticated()")
  fun deliveryFeeRule(): DeliveryFeeRule = DeliveryFeeRuleMapper.map(deliveryFeeRuleQueryApiService.getEffectiveDeliveryFeeRule())
}
