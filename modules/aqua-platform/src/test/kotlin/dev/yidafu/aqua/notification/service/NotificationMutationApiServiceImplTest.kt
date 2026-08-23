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

package dev.yidafu.aqua.notification.service

import dev.yidafu.aqua.common.domain.model.NotificationModel
import dev.yidafu.aqua.common.domain.model.enums.NotificationChannelStatus
import dev.yidafu.aqua.common.domain.model.enums.NotificationMessageType
import dev.yidafu.aqua.common.domain.model.enums.NotificationReceiverType
import dev.yidafu.aqua.notification.channel.wechat.WeChatSubscribeMessageService
import dev.yidafu.aqua.notification.domain.repository.NotificationRepository
import dev.yidafu.aqua.notification.domain.repository.UserSubscriptionAuthRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.Optional

/**
 * 通知写操作服务测试(PRD 13-notification.md F-NTF-006 失败消息手动重发):
 * 重发门控 —— 仅 USER + 已持久化模板场景 + 通道状态 FAILED/PENDING 可重发;
 * 重发重置 PENDING 后复用微信订阅消息发送,返回更新后记录。
 */
class NotificationMutationApiServiceImplTest :
  FunSpec({
    var notificationRepository: NotificationRepository = mockk()
    var userSubscriptionAuthRepository: UserSubscriptionAuthRepository = mockk()
    var weChatSubscribeMessageService: WeChatSubscribeMessageService = mockk()
    var service: NotificationMutationApiServiceImpl = mockk()

    fun notification(
      id: Long = 1L,
      receiverType: NotificationReceiverType = NotificationReceiverType.USER,
      scene: String? = "payment_result",
      channelStatus: NotificationChannelStatus = NotificationChannelStatus.FAILED,
    ): NotificationModel =
      NotificationModel(
        id = id,
        receiverType = receiverType,
        userId = if (receiverType == NotificationReceiverType.USER) 7L else null,
        workerId = if (receiverType == NotificationReceiverType.WORKER) 3L else null,
        messageType = NotificationMessageType.ORDER,
        scene = "ORDER_PAID",
        title = "支付成功",
        relatedNo = "1001",
        idempotencyKey = "1001:ORDER_PAID:1715000000000",
        templateScene = scene,
        templatePage = "pages/order-detail/index?id=1001",
        templateFields = """{"thing1":"订单号:1001"}""",
        channelStatus = channelStatus,
      )

    beforeTest {
      notificationRepository = mockk(relaxed = true)
      userSubscriptionAuthRepository = mockk(relaxed = true)
      weChatSubscribeMessageService = mockk(relaxed = true)
      service =
        NotificationMutationApiServiceImpl(
          notificationRepository = notificationRepository,
          userSubscriptionAuthRepository = userSubscriptionAuthRepository,
          weChatSubscribeMessageService = weChatSubscribeMessageService,
        )
    }

    test("FAILED 用户消息重发:重置 PENDING 并复用微信发送,返回更新后记录") {
      val notif = notification()
      every { notificationRepository.findById(1L) } returns Optional.of(notif)
      every { notificationRepository.save(any()) } returns notif

      val result = service.resendNotification(1L)

      result shouldBe notif
      notif.channelStatus shouldBe NotificationChannelStatus.PENDING
      verify { weChatSubscribeMessageService.trySend(notif, "payment_result", "pages/order-detail/index?id=1001", null) }
    }

    test("SENT 消息不可重发 → null") {
      val notif = notification(channelStatus = NotificationChannelStatus.SENT)
      every { notificationRepository.findById(1L) } returns Optional.of(notif)

      service.resendNotification(1L).shouldBeNull()

      verify(exactly = 0) { weChatSubscribeMessageService.trySend(any(), any(), any(), any()) }
    }

    test("送水工消息不可重发 → null") {
      val notif = notification(receiverType = NotificationReceiverType.WORKER)
      every { notificationRepository.findById(1L) } returns Optional.of(notif)

      service.resendNotification(1L).shouldBeNull()

      verify(exactly = 0) { weChatSubscribeMessageService.trySend(any(), any(), any(), any()) }
    }

    test("无模板场景的消息不可重发 → null") {
      val notif = notification(scene = null)
      every { notificationRepository.findById(1L) } returns Optional.of(notif)

      service.resendNotification(1L).shouldBeNull()

      verify(exactly = 0) { weChatSubscribeMessageService.trySend(any(), any(), any(), any()) }
    }

    test("消息不存在 → null") {
      every { notificationRepository.findById(1L) } returns Optional.empty()

      service.resendNotification(1L).shouldBeNull()
    }
  })
