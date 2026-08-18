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

import dev.yidafu.aqua.common.domain.model.NotificationModel
import dev.yidafu.aqua.common.domain.model.enums.NotificationReceiverType
import dev.yidafu.aqua.common.messaging.event.DomainEvent
import dev.yidafu.aqua.notification.channel.wechat.WeChatSubscribeMessageService
import dev.yidafu.aqua.notification.channel.ws.NotificationPushEventPublisher
import dev.yidafu.aqua.notification.domain.repository.NotificationRepository
import dev.yidafu.aqua.notification.dto.NotificationEventData
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import tools.jackson.module.kotlin.jacksonObjectMapper

/**
 * 通知网关测试(PRD 13-notification.md R-NTF-02/03):
 * 幂等落库(重复事件跳过)、定向分发(WebSocket 推送事件 + 微信)、广播路径(仅推送不落库)。
 */
class NotificationGatewayServiceTest :
  FunSpec({
    val objectMapper = jacksonObjectMapper()

    var repository: NotificationRepository = mockk()
    var pushEventPublisher: NotificationPushEventPublisher = mockk()
    var weChatSubscribeMessageService: WeChatSubscribeMessageService = mockk()
    var gateway: NotificationGatewayService = mockk()

    // 同步执行器:使 afterCommit(无事务时立即执行)在测试线程内完成,便于 verify
    val syncExecutor =
      org.springframework.core.task
        .TaskExecutor { task: Runnable -> task.run() }

    beforeTest {
      repository = mockk(relaxed = true)
      pushEventPublisher = mockk(relaxed = true)
      weChatSubscribeMessageService = mockk(relaxed = true)
      gateway =
        NotificationGatewayService(
          notificationRepository = repository,
          notificationPushEventPublisher = pushEventPublisher,
          weChatSubscribeMessageService = weChatSubscribeMessageService,
          dispatchExecutor = syncExecutor,
        )
    }

    fun event(data: NotificationEventData): DomainEvent =
      DomainEvent(
        eventType = "NOTIFICATION_SEND",
        payload =
          objectMapper.writeValueAsString(
            objectMapper.convertValue(data, Map::class.java),
          ),
      )

    test("新用户通知事件应落库并发布推送事件,无模板场景不调微信") {
      val data =
        NotificationEventData(
          receiverType = "USER",
          userId = 1L,
          messageType = "ORDER",
          scene = "ORDER_PAID",
          title = "支付成功",
          summary = "订单已支付",
          relatedNo = "1001",
          eventTime = 1715000000000L,
        )
      every { repository.existsByIdempotencyKey(any()) } returns false
      every { repository.countUnread(any(), any()) } returns 3L
      val saved =
        NotificationModel(
          id = 1L,
          receiverType = NotificationReceiverType.USER,
          userId = 1L,
          messageType = dev.yidafu.aqua.common.domain.model.enums.NotificationMessageType.ORDER,
          scene = "ORDER_PAID",
          title = "支付成功",
          summary = "订单已支付",
          relatedNo = "1001",
          idempotencyKey = data.idempotencyKey,
        )
      every { repository.saveAndFlush(any()) } returns saved

      gateway.handle(event(data))

      verify { repository.saveAndFlush(any()) }
      verify {
        pushEventPublisher.publish(
          match { it.receiverType == "USER" && it.receiverId == 1L && !it.broadcast },
        )
      }
      verify(exactly = 0) { weChatSubscribeMessageService.trySend(any(), any(), any(), any()) }
    }

    test("重复幂等键应跳过落库与分发(R-NTF-02)") {
      val data =
        NotificationEventData(
          receiverType = "USER",
          userId = 1L,
          messageType = "ORDER",
          scene = "ORDER_PAID",
          title = "支付成功",
          relatedNo = "1001",
          eventTime = 1715000000000L,
        )
      every { repository.existsByIdempotencyKey(data.idempotencyKey) } returns true

      gateway.handle(event(data))

      verify(exactly = 0) { repository.saveAndFlush(any()) }
      verify(exactly = 0) { pushEventPublisher.publish(any()) }
    }

    test("带模板场景的用户事件应触发微信订阅消息发送") {
      val data =
        NotificationEventData(
          receiverType = "USER",
          userId = 1L,
          messageType = "ORDER",
          scene = "ORDER_PAID",
          title = "支付成功",
          relatedNo = "1001",
          eventTime = 1715000000000L,
          templateScene = "payment_result",
          page = "pages/order-detail/index?id=1001",
          templateFields = mapOf("thing1" to "订单号:1001"),
        )
      every { repository.existsByIdempotencyKey(any()) } returns false
      every { repository.countUnread(any(), any()) } returns 1L
      val saved =
        NotificationModel(
          id = 2L,
          receiverType = NotificationReceiverType.USER,
          userId = 1L,
          messageType = dev.yidafu.aqua.common.domain.model.enums.NotificationMessageType.ORDER,
          scene = "ORDER_PAID",
          title = "支付成功",
          relatedNo = "1001",
          idempotencyKey = data.idempotencyKey,
        )
      every { repository.saveAndFlush(any()) } returns saved

      gateway.handle(event(data))

      verify { pushEventPublisher.publish(match { it.receiverType == "USER" && it.receiverId == 1L }) }
      verify {
        weChatSubscribeMessageService.trySend(
          saved,
          "payment_result",
          "pages/order-detail/index?id=1001",
          mapOf(
            "thing1" to "订单号:1001",
          ),
        )
      }
    }

    test("广播事件仅推送广播,不落库") {
      val data =
        NotificationEventData(
          receiverType = "WORKER",
          workerId = null,
          messageType = "ORDER",
          scene = "NEW_ORDER",
          title = "新订单待接单",
          relatedNo = "1002",
          eventTime = 1715000000000L,
          broadcast = true,
        )

      gateway.handle(event(data))

      verify(exactly = 0) { repository.saveAndFlush(any()) }
      verify { pushEventPublisher.publish(match { it.broadcast }) }
    }

    test("缺少接收人 id 的通知事件应跳过") {
      val data =
        NotificationEventData(
          receiverType = "WORKER",
          workerId = null,
          messageType = "ORDER",
          scene = "DELIVERY_ASSIGNED",
          title = "新派单",
          relatedNo = "1003",
          eventTime = 1715000000000L,
        )
      gateway.handle(event(data))
      verify(exactly = 0) { repository.saveAndFlush(any()) }
      verify(exactly = 0) { pushEventPublisher.publish(any()) }
    }

    test("幂等键格式应为 关联单号:场景:事件时间") {
      val data =
        NotificationEventData(
          receiverType = "USER",
          userId = 1L,
          messageType = "ORDER",
          scene = "ORDER_PAID",
          title = "支付成功",
          relatedNo = "1001",
          eventTime = 1715000000000L,
        )
      data.idempotencyKey shouldBe "1001:ORDER_PAID:1715000000000"
    }
  })
