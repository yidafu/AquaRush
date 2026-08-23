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

package dev.yidafu.aqua.marketing.coupon.service.impl

import dev.yidafu.aqua.api.service.ops.SystemConfigQueryApiService
import dev.yidafu.aqua.common.domain.model.SystemSettingKeys
import dev.yidafu.aqua.common.domain.model.UserCouponModel
import dev.yidafu.aqua.common.domain.model.enums.UserCouponStatus
import dev.yidafu.aqua.common.messaging.event.DomainEventType
import dev.yidafu.aqua.common.messaging.service.SimplifiedEventPublishService
import dev.yidafu.aqua.marketing.coupon.domain.repository.UserCouponRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 优惠券即将过期提醒调度器(PRD 10-marketing.md 优惠券 + 13-notification.md F-NTF-002 消息来源):
 * 每日扫描「已领取且未使用、N 天内过期」的用户优惠券,逐张发布 COUPON_EXPIRING 领域事件,
 * 由通知域 CouponExpiringNotificationHandler 转为站内消息(幂等键 = 券 id + 场景 + 过期时间,
 * 跨天/跨服务重复扫描由消息网关唯一键去重,不重复提醒)。
 *
 * 窗口天数由系统参数 coupon_expiring_notify_days 配置(默认 3 天,仅超管可改);
 * 分页游标扫描(每页 500),遵守「禁止数据表全量查询」规则(规则 13)。
 */
@Component
class CouponExpiryNotifyScheduler(
  private val userCouponRepository: UserCouponRepository,
  private val eventPublishService: SimplifiedEventPublishService,
  private val systemConfigQueryApiService: SystemConfigQueryApiService,
) {
  private val logger = LoggerFactory.getLogger(CouponExpiryNotifyScheduler::class.java)

  @Scheduled(cron = "0 0 8 * * *")
  fun notifyExpiringCoupons() {
    val windowDays = systemConfigQueryApiService.getLongValue(SystemSettingKeys.COUPON_EXPIRING_NOTIFY_DAYS, DEFAULT_WINDOW_DAYS)
    val now = LocalDateTime.now()
    val end = now.plusDays(windowDays)
    var page = 0
    var published = 0

    while (true) {
      val result =
        userCouponRepository.findByStatusAndExpireTimeBetween(
          status = UserCouponStatus.RECEIVED,
          from = now,
          to = end,
          pageable = PageRequest.of(page, PAGE_SIZE),
        )
      if (result.content.isEmpty()) {
        break
      }
      result.content.forEach { coupon ->
        if (publish(coupon)) {
          published++
        }
      }
      if (!result.hasNext()) {
        break
      }
      page++
    }

    if (published > 0) {
      logger.info("优惠券即将过期提醒完成: 共发布 {} 条通知事件(窗口 {} 天)", published, windowDays)
    }
  }

  private fun publish(coupon: UserCouponModel): Boolean {
    val couponId = coupon.id ?: return false
    eventPublishService.publishDomainEvent(
      eventType = DomainEventType.COUPON_EXPIRING.value,
      aggregateId = couponId.toString(),
      eventData =
        mapOf(
          "userId" to coupon.userId,
          "couponId" to couponId,
          "couponName" to coupon.name,
          "expireTime" to
            coupon.expireTime
              .atZone(ZoneId.systemDefault())
              .toInstant()
              .toEpochMilli(),
        ),
    )
    return true
  }

  companion object {
    private const val DEFAULT_WINDOW_DAYS = 3L
    private const val PAGE_SIZE = 500
  }
}
