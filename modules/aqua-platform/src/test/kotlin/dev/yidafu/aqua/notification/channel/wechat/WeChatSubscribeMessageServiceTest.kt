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

package dev.yidafu.aqua.notification.channel.wechat

import dev.yidafu.aqua.api.service.ops.SystemConfigQueryApiService
import dev.yidafu.aqua.api.service.user.UserQueryApiService
import dev.yidafu.aqua.common.domain.model.NotificationModel
import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.domain.model.UserNotificationSettingsModel
import dev.yidafu.aqua.common.domain.model.UserSubscriptionAuthModel
import dev.yidafu.aqua.common.domain.model.enums.NotificationChannelStatus
import dev.yidafu.aqua.common.domain.model.enums.NotificationMessageType
import dev.yidafu.aqua.common.domain.model.enums.NotificationReceiverType
import dev.yidafu.aqua.notice.config.WeChatProperties
import dev.yidafu.aqua.notice.domain.repository.UserNotificationSettingsRepository
import dev.yidafu.aqua.notification.domain.repository.NotificationRepository
import dev.yidafu.aqua.notification.domain.repository.UserSubscriptionAuthRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.web.client.RestTemplate
import java.util.*

/**
 * 微信订阅消息发送门控测试(PRD 13-notification.md R-NTF-01/F-NTF-001):
 * 模板配置/启用、用户分类开关、授权剩余次数、真实 openid 缺一不可;发送成功消耗授权。
 */
class WeChatSubscribeMessageServiceTest :
  FunSpec({
    var systemConfigQueryApiService: SystemConfigQueryApiService = mockk()
    var settingsRepository: UserNotificationSettingsRepository = mockk()
    var authRepository: UserSubscriptionAuthRepository = mockk()
    var userQueryApiService: UserQueryApiService = mockk()
    var notificationRepository: NotificationRepository = mockk()
    var restTemplate: RestTemplate = mockk()
    var accessTokenService: WeChatAccessTokenService = mockk()
    var service: WeChatSubscribeMessageService = mockk()

    fun notification(
      userId: Long = 1L,
      scene: String = "ORDER_PAID",
      id: Long = 1L,
    ): NotificationModel =
      NotificationModel(
        id = id,
        receiverType = NotificationReceiverType.USER,
        userId = userId,
        messageType = NotificationMessageType.ORDER,
        scene = scene,
        title = "支付成功",
        relatedNo = "1001",
        idempotencyKey = "1001:$scene:1715000000000",
      )

    fun authorizedAuth(
      templateId: String = "tpl_payment",
      remaining: Int = 1,
    ): UserSubscriptionAuthModel = UserSubscriptionAuthModel(userId = 1L, templateId = templateId, remainingCount = remaining)

    beforeTest {
      systemConfigQueryApiService = mockk(relaxed = true)
      settingsRepository = mockk(relaxed = true)
      authRepository = mockk(relaxed = true)
      userQueryApiService = mockk(relaxed = true)
      notificationRepository = mockk(relaxed = true)
      restTemplate = mockk(relaxed = true)
      accessTokenService = mockk(relaxed = true)
      service =
        WeChatSubscribeMessageService(
          accessTokenService = accessTokenService,
          systemConfigQueryApiService = systemConfigQueryApiService,
          userNotificationSettingsRepository = settingsRepository,
          userSubscriptionAuthRepository = authRepository,
          userQueryApiService = userQueryApiService,
          notificationRepository = notificationRepository,
          restTemplate = restTemplate,
          weChatProperties = WeChatProperties(appId = "appid", appSecret = "secret"),
        )
    }

    fun gateAllOpen(
      templateId: String = "tpl_payment",
      auth: UserSubscriptionAuthModel = authorizedAuth(templateId),
    ) {
      every { systemConfigQueryApiService.getStringValue("notification_template_payment_result_id", null) } returns templateId
      every { systemConfigQueryApiService.getBooleanValue("notification_template_payment_result_enabled", false) } returns true
      every { settingsRepository.findByUserId(1L) } returns Optional.of(UserNotificationSettingsModel(userId = 1L))
      every { authRepository.findByUserIdAndTemplateId(1L, templateId) } returns Optional.of(auth)
      every { userQueryApiService.findById(1L) } returns UserModel(id = 1L, wechatOpenId = "openid_1")
    }

    test("未配置模板 → 不发送") {
      every { systemConfigQueryApiService.getStringValue("notification_template_payment_result_id", null) } returns null
      service.trySend(notification(), "payment_result", null, null)
      verify(exactly = 0) { restTemplate.postForObject(any(), any(), WeChatSubscribeSendResponse::class.java, *anyVararg()) }
    }

    test("模板未启用 → 不发送") {
      every { systemConfigQueryApiService.getStringValue("notification_template_payment_result_id", null) } returns "tpl_payment"
      every { systemConfigQueryApiService.getBooleanValue("notification_template_payment_result_enabled", false) } returns false
      service.trySend(notification(), "payment_result", null, null)
      verify(exactly = 0) { restTemplate.postForObject(any(), any(), WeChatSubscribeSendResponse::class.java, *anyVararg()) }
    }

    test("用户分类开关关闭 → 不发送") {
      every { systemConfigQueryApiService.getStringValue("notification_template_payment_result_id", null) } returns "tpl_payment"
      every { systemConfigQueryApiService.getBooleanValue("notification_template_payment_result_enabled", false) } returns true
      every { settingsRepository.findByUserId(1L) } returns
        Optional.of(UserNotificationSettingsModel(userId = 1L, paymentNotifications = false))
      service.trySend(notification(), "payment_result", null, null)
      verify(exactly = 0) { restTemplate.postForObject(any(), any(), WeChatSubscribeSendResponse::class.java, *anyVararg()) }
    }

    test("未授权模板 → 不发送") {
      gateAllOpen()
      every { authRepository.findByUserIdAndTemplateId(1L, "tpl_payment") } returns Optional.empty()
      service.trySend(notification(), "payment_result", null, null)
      verify(exactly = 0) { restTemplate.postForObject(any(), any(), WeChatSubscribeSendResponse::class.java, *anyVararg()) }
    }

    test("授权次数耗尽 → 不发送") {
      gateAllOpen()
      every { authRepository.findByUserIdAndTemplateId(1L, "tpl_payment") } returns
        Optional.of(authorizedAuth(remaining = 0))
      service.trySend(notification(), "payment_result", null, null)
      verify(exactly = 0) { restTemplate.postForObject(any(), any(), WeChatSubscribeSendResponse::class.java, *anyVararg()) }
    }

    test("非真实 openid(manual_ 占位)→ 不发送") {
      gateAllOpen()
      every { userQueryApiService.findById(1L) } returns UserModel(id = 1L, wechatOpenId = "manual_123")
      service.trySend(notification(), "payment_result", null, null)
      verify(exactly = 0) { restTemplate.postForObject(any(), any(), WeChatSubscribeSendResponse::class.java, *anyVararg()) }
    }

    test("发送成功 → 消耗授权并回写 SENT") {
      val auth = authorizedAuth()
      gateAllOpen(auth = auth)
      every { accessTokenService.getAccessToken() } returns "access_token"
      every {
        restTemplate.postForObject(any(), any(), WeChatSubscribeSendResponse::class.java, *anyVararg())
      } returns WeChatSubscribeSendResponse(errcode = 0, errmsg = "ok", msgid = 123456L)
      val notif = notification()
      every { notificationRepository.save(any()) } returns notif
      every { authRepository.save(any()) } returns auth

      service.trySend(notif, "payment_result", null, null)

      verify { restTemplate.postForObject(any(), any(), WeChatSubscribeSendResponse::class.java, *anyVararg()) }
      auth.remainingCount shouldBe 0
      notif.channelStatus shouldBe NotificationChannelStatus.SENT
      notif.wxMessageId shouldBe "123456"
      notif.errorMessage shouldBe null
      verify { notificationRepository.save(notif) }
    }

    test("微信返回失败 → 回写 FAILED 且不消耗授权") {
      val auth = authorizedAuth()
      gateAllOpen(auth = auth)
      every { accessTokenService.getAccessToken() } returns "access_token"
      every {
        restTemplate.postForObject(any(), any(), WeChatSubscribeSendResponse::class.java, *anyVararg())
      } returns WeChatSubscribeSendResponse(errcode = 43101, errmsg = "user refuse")
      val notif = notification()
      every { notificationRepository.save(any()) } returns notif

      service.trySend(notif, "payment_result", null, null)

      auth.remainingCount shouldBe 1
      notif.channelStatus shouldBe NotificationChannelStatus.FAILED
    }
  })
