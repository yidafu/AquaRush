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

import com.fasterxml.jackson.annotation.JsonProperty
import dev.yidafu.aqua.api.service.ops.SystemConfigQueryApiService
import dev.yidafu.aqua.api.service.user.UserQueryApiService
import dev.yidafu.aqua.common.domain.model.NotificationModel
import dev.yidafu.aqua.common.domain.model.UserNotificationSettingsModel
import dev.yidafu.aqua.common.domain.model.enums.NotificationChannelStatus
import dev.yidafu.aqua.common.domain.model.enums.NotificationReceiverType
import dev.yidafu.aqua.notice.config.WeChatProperties
import dev.yidafu.aqua.notice.domain.repository.UserNotificationSettingsRepository
import dev.yidafu.aqua.notification.domain.repository.NotificationRepository
import dev.yidafu.aqua.notification.domain.repository.UserSubscriptionAuthRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import tools.jackson.core.type.TypeReference
import tools.jackson.module.kotlin.jacksonObjectMapper

/**
 * 微信订阅消息发送服务(PRD 13-notification.md F-NTF-001)
 *
 * POST /cgi-bin/message/subscribe/send,发送门控(R-NTF-01):
 * 1) 接收人为 USER 且有 userId;
 * 2) 模板已配置且启用(SystemSettingKeys.NOTIFICATION_TEMPLATE_*_ID/_ENABLED,F-NTF-005);
 * 3) 用户分类订阅开关(user_notification_settings,order_updates/payment/delivery);
 * 4) 用户已授权该模板且有剩余次数(user_subscription_auths,一次性模板授权一次发一次);
 * 5) 真实 openid(非 manual_ 占位)。
 *
 * 发送结果回写 notifications.channel_status/wx_message_id/error_message;
 * 仅发送成功时消耗授权次数;失败不消耗,消息中心仍可见(降级站内查看)。
 */
@Service
class WeChatSubscribeMessageService(
  private val accessTokenService: WeChatAccessTokenService,
  private val systemConfigQueryApiService: SystemConfigQueryApiService,
  private val userNotificationSettingsRepository: UserNotificationSettingsRepository,
  private val userSubscriptionAuthRepository: UserSubscriptionAuthRepository,
  private val userQueryApiService: UserQueryApiService,
  private val notificationRepository: NotificationRepository,
  private val restTemplate: RestTemplate,
  private val weChatProperties: WeChatProperties,
) {
  private val logger = LoggerFactory.getLogger(WeChatSubscribeMessageService::class.java)
  private val objectMapper = jacksonObjectMapper()

  /**
   * 尝试发送订阅消息;所有门控不满足时静默跳过(站内消息已可见)。
   * 模板场景/页面/字段优先取入参(网关首次发送);入参为空时回退持久化字段
   * (notifications.template_scene/template_page/template_fields,失败消息手动重发场景)。
   */
  fun trySend(
    notification: NotificationModel,
    templateScene: String?,
    page: String?,
    fields: Map<String, String>?,
  ) {
    try {
      if (notification.receiverType != NotificationReceiverType.USER) return
      val userId = notification.userId ?: return

      // 模板场景:入参为空时回退持久化字段(重发场景)
      val scene = templateScene ?: notification.templateScene ?: return

      // 门控 2:模板配置 + 启用
      val templateId =
        systemConfigQueryApiService.getStringValue(templateIdKey(scene), null)
          ?: return
      val enabled = systemConfigQueryApiService.getBooleanValue(templateEnabledKey(scene), false)
      if (!enabled) return

      // 门控 3:用户分类订阅开关(默认行缺失时按全部开启处理)
      val settings = userNotificationSettingsRepository.findByUserId(userId).orElse(null)
      if (settings != null && !categoryAllowed(settings, scene)) return

      // 门控 4:授权剩余次数
      val auth =
        userSubscriptionAuthRepository.findByUserIdAndTemplateId(userId, templateId).orElse(null)
          ?: return
      if (auth.remainingCount <= 0) return

      // 门控 5:真实 openid
      val openid = userQueryApiService.findById(userId)?.wechatOpenId ?: return
      if (openid.isBlank() || openid.startsWith(MANUAL_OPENID_PREFIX)) return

      val pagePath = page ?: notification.templatePage
      val fieldMap = fields ?: parseTemplateFields(notification.templateFields)
      sendAndRecord(notification, templateId, openid, pagePath, fieldMap, auth, scene)
    } catch (e: Exception) {
      logger.error("订阅消息发送失败, notificationId={}, scene={}", notification.id, templateScene, e)
      notification.recordChannelResult(
        NotificationChannelStatus.FAILED,
        errorMessage = e.message?.take(MAX_ERROR_LENGTH),
      )
      try {
        notificationRepository.save(notification)
      } catch (saveError: Exception) {
        logger.error("订阅消息失败状态回写失败, notificationId={}", notification.id, saveError)
      }
    }
  }

  private fun sendAndRecord(
    notification: NotificationModel,
    templateId: String,
    openid: String,
    page: String?,
    fields: Map<String, String>?,
    auth: dev.yidafu.aqua.common.domain.model.UserSubscriptionAuthModel,
    scene: String,
  ) {
    val token = accessTokenService.getAccessToken()
    val body =
      mapOf(
        "touser" to openid,
        "template_id" to templateId,
        "page" to page,
        "miniprogram_state" to "formal",
        "lang" to "zh_CN",
        "data" to (fields ?: emptyMap()).mapValues { (_, value) -> mapOf("value" to value) },
      )
    val response =
      restTemplate.postForObject(
        "${weChatProperties.apiUrl}/cgi-bin/message/subscribe/send?access_token=$token",
        body,
        WeChatSubscribeSendResponse::class.java,
      )

    if (response != null && response.errcode == 0) {
      auth.consume()
      userSubscriptionAuthRepository.save(auth)
      notification.recordChannelResult(
        NotificationChannelStatus.SENT,
        wxMessageId = response.msgid?.toString(),
      )
      logger.info("订阅消息发送成功, notificationId={}, templateId={}, msgid={}", notification.id, templateId, response.msgid)
    } else {
      val reason = "${response?.errcode}:${response?.errmsg ?: "未知错误"}"
      notification.recordChannelResult(
        NotificationChannelStatus.FAILED,
        errorMessage = reason.take(MAX_ERROR_LENGTH),
      )
      logger.warn("订阅消息发送失败(微信返回), notificationId={}, {}", notification.id, reason)
    }
    notificationRepository.save(notification)
  }

  /** 解析持久化的模板字段 JSON(失败消息重发还原用) */
  private fun parseTemplateFields(json: String?): Map<String, String>? {
    if (json.isNullOrBlank()) return null
    return try {
      objectMapper.readValue(json, object : TypeReference<Map<String, String>>() {})
    } catch (e: Exception) {
      logger.warn("解析持久化模板字段失败,按无字段处理: {}", e.message)
      null
    }
  }

  /** 模板场景 → 用户分类开关(门控 3) */
  private fun categoryAllowed(
    settings: UserNotificationSettingsModel,
    templateScene: String,
  ): Boolean =
    when (templateScene) {
      ORDER_CREATED_SCENE, ORDER_URGING_SCENE -> settings.orderUpdates
      DELIVERY_ASSIGNED_SCENE, DELIVERY_STARTED_SCENE, DELIVERY_COMPLETED_SCENE -> settings.deliveryNotifications
      PAYMENT_RESULT_SCENE -> settings.paymentNotifications
      else -> true
    }

  private fun templateIdKey(scene: String): String = "notification_template_${scene}_id"

  private fun templateEnabledKey(scene: String): String = "notification_template_${scene}_enabled"

  companion object {
    private const val MAX_ERROR_LENGTH = 500
    private const val MANUAL_OPENID_PREFIX = "manual_"
    private const val ORDER_CREATED_SCENE = "order_created"
    private const val ORDER_URGING_SCENE = "order_urging"
    private const val DELIVERY_ASSIGNED_SCENE = "delivery_assigned"
    private const val DELIVERY_STARTED_SCENE = "delivery_started"
    private const val DELIVERY_COMPLETED_SCENE = "delivery_completed"
    private const val PAYMENT_RESULT_SCENE = "payment_result"
  }
}

/** 微信订阅消息发送响应(errcode=0 成功) */
data class WeChatSubscribeSendResponse(
  @JsonProperty("errcode") val errcode: Int = -1,
  @JsonProperty("errmsg") val errmsg: String? = null,
  @JsonProperty("msgid") val msgid: Long? = null,
)
