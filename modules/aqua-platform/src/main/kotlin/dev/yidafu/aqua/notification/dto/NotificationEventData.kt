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

package dev.yidafu.aqua.notification.dto

import tools.jackson.module.kotlin.jacksonObjectMapper

/**
 * NOTIFICATION_SEND 领域事件载荷(PRD 13-notification.md)
 *
 * 由各场景 NotificationEventHandler 组装并发布到 notifications-events 队列,
 * NotificationMessageConsumer 消费后经 NotificationGatewayService 落库 + 分发。
 * 幂等键 = relatedNo + ":" + scene + ":" + eventTime(R-NTF-02)。
 */
data class NotificationEventData(
  val receiverType: String, // USER | WORKER
  val userId: Long? = null,
  val workerId: Long? = null,
  val messageType: String, // ORDER | SYSTEM
  val scene: String,
  val title: String,
  val summary: String? = null,
  val relatedNo: String? = null,
  /** 事件时间(epoch millis),幂等键组成 */
  val eventTime: Long,
  /** 微信订阅消息模板场景键(见 NotificationTemplateScenes),非空且接收人为 USER 时尝试发送 */
  val templateScene: String? = null,
  /** 微信订阅消息点击跳转的小程序页面路径 */
  val page: String? = null,
  /** 微信模板字段值(模板参数名 → 值) */
  val templateFields: Map<String, String>? = null,
  /** true → 仅广播推送(不落库,如送水工新订单可抢单,推送给全部在线送水工);false → 定向落库 + 推送 */
  val broadcast: Boolean = false,
) {
  /** 幂等键(与 notifications.idempotency_key 对应) */
  val idempotencyKey: String
    get() = "${relatedNo ?: "-"}:$scene:$eventTime"

  companion object {
    private val objectMapper = jacksonObjectMapper()

    /** 从领域事件 payload 解析 */
    fun fromPayload(payload: String): NotificationEventData = objectMapper.readValue(payload, NotificationEventData::class.java)
  }
}

/**
 * 微信订阅消息模板场景键(F-NTF-001 五节点)
 * 与 SystemSettingKeys.NOTIFICATION_TEMPLATE_*_ID/_ENABLED 键后缀一致。
 */
object NotificationTemplateScenes {
  const val ORDER_CREATED = "order_created" // 下单通知
  const val DELIVERY_ASSIGNED = "delivery_assigned" // 配送员接单
  const val DELIVERY_STARTED = "delivery_started" // 开始配送
  const val DELIVERY_COMPLETED = "delivery_completed" // 配送完成/送达确认
  const val ORDER_URGING = "order_urging" // 催单
  const val PAYMENT_RESULT = "payment_result" // 支付结果/退款结果
}
