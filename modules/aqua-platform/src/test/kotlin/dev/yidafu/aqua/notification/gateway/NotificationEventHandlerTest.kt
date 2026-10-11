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
import dev.yidafu.aqua.api.service.order.OrderQueryApiService
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.messaging.event.DomainEvent
import dev.yidafu.aqua.common.messaging.service.SimplifiedEventPublishService
import dev.yidafu.aqua.notification.dto.NotificationEventData
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 各场景通知事件处理器测试(PRD 13-notification.md F-NTF-001/002/003):
 * 业务事件(支付成功/配送进度/支付超时/下单成功)→ 组装 NOTIFICATION_SEND 事件载荷,
 * 校验接收人/消息类型/标题/关联单号/微信模板场景与字段/送水工推送开关。
 */
class NotificationEventHandlerTest :
  FunSpec({
    val objectMapper = jacksonObjectMapper()

    fun expectedTime(epochMillis: Long): String =
      DateTimeFormatter
        .ofPattern("yyyy-MM-dd HH:mm")
        .format(LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault()))

    fun publisher(eventPublishService: SimplifiedEventPublishService): NotificationEventPublisher =
      NotificationEventPublisher(eventPublishService)

    fun businessEvent(
      eventType: String,
      data: Map<String, Any>,
    ): DomainEvent =
      DomainEvent(
        eventType = eventType,
        aggregateId = data["orderId"]?.toString() ?: "-",
        payload = objectMapper.writeValueAsString(data),
      )

    /** 预配置发布捕获的桩(须在处理器 handle 前创建),返回 (service, 已发布载荷列表) */
    fun publishHarness(): Pair<SimplifiedEventPublishService, MutableList<Map<String, Any>>> {
      val maps = mutableListOf<Map<String, Any>>()
      val service = mockk<SimplifiedEventPublishService>(relaxed = true)
      every { service.publishDomainEvent(any(), any(), capture(maps)) } returns true
      return service to maps
    }

    fun List<Map<String, Any>>.asEvents(): List<NotificationEventData> =
      map { objectMapper.convertValue(it, NotificationEventData::class.java) }

    val eventTime = 1715000000000L

    context("OrderPaidNotificationHandler(支付成功)") {
      test("开关开启:用户「支付成功」+ 送水工「新订单」广播") {
        val (eventPublishService, maps) = publishHarness()
        val systemConfig = mockk<SystemConfigQueryApiService>(relaxed = true)
        every { systemConfig.getBooleanValue("delivery_push_new_order_enabled", true) } returns true
        val handler = OrderPaidNotificationHandler(publisher(eventPublishService), systemConfig)

        handler.handle(
          businessEvent(
            "ORDER_PAID",
            mapOf("orderId" to 1001L, "userId" to 7L, "amountCents" to 2500L, "timestamp" to eventTime),
          ),
        )

        val events = maps.asEvents()
        events.size shouldBe 2
        val user = events.first { it.receiverType == "USER" }
        user.userId shouldBe 7L
        user.messageType shouldBe "ORDER"
        user.scene shouldBe "ORDER_PAID"
        user.title shouldBe "支付成功"
        user.relatedNo shouldBe "1001"
        user.templateScene shouldBe "payment_result"
        user.page shouldBe "pages/order-detail/index?id=1001"
        user.templateFields shouldBe mapOf("thing1" to "订单号:1001", "amount2" to "¥25.00", "time3" to expectedTime(eventTime))
        val worker = events.first { it.receiverType == "WORKER" }
        worker.broadcast shouldBe true
        worker.scene shouldBe "NEW_ORDER"
        worker.workerId shouldBe null
      }

      test("开关关闭:仅用户「支付成功」,不广播送水工") {
        val (eventPublishService, maps) = publishHarness()
        val systemConfig = mockk<SystemConfigQueryApiService>(relaxed = true)
        every { systemConfig.getBooleanValue("delivery_push_new_order_enabled", true) } returns false
        val handler = OrderPaidNotificationHandler(publisher(eventPublishService), systemConfig)

        handler.handle(
          businessEvent(
            "ORDER_PAID",
            mapOf("orderId" to 1001L, "userId" to 7L, "amountCents" to 2500L, "timestamp" to eventTime),
          ),
        )

        val events = maps.asEvents()
        events.size shouldBe 1
        events[0].receiverType shouldBe "USER"
      }
    }

    context("DeliveryAssignedNotificationHandler(配送员接单)") {
      test("用户「配送员接单」+ 送水工「新派单」(开关开启)") {
        val (eventPublishService, maps) = publishHarness()
        val systemConfig = mockk<SystemConfigQueryApiService>(relaxed = true)
        val orderQuery = mockk<OrderQueryApiService>(relaxed = true)
        every { systemConfig.getBooleanValue("delivery_push_assigned_enabled", true) } returns true
        every { orderQuery.findById(1001L) } returns OrderModel(id = 1001L, orderNo = "NO1001", userId = 7L, deliveryWorkerId = 3L)
        val handler = DeliveryAssignedNotificationHandler(publisher(eventPublishService), systemConfig, orderQuery)

        handler.handle(
          businessEvent(
            "DELIVERY_ASSIGNED",
            mapOf("orderId" to 1001L, "userId" to 7L, "deliveryWorkerId" to 3L, "timestamp" to eventTime),
          ),
        )

        val events = maps.asEvents()
        events.size shouldBe 2
        val user = events.first { it.receiverType == "USER" }
        user.title shouldBe "配送员接单"
        user.templateScene shouldBe "delivery_assigned"
        user.relatedNo shouldBe "1001"
        val worker = events.first { it.receiverType == "WORKER" }
        worker.workerId shouldBe 3L
        worker.title shouldBe "新派单"
        worker.relatedNo shouldBe "NO1001"
      }
    }

    context("DeliveryStartedNotificationHandler(开始配送)") {
      test("用户「开始配送」+ 送水工「开始配送」") {
        val (eventPublishService, maps) = publishHarness()
        val systemConfig = mockk<SystemConfigQueryApiService>(relaxed = true)
        val orderQuery = mockk<OrderQueryApiService>(relaxed = true)
        every { systemConfig.getBooleanValue("delivery_push_assigned_enabled", true) } returns true
        every { orderQuery.findById(1001L) } returns OrderModel(id = 1001L, orderNo = "NO1001", userId = 7L, deliveryWorkerId = 3L)
        val handler = DeliveryStartedNotificationHandler(publisher(eventPublishService), orderQuery, systemConfig)

        handler.handle(
          businessEvent(
            "DELIVERY_STARTED",
            mapOf("orderId" to 1001L, "deliveryWorkerId" to 3L, "timestamp" to eventTime),
          ),
        )

        val events = maps.asEvents()
        events.size shouldBe 2
        val user = events.first { it.receiverType == "USER" }
        user.title shouldBe "开始配送"
        user.templateScene shouldBe "delivery_started"
        user.userId shouldBe 7L
        val worker = events.first { it.receiverType == "WORKER" }
        worker.workerId shouldBe 3L
        worker.title shouldBe "开始配送"
      }
    }

    context("DeliveryCompletedNotificationHandler(配送完成/送达确认)") {
      test("用户「送达确认」+ 送水工「配送完成」") {
        val (eventPublishService, maps) = publishHarness()
        val systemConfig = mockk<SystemConfigQueryApiService>(relaxed = true)
        val orderQuery = mockk<OrderQueryApiService>(relaxed = true)
        every { systemConfig.getBooleanValue("delivery_push_assigned_enabled", true) } returns true
        every { orderQuery.findById(1001L) } returns OrderModel(id = 1001L, orderNo = "NO1001", userId = 7L, deliveryWorkerId = 3L)
        val handler = DeliveryCompletedNotificationHandler(publisher(eventPublishService), orderQuery, systemConfig)

        handler.handle(
          businessEvent(
            "DELIVERY_COMPLETED",
            mapOf("orderId" to 1001L, "deliveryWorkerId" to 3L, "timestamp" to eventTime),
          ),
        )

        val events = maps.asEvents()
        events.size shouldBe 2
        val user = events.first { it.receiverType == "USER" }
        user.title shouldBe "送达确认"
        user.templateScene shouldBe "delivery_completed"
        val worker = events.first { it.receiverType == "WORKER" }
        worker.workerId shouldBe 3L
        worker.title shouldBe "配送完成"
      }
    }

    context("PaymentTimeoutNotificationHandler(支付超时)") {
      test("用户「支付超时」:复用支付结果模板场景") {
        val (eventPublishService, maps) = publishHarness()
        val orderQuery = mockk<OrderQueryApiService>(relaxed = true)
        val systemConfig = mockk<SystemConfigQueryApiService>(relaxed = true)
        every { orderQuery.findById(1001L) } returns OrderModel(id = 1001L, orderNo = "NO1001", userId = 7L)
        val handler = PaymentTimeoutNotificationHandler(publisher(eventPublishService), orderQuery, systemConfig)

        handler.handle(
          businessEvent(
            "PAYMENT_TIMEOUT",
            mapOf("orderId" to 1001L, "userId" to 7L, "timestamp" to eventTime),
          ),
        )

        val events = maps.asEvents()
        events.size shouldBe 1
        val user = events[0]
        user.receiverType shouldBe "USER"
        user.userId shouldBe 7L
        user.title shouldBe "支付超时"
        user.scene shouldBe "PAYMENT_TIMEOUT"
        user.relatedNo shouldBe "1001"
        user.templateScene shouldBe "payment_result"
        user.page shouldBe "pages/order-detail/index?id=1001"
        user.templateFields shouldBe mapOf("thing1" to "订单号:1001", "time3" to expectedTime(eventTime))
      }

      test("订单已分配送水工时追加送水工「支付超时」提醒(开关开启)") {
        val (eventPublishService, maps) = publishHarness()
        val orderQuery = mockk<OrderQueryApiService>(relaxed = true)
        val systemConfig = mockk<SystemConfigQueryApiService>(relaxed = true)
        every { systemConfig.getBooleanValue("delivery_push_payment_timeout_enabled", true) } returns true
        every { orderQuery.findById(1001L) } returns OrderModel(id = 1001L, orderNo = "NO1001", userId = 7L, deliveryWorkerId = 3L)
        val handler = PaymentTimeoutNotificationHandler(publisher(eventPublishService), orderQuery, systemConfig)

        handler.handle(
          businessEvent(
            "PAYMENT_TIMEOUT",
            mapOf("orderId" to 1001L, "userId" to 7L, "timestamp" to eventTime),
          ),
        )

        val events = maps.asEvents()
        events.size shouldBe 2
        val worker = events.first { it.receiverType == "WORKER" }
        worker.workerId shouldBe 3L
        worker.title shouldBe "支付超时"
        worker.relatedNo shouldBe "NO1001"
      }
    }

    context("OrderCancelledNotificationHandler(订单取消)") {
      test("用户「订单已取消」+ 已分配送水工收到「订单已取消」") {
        val (eventPublishService, maps) = publishHarness()
        val orderQuery = mockk<OrderQueryApiService>(relaxed = true)
        val systemConfig = mockk<SystemConfigQueryApiService>(relaxed = true)
        every { systemConfig.getBooleanValue("delivery_push_cancelled_enabled", true) } returns true
        every { orderQuery.findById(1001L) } returns OrderModel(id = 1001L, orderNo = "NO1001", userId = 7L, deliveryWorkerId = 3L)
        val handler = OrderCancelledNotificationHandler(publisher(eventPublishService), orderQuery, systemConfig)

        handler.handle(
          businessEvent(
            "ORDER_CANCELLED",
            mapOf("orderId" to 1001L, "userId" to 7L, "reason" to "用户取消", "shouldRefund" to true, "timestamp" to eventTime),
          ),
        )

        val events = maps.asEvents()
        events.size shouldBe 2
        val user = events.first { it.receiverType == "USER" }
        user.title shouldBe "订单已取消"
        user.templateScene shouldBe "payment_result"
        val worker = events.first { it.receiverType == "WORKER" }
        worker.workerId shouldBe 3L
        worker.title shouldBe "订单已取消"
        worker.relatedNo shouldBe "NO1001"
      }

      test("取消开关关闭或订单未分配送水工:仅用户消息") {
        val (eventPublishService, maps) = publishHarness()
        val orderQuery = mockk<OrderQueryApiService>(relaxed = true)
        val systemConfig = mockk<SystemConfigQueryApiService>(relaxed = true)
        every { systemConfig.getBooleanValue("delivery_push_cancelled_enabled", true) } returns false
        every { orderQuery.findById(1001L) } returns OrderModel(id = 1001L, orderNo = "NO1001", userId = 7L)
        val handler = OrderCancelledNotificationHandler(publisher(eventPublishService), orderQuery, systemConfig)

        handler.handle(
          businessEvent(
            "ORDER_CANCELLED",
            mapOf("orderId" to 1001L, "userId" to 7L, "reason" to "用户取消", "timestamp" to eventTime),
          ),
        )

        val events = maps.asEvents()
        events.size shouldBe 1
        events[0].receiverType shouldBe "USER"
      }
    }

    context("OrderUrgingNotificationHandler(催单)") {
      test("用户「催单已受理」+ 送水工「用户催单」(开关开启)") {
        val (eventPublishService, maps) = publishHarness()
        val orderQuery = mockk<OrderQueryApiService>(relaxed = true)
        val systemConfig = mockk<SystemConfigQueryApiService>(relaxed = true)
        every { systemConfig.getBooleanValue("delivery_push_urging_enabled", true) } returns true
        every { orderQuery.findById(1001L) } returns OrderModel(id = 1001L, orderNo = "NO1001", userId = 7L, deliveryWorkerId = 3L)
        val handler = OrderUrgingNotificationHandler(publisher(eventPublishService), orderQuery, systemConfig)

        handler.handle(
          businessEvent(
            "ORDER_URGING",
            mapOf("orderId" to 1001L, "userId" to 7L, "deliveryWorkerId" to 3L, "timestamp" to eventTime),
          ),
        )

        val events = maps.asEvents()
        events.size shouldBe 2
        val user = events.first { it.receiverType == "USER" }
        user.title shouldBe "催单已受理"
        user.templateScene shouldBe "order_urging"
        user.relatedNo shouldBe "1001"
        val worker = events.first { it.receiverType == "WORKER" }
        worker.workerId shouldBe 3L
        worker.title shouldBe "用户催单"
        worker.relatedNo shouldBe "NO1001"
      }
    }

    context("DeliveryTimeoutNotificationHandler(配送超时)") {
      test("送水工「配送超时」(开关开启)") {
        val (eventPublishService, maps) = publishHarness()
        val orderQuery = mockk<OrderQueryApiService>(relaxed = true)
        val systemConfig = mockk<SystemConfigQueryApiService>(relaxed = true)
        every { systemConfig.getBooleanValue("delivery_push_timeout_enabled", true) } returns true
        every { orderQuery.findById(1001L) } returns OrderModel(id = 1001L, orderNo = "NO1001", userId = 7L, deliveryWorkerId = 3L)
        val handler = DeliveryTimeoutNotificationHandler(publisher(eventPublishService), orderQuery, systemConfig)

        handler.handle(
          businessEvent(
            "DELIVERY_TIMEOUT",
            mapOf("orderId" to 1001L, "deliveryWorkerId" to 3L, "eventTime" to eventTime, "timestamp" to eventTime),
          ),
        )

        val events = maps.asEvents()
        events.size shouldBe 1
        val worker = events[0]
        worker.receiverType shouldBe "WORKER"
        worker.workerId shouldBe 3L
        worker.title shouldBe "配送超时"
        worker.scene shouldBe "DELIVERY_TIMEOUT"
        worker.relatedNo shouldBe "NO1001"
        worker.eventTime shouldBe eventTime
      }
    }

    context("CouponExpiringNotificationHandler(优惠券即将过期)") {
      test("用户收到站内系统消息,幂等键取过期时间") {
        val (eventPublishService, maps) = publishHarness()
        val handler = CouponExpiringNotificationHandler(publisher(eventPublishService))

        handler.handle(
          businessEvent(
            "COUPON_EXPIRING",
            mapOf("userId" to 7L, "couponId" to 5001L, "couponName" to "满100减20", "expireTime" to eventTime),
          ),
        )

        val events = maps.asEvents()
        events.size shouldBe 1
        val user = events[0]
        user.receiverType shouldBe "USER"
        user.userId shouldBe 7L
        user.messageType shouldBe "SYSTEM"
        user.title shouldBe "优惠券即将过期"
        user.summary shouldBe "您的优惠券「满100减20」将于 ${expectedTime(eventTime)} 过期,请尽快使用。"
        user.relatedNo shouldBe "5001"
        user.scene shouldBe "COUPON_EXPIRING"
        user.eventTime shouldBe eventTime
        user.templateScene shouldBe null
      }
    }

    context("OrderAcceptTimeoutNotificationHandler(接单超时预警,G94/F-NTF-008)") {
      test("待派单超阈值 → 全体管理员站内预警,摘要含订单号/金额/超时时长") {
        val (eventPublishService, maps) = publishHarness()
        val orderQuery = mockk<OrderQueryApiService>(relaxed = true)
        every { orderQuery.findById(1001L) } returns
          OrderModel(
            id = 1001L,
            orderNo = "NO1001",
            userId = 7L,
            status = OrderModelStatus.PENDING_DISPATCH,
            amountCents = 2500L,
          )
        val handler = OrderAcceptTimeoutNotificationHandler(publisher(eventPublishService), orderQuery)

        handler.handle(
          businessEvent(
            "ORDER_ACCEPT_TIMEOUT",
            mapOf("orderId" to 1001L, "timeoutMinutes" to 30L, "eventTime" to eventTime, "timestamp" to eventTime),
          ),
        )

        val events = maps.asEvents()
        events.size shouldBe 1
        val alert = events[0]
        alert.receiverType shouldBe "ADMIN"
        alert.userId shouldBe null
        alert.workerId shouldBe null
        alert.messageType shouldBe "ORDER"
        alert.scene shouldBe "ORDER_ACCEPT_TIMEOUT"
        alert.title shouldBe "订单接单超时"
        alert.summary shouldBe "订单 NO1001(¥25.00)已待派单超 30 分钟无人接单,请及时处理。"
        alert.relatedNo shouldBe "NO1001"
        alert.eventTime shouldBe eventTime
        alert.templateScene shouldBe null
      }

      test("预警前订单被接单(状态已变化)→ 不预警(AC-NTF-008-04)") {
        val (eventPublishService, maps) = publishHarness()
        val orderQuery = mockk<OrderQueryApiService>(relaxed = true)
        every { orderQuery.findById(1001L) } returns
          OrderModel(id = 1001L, orderNo = "NO1001", status = OrderModelStatus.DELIVERING)
        val handler = OrderAcceptTimeoutNotificationHandler(publisher(eventPublishService), orderQuery)

        handler.handle(
          businessEvent("ORDER_ACCEPT_TIMEOUT", mapOf("orderId" to 1001L, "eventTime" to eventTime)),
        )

        maps.size shouldBe 0
      }

      test("订单已取消/不存在 → 不预警(AC-NTF-008-04)") {
        val (eventPublishService, maps) = publishHarness()
        val orderQuery = mockk<OrderQueryApiService>(relaxed = true)
        every { orderQuery.findById(1001L) } returns null
        val handler = OrderAcceptTimeoutNotificationHandler(publisher(eventPublishService), orderQuery)

        handler.handle(
          businessEvent("ORDER_ACCEPT_TIMEOUT", mapOf("orderId" to 1001L, "eventTime" to eventTime)),
        )

        maps.size shouldBe 0
      }
    }

    context("DeliveryStartTimeoutNotificationHandler(开始配送超时预警,G94/F-NTF-008)") {
      test("待配送超阈值 → 全体管理员站内预警") {
        val (eventPublishService, maps) = publishHarness()
        val orderQuery = mockk<OrderQueryApiService>(relaxed = true)
        every { orderQuery.findById(2002L) } returns
          OrderModel(
            id = 2002L,
            orderNo = "NO2002",
            userId = 7L,
            status = OrderModelStatus.PENDING_DELIVERY,
            amountCents = 3300L,
          )
        val handler = DeliveryStartTimeoutNotificationHandler(publisher(eventPublishService), orderQuery)

        handler.handle(
          businessEvent(
            "DELIVERY_START_TIMEOUT",
            mapOf("orderId" to 2002L, "timeoutMinutes" to 60L, "eventTime" to eventTime, "timestamp" to eventTime),
          ),
        )

        val events = maps.asEvents()
        events.size shouldBe 1
        val alert = events[0]
        alert.receiverType shouldBe "ADMIN"
        alert.messageType shouldBe "ORDER"
        alert.scene shouldBe "DELIVERY_START_TIMEOUT"
        alert.title shouldBe "开始配送超时"
        alert.summary shouldBe "订单 NO2002(¥33.00)派单后超 60 分钟未开始配送,请及时处理。"
        alert.relatedNo shouldBe "NO2002"
        alert.eventTime shouldBe eventTime
      }

      test("预警前订单已开始配送(状态已变化)→ 不预警(AC-NTF-008-04)") {
        val (eventPublishService, maps) = publishHarness()
        val orderQuery = mockk<OrderQueryApiService>(relaxed = true)
        every { orderQuery.findById(2002L) } returns
          OrderModel(id = 2002L, orderNo = "NO2002", status = OrderModelStatus.DELIVERING)
        val handler = DeliveryStartTimeoutNotificationHandler(publisher(eventPublishService), orderQuery)

        handler.handle(
          businessEvent("DELIVERY_START_TIMEOUT", mapOf("orderId" to 2002L, "eventTime" to eventTime)),
        )

        maps.size shouldBe 0
      }
    }

    context("OrderCreatedNotificationHandler(下单成功)") {
      test("用户「下单成功」:模板场景 order_created") {
        val (eventPublishService, maps) = publishHarness()
        val handler = OrderCreatedNotificationHandler(publisher(eventPublishService))

        handler.handle(
          businessEvent(
            "ORDER_CREATED",
            mapOf("orderId" to 1001L, "userId" to 7L, "amountCents" to 2500L, "timestamp" to eventTime),
          ),
        )

        val events = maps.asEvents()
        events.size shouldBe 1
        val user = events[0]
        user.receiverType shouldBe "USER"
        user.title shouldBe "下单成功"
        user.templateScene shouldBe "order_created"
        user.relatedNo shouldBe "1001"
      }
    }
  })
