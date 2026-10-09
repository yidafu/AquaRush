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
import dev.yidafu.aqua.api.service.ops.SystemConfigQueryApiService
import dev.yidafu.aqua.common.annotation.ClientService
import dev.yidafu.aqua.common.domain.model.SystemSettingKeys
import dev.yidafu.aqua.common.graphql.generated.Banner
import dev.yidafu.aqua.common.graphql.generated.DeliveryFeeRule
import dev.yidafu.aqua.common.graphql.generated.DeliverySlotVo
import dev.yidafu.aqua.common.graphql.generated.DeliveryTimeSlot
import dev.yidafu.aqua.common.graphql.generated.Notice
import dev.yidafu.aqua.common.graphql.generated.StoreProfile
import dev.yidafu.aqua.ops.banner.mapper.toGraphQL
import dev.yidafu.aqua.ops.deliveryfeerule.mapper.toGraphQL
import dev.yidafu.aqua.ops.notice.mapper.toGraphQL
import dev.yidafu.aqua.ops.storeprofile.mapper.toGraphQL
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

/**
 * 用户端运营配置查询 Resolver
 * 轮播位(F-OPS-001)/公告栏(F-OPS-002)/店铺信息(F-OPS-004)/配送费规则(F-OPS-005)/配送时段(F-SYS-002)用户端消费;
 * 时间窗过期/停用自动过滤由服务端完成(R-OPS-01),未配置时按默认值兜底。
 */
@ClientService
@Controller
class ClientOpsQueryResolver(
  private val bannerQueryApiService: BannerQueryApiService,
  private val noticeQueryApiService: NoticeQueryApiService,
  private val storeProfileQueryApiService: StoreProfileQueryApiService,
  private val deliveryFeeRuleQueryApiService: DeliveryFeeRuleQueryApiService,
  private val systemConfigQueryApiService: SystemConfigQueryApiService,
) {
  /**
   * 当前生效轮播图列表(按排序号升序;未配置/无生效条目返回空列表,用户端隐藏轮播位)
   */
  @QueryMapping
  @PreAuthorize("isAuthenticated()")
  fun activeBanners(): List<Banner> = bannerQueryApiService.getActiveBanners().map { it.toGraphQL() }

  /**
   * 当前生效公告列表(已发布且在时间窗内,置顶优先、发布时间倒序;无公告返回空列表,用户端隐藏公告栏)
   */
  @QueryMapping
  @PreAuthorize("isAuthenticated()")
  fun activeNotices(): List<Notice> = noticeQueryApiService.getActiveNotices().map { it.toGraphQL() }

  /**
   * 店铺信息(未配置返回 null,用户端按默认值兜底展示)
   */
  @QueryMapping
  @PreAuthorize("isAuthenticated()")
  fun storeProfile(): StoreProfile? = storeProfileQueryApiService.getStoreProfile()?.let { it.toGraphQL() }

  /**
   * 配送费规则(未配置返回默认规则 免费4楼/加价2元,确认订单页据此展示运费)
   */
  @QueryMapping
  @PreAuthorize("isAuthenticated()")
  fun deliveryFeeRule(): DeliveryFeeRule = deliveryFeeRuleQueryApiService.getEffectiveDeliveryFeeRule().toGraphQL()

  /**
   * 配送时段配置(F-SYS-002,AC-SYS-002-01/03):
   * 后台「订单与履约参数」页配置立即配送时限/上午/下午/晚上时间窗与启停,保存即热生效;
   * 固定返回 4 个时段(IMMEDIATE 恒启用),确认订单页只展示 enabled=true 的时段,
   * 订单详情按 id 映射全量时段文案(含已停用,历史快照展示用);
   * 未配置/读取失败时按种子默认兜底(30分钟内 / 9:00-12:00 / 14:00-18:00 / 18:00-21:00)。
   */
  @QueryMapping
  @PreAuthorize("isAuthenticated()")
  fun deliverySlots(): List<DeliverySlotVo> {
    val immediateMinutes =
      systemConfigQueryApiService.getLongValue(SystemSettingKeys.DELIVERY_SLOT_IMMEDIATE_MINUTES, DEFAULT_IMMEDIATE_MINUTES)
    return listOf(
      DeliverySlotVo(
        id = DeliveryTimeSlot.IMMEDIATE,
        name = "立即配送",
        hint = "${immediateMinutes}分钟内",
        enabled = true,
      ),
      windowSlot(
        slot = DeliveryTimeSlot.MORNING,
        name = "上午配送",
        enabledKey = SystemSettingKeys.DELIVERY_SLOT_MORNING_ENABLED,
        startKey = SystemSettingKeys.DELIVERY_SLOT_MORNING_START,
        endKey = SystemSettingKeys.DELIVERY_SLOT_MORNING_END,
        defaultStart = "09:00",
        defaultEnd = "12:00",
      ),
      windowSlot(
        slot = DeliveryTimeSlot.AFTERNOON,
        name = "下午配送",
        enabledKey = SystemSettingKeys.DELIVERY_SLOT_AFTERNOON_ENABLED,
        startKey = SystemSettingKeys.DELIVERY_SLOT_AFTERNOON_START,
        endKey = SystemSettingKeys.DELIVERY_SLOT_AFTERNOON_END,
        defaultStart = "14:00",
        defaultEnd = "18:00",
      ),
      windowSlot(
        slot = DeliveryTimeSlot.EVENING,
        name = "晚上配送",
        enabledKey = SystemSettingKeys.DELIVERY_SLOT_EVENING_ENABLED,
        startKey = SystemSettingKeys.DELIVERY_SLOT_EVENING_START,
        endKey = SystemSettingKeys.DELIVERY_SLOT_EVENING_END,
        defaultStart = "18:00",
        defaultEnd = "21:00",
      ),
    )
  }

  /** 时间窗时段:启用开关 + 起止时间(HH:mm),小时去前导零展示(与既有 UI「9:00-12:00」一致) */
  private fun windowSlot(
    slot: DeliveryTimeSlot,
    name: String,
    enabledKey: String,
    startKey: String,
    endKey: String,
    defaultStart: String,
    defaultEnd: String,
  ): DeliverySlotVo {
    val start = systemConfigQueryApiService.getStringValue(startKey, defaultStart)
    val end = systemConfigQueryApiService.getStringValue(endKey, defaultEnd)
    val hint = "${formatTime(start)}-${formatTime(end)}"
    return DeliverySlotVo(
      id = slot,
      name = name,
      hint = hint,
      enabled = systemConfigQueryApiService.getBooleanValue(enabledKey, true),
    )
  }

  private fun formatTime(time: String?): String {
    if (time.isNullOrBlank()) return ""
    // "09:00" -> "9:00"(小时去前导零,分钟保留两位)
    val parts = time.split(":")
    if (parts.size != 2) return time
    val hour = parts[0].toIntOrNull() ?: return time
    return "$hour:${parts[1]}"
  }

  private companion object {
    const val DEFAULT_IMMEDIATE_MINUTES = 30L
  }
}
