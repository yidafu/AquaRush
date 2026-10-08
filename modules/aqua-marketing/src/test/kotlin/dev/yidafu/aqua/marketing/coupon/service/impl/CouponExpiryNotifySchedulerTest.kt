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
import dev.yidafu.aqua.common.domain.model.UserCouponModel
import dev.yidafu.aqua.common.domain.model.enums.UserCouponStatus
import dev.yidafu.aqua.common.messaging.event.DomainEventType
import dev.yidafu.aqua.common.messaging.service.SimplifiedEventPublishService
import dev.yidafu.aqua.marketing.coupon.domain.repository.UserCouponRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 优惠券即将过期提醒调度器测试(PRD 10-marketing.md / 13-notification.md F-NTF-002):
 * 按配置窗口分页扫描到期券并逐张发布 COUPON_EXPIRING 事件,载荷含稳定过期时间(幂等键基准)。
 */
class CouponExpiryNotifySchedulerTest :
  FunSpec({
    var userCouponRepository: UserCouponRepository = mockk()
    var eventPublishService: SimplifiedEventPublishService = mockk()
    var systemConfigQueryApiService: SystemConfigQueryApiService = mockk()
    var scheduler: CouponExpiryNotifyScheduler = mockk()

    fun coupon(
      id: Long,
      userId: Long = 1L,
      name: String = "满100减20",
      expireTime: LocalDateTime = LocalDateTime.now().plusDays(2),
    ): UserCouponModel =
      UserCouponModel(
        id = id,
        userId = userId,
        couponTemplateId = 100L,
        status = UserCouponStatus.RECEIVED,
        name = name,
        expireTime = expireTime,
      )

    beforeTest {
      userCouponRepository = mockk(relaxed = true)
      eventPublishService = mockk(relaxed = true)
      systemConfigQueryApiService = mockk(relaxed = true)
      scheduler =
        CouponExpiryNotifyScheduler(
          userCouponRepository = userCouponRepository,
          eventPublishService = eventPublishService,
          systemConfigQueryApiService = systemConfigQueryApiService,
        )
    }

    test("窗口内到期券逐张发布 COUPON_EXPIRING 事件,载荷含稳定过期时间") {
      every { systemConfigQueryApiService.getLongValue("coupon_expiring_notify_days", 3L) } returns 3L
      val expire = LocalDateTime.now().plusDays(2)
      val page =
        PageImpl(
          listOf(
            coupon(id = 5001L, userId = 7L, expireTime = expire),
            coupon(id = 5002L, userId = 8L, name = "新客立减", expireTime = expire),
          ),
        )
      every {
        userCouponRepository.findByStatusAndExpireTimeBetween(UserCouponStatus.RECEIVED, any(), any(), any())
      } returns page

      scheduler.notifyExpiringCoupons()

      verify(exactly = 2) { eventPublishService.publishDomainEvent(any(), any(), any()) }
      val captured = slot<Map<String, Any>>()
      verify {
        eventPublishService.publishDomainEvent(
          match { it == DomainEventType.COUPON_EXPIRING.value },
          match { it == "5001" },
          capture(captured),
        )
      }
      captured.captured["userId"] shouldBe 7L
      captured.captured["couponId"] shouldBe 5001L
      captured.captured["couponName"] shouldBe "满100减20"
      captured.captured["expireTime"] shouldBe expire.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    test("分页扫描:hasNext 时继续下一页") {
      every { systemConfigQueryApiService.getLongValue("coupon_expiring_notify_days", 3L) } returns 3L
      every {
        userCouponRepository.findByStatusAndExpireTimeBetween(UserCouponStatus.RECEIVED, any(), any(), any())
      } returnsMany
        listOf(
          // 第 1 页有数据且 hasNext=true(总条数超过单页),继续取下一页
          PageImpl(listOf(coupon(id = 5001L)), PageRequest.of(0, 500), 501),
          PageImpl(emptyList(), PageRequest.of(1, 500), 501),
        )

      scheduler.notifyExpiringCoupons()

      verify(exactly = 2) { userCouponRepository.findByStatusAndExpireTimeBetween(UserCouponStatus.RECEIVED, any(), any(), any()) }
      verify(exactly = 1) { eventPublishService.publishDomainEvent(any(), any(), any()) }
    }

    test("无到期券时不发布任何事件") {
      every { systemConfigQueryApiService.getLongValue("coupon_expiring_notify_days", 3L) } returns 3L
      every {
        userCouponRepository.findByStatusAndExpireTimeBetween(UserCouponStatus.RECEIVED, any(), any(), any())
      } returns PageImpl(emptyList())

      scheduler.notifyExpiringCoupons()

      verify(exactly = 0) { eventPublishService.publishDomainEvent(any(), any(), any()) }
    }
  })
