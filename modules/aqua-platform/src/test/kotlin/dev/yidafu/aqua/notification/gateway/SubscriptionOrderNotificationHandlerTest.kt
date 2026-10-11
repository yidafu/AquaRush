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

package dev.yidafu.aqua.notification.gateway

import dev.yidafu.aqua.api.service.ops.SystemConfigQueryApiService
import dev.yidafu.aqua.common.messaging.event.DomainEvent
import dev.yidafu.aqua.common.messaging.service.SimplifiedEventPublishService
import dev.yidafu.aqua.notification.dto.NotificationEventData
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import tools.jackson.module.kotlin.jacksonObjectMapper

/**
 * 周期订购到期提醒 handler 测试(G77/F-ORD-026:提醒送水工按时配送,非提醒用户):
 * 绑定送水工 → 定向落库;未绑定 → 广播;开关关闭不发;幂等键锚定计划到期日。
 */
class SubscriptionOrderNotificationHandlerTest :
  FunSpec({
    val objectMapper = jacksonObjectMapper()
    val eventTime = 1715000000000L

    fun publishHarness(): Pair<SimplifiedEventPublishService, MutableList<Map<String, Any>>> {
      val maps = mutableListOf<Map<String, Any>>()
      val service = mockk<SimplifiedEventPublishService>(relaxed = true)
      every { service.publishDomainEvent(any(), any(), capture(maps)) } returns true
      return service to maps
    }

    fun businessEvent(data: Map<String, Any>): DomainEvent =
      DomainEvent(
        eventType = "SUBSCRIPTION_ORDER_CREATED",
        aggregateId = data["orderNo"]?.toString() ?: "-",
        payload = objectMapper.writeValueAsString(data),
      )

    fun config(enabled: Boolean): SystemConfigQueryApiService {
      val service = mockk<SystemConfigQueryApiService>(relaxed = true)
      every { service.getBooleanValue("delivery_push_subscription_enabled", true) } returns enabled
      return service
    }

    test("bound worker: targeted in-app message with time slot") {
      val (eventPublishService, maps) = publishHarness()
      val handler = SubscriptionOrderNotificationHandler(NotificationEventPublisher(eventPublishService), config(true))

      handler.handle(
        businessEvent(
          mapOf(
            "orderId" to 1001L,
            "orderNo" to "ORD-SUB-1",
            "period" to 3L,
            "deliveryTimeSlot" to "MORNING",
            "workerId" to 42L,
            "timestamp" to eventTime,
          ),
        ),
      )

      val events = maps.map { objectMapper.convertValue(it, NotificationEventData::class.java) }
      events.size shouldBe 1
      val e = events.single()
      e.receiverType shouldBe "WORKER"
      e.workerId shouldBe 42L
      e.broadcast shouldBe false
      e.messageType shouldBe "ORDER"
      e.scene shouldBe "SUBSCRIPTION_ORDER_CREATED"
      e.title shouldBe "周期配送提醒"
      e.summary!!.contains("第3期") shouldBe true
      e.summary!!.contains("上午(9:00-12:00)") shouldBe true
      e.summary!!.contains("ORD-SUB-1") shouldBe true
      e.relatedNo shouldBe "ORD-SUB-1"
      e.eventTime shouldBe eventTime
      e.idempotencyKey shouldBe "ORD-SUB-1:SUBSCRIPTION_ORDER_CREATED:$eventTime"
    }

    test("no bound worker: broadcast to online workers") {
      val (eventPublishService, maps) = publishHarness()
      val handler = SubscriptionOrderNotificationHandler(NotificationEventPublisher(eventPublishService), config(true))

      handler.handle(
        businessEvent(
          mapOf(
            "orderId" to 1001L,
            "orderNo" to "ORD-SUB-1",
            "period" to 1L,
            "deliveryTimeSlot" to "AFTERNOON",
            "timestamp" to eventTime,
          ),
        ),
      )

      val events = maps.map { objectMapper.convertValue(it, NotificationEventData::class.java) }
      val e = events.single()
      e.broadcast shouldBe true
      e.workerId shouldBe null
      e.summary!!.contains("下午(14:00-18:00)") shouldBe true
    }

    test("switch off: no notification published") {
      val (eventPublishService, maps) = publishHarness()
      val handler = SubscriptionOrderNotificationHandler(NotificationEventPublisher(eventPublishService), config(false))

      handler.handle(
        businessEvent(
          mapOf("orderId" to 1001L, "orderNo" to "ORD-SUB-1", "timestamp" to eventTime),
        ),
      )

      maps.size shouldBe 0
    }

    test("never publishes to USER receiver (非提醒用户)") {
      val (eventPublishService, maps) = publishHarness()
      val handler = SubscriptionOrderNotificationHandler(NotificationEventPublisher(eventPublishService), config(true))

      handler.handle(
        businessEvent(
          mapOf("orderId" to 1001L, "orderNo" to "ORD-SUB-1", "userId" to 7L, "timestamp" to eventTime),
        ),
      )

      val events = maps.map { objectMapper.convertValue(it, NotificationEventData::class.java) }
      events.none { it.receiverType == "USER" } shouldBe true
    }
  })
